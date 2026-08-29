package com.enlpot.notix

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream

/**
 * Rule persistence.
 *
 * Every mutation is **id-keyed** and performed under [lock] against freshly-read state.
 * That matters because two writers race: the listener bumps hit counts from a background
 * executor while the UI edits/deletes/imports from Compose state. A whole-list
 * read-modify-write loses writes in both directions — a hit-count bump can resurrect a rule
 * the UI just deleted, and a UI save can erase an import. The listener and the UI share one
 * process (no `android:process` in the manifest), so this lock is a genuine mutual-exclusion
 * point.
 */
class RuleStorage(private val context: Context) {

    companion object {
        private const val TAG = "RuleStorage"
        @Volatile
        private var cachedRules: List<BlockerRule>? = null
        private val lock = Any()
    }

    private val gson = Gson()
    private val rulesFile = File(context.filesDir, "rules.json")
    private val atomicFile = AtomicFile(rulesFile)

    fun getRules(): List<BlockerRule> {
        cachedRules?.let { return it }
        synchronized(lock) {
            return loadLocked()
        }
    }

    /** Caller must hold [lock]. */
    private fun loadLocked(): List<BlockerRule> {
        cachedRules?.let { return it }
        if (!rulesFile.exists()) {
            return emptyList<BlockerRule>().also { cachedRules = it }
        }
        val json: String
        val parsed = try {
            json = atomicFile.readFully().toString(Charsets.UTF_8)
            val type = object : TypeToken<List<BlockerRule>>() {}.type
            gson.fromJson<List<BlockerRule>>(json, type) ?: emptyList()
        } catch (e: JsonSyntaxException) {
            preserveCorruptFile(e)
            // v7.24：解析异常同样不缓存空列表——避免缓存污染导致后续即使文件可恢复也一直返回空
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading rules", e)
            // v7.24：异常时不缓存空列表——避免缓存污染导致后续即使文件可恢复也一直返回空
            return emptyList()
        }

        // Ids missing *from the JSON* must be persisted immediately. Gson deserializes such a rule
        // through Kotlin's synthesized no-arg constructor, which mints a fresh random UUID — so the
        // id looks valid but would differ on every load, silently re-keying the rule's notification
        // channel each time. Checking the raw JSON is the only way to see this.
        val missingFromDisk = !RuleIds.rulesJsonHasAllIds(json)
        val normalized = if (missingFromDisk || RuleIds.needsNormalizing(parsed)) {
            RuleIds.normalizeIds(parsed)
        } else {
            parsed
        }
        // v7.21：旧数据字段归一化——Gson 反序列化旧规则 JSON 时（Unsafe 分配、不走 Kotlin
        // 构造器默认值），v7.20 新增的 ExtraCondition.bluetoothDeviceNames 字段缺失会得到 null，
        // 匹配引擎 .isNotEmpty() 即 NPE（v7.20 打开闪退根因）。加载时统一兜底为 emptyList()，
        // 与下方无效规则过滤同层处理，一次性消除旧数据风险。
        var sanitizedChanged = false
        val sanitized = normalized.map { rule ->
            val extra = rule.extraCondition
            @Suppress("SENSELESS_COMPARISON")
            if (extra != null && extra.bluetoothDeviceNames == null) {
                sanitizedChanged = true
                rule.copy(extraCondition = extra.copy(bluetoothDeviceNames = emptyList()))
            } else {
                rule
            }
        }
        // v7.24：过滤无效规则前先备份 rules.json 为 rules.json.bak，防止过滤/回写异常导致规则丢失无法恢复
        try {
            if (rulesFile.exists()) {
                rulesFile.copyTo(File(context.filesDir, "rules.json.bak"), overwrite = true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to backup rules.json before filtering", e)
        }
        // v7.13：剔除无效（僵尸）规则——旧版/字段缺失规则解析后存在 null 字段，UI 解引用即崩溃。
        // 加载即过滤并回写清理，保证进入 UI 的规则全部 isValid。
        val valid = sanitized.filter { it.isValid }
        if (valid.size != sanitized.size) {
            Log.w(TAG, "Dropped ${sanitized.size - valid.size} invalid rule(s) on load")
            writeLocked(valid)
            return valid
        }
        return if (missingFromDisk || RuleIds.needsNormalizing(parsed) || sanitizedChanged) {
            writeLocked(sanitized)
            sanitized
        } else {
            sanitized.also { cachedRules = it }
        }
    }

    /**
     * A corrupt file is *preserved*, not deleted. The previous behaviour discarded every rule
     * the user had, unrecoverably, on a single bad parse. The timestamp stops a later failure
     * from overwriting the first (most likely still-recoverable) copy.
     */
    private fun preserveCorruptFile(e: Exception) {
        val backup = File(context.filesDir, "rules.json.corrupt.${System.currentTimeMillis()}")
        val moved = try {
            rulesFile.renameTo(backup)
        } catch (io: Exception) {
            false
        }
        Log.e(
            TAG,
            "Corrupted rules file; ${if (moved) "preserved as ${backup.name}" else "could not preserve"}",
            e
        )
    }

    /**
     * Atomically replace the rules file, then publish to the cache. The cache is updated only
     * after a durable write, so it can never run ahead of disk.
     * Caller must hold [lock].
     */
    private fun writeLocked(rules: List<BlockerRule>) {
        var stream: FileOutputStream? = null
        try {
            stream = atomicFile.startWrite()
            stream.write(gson.toJson(rules).toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(stream)
        } catch (e: Exception) {
            stream?.let { atomicFile.failWrite(it) }
            Log.e(TAG, "Failed to write rules; cache left untouched", e)
            throw e
        }
        cachedRules = rules
    }

    /**
     * Whole-list overwrite. Prefer the id-keyed mutations below — this clobbers whatever a
     * concurrent writer committed, and exists only for callers that genuinely own the list.
     */
    fun saveRules(rules: List<BlockerRule>) {
        synchronized(lock) { writeLocked(RuleIds.normalizeIds(rules)) }
        // v8.26：规则保存后，将规则应用到当前通知栏中已有的通知
        NotificationBlockerService.instance?.applyRulesToActiveNotifications()
    }

    /**
     * Apply a pure mutation from [RuleMutations] against freshly-read state and commit it.
     * Nothing outside this class may write, so every mutation is serialized against every
     * other and against the listener's hit-count path.
     *
     * @return the committed list, or null if [mutate] declined (e.g. the rule no longer exists).
     */
    private fun mutate(mutate: (List<BlockerRule>) -> List<BlockerRule>?): List<BlockerRule>? {
        synchronized(lock) {
            val current = loadLocked()
            val updated = mutate(current) ?: return null
            if (updated != current) writeLocked(updated)
            return updated
        }
    }

    /**
     * Hot path: called by the listener for every matched notification. Bumps only the named
     * rules, against current state, so it cannot resurrect a rule deleted in the meantime.
     * Deliberately does **not** trigger channel sync — nothing structural changes here.
     */
    fun incrementHitCounts(ruleIds: List<String>) {
        if (ruleIds.isEmpty()) return
        mutate { RuleMutations.applyHitCounts(it, ruleIds) }
    }

    /**
     * Replace the rule identified by [id], forcing the committed rule back onto [id] — see
     * [RuleMutations.applyUpdate].
     * @return the committed list, or null if no rule with [id] exists.
     */
    fun updateRuleById(id: String, newRule: BlockerRule): List<BlockerRule>? {
        val result = mutate { RuleMutations.applyUpdate(it, id, newRule) }
        // v8.26：规则更新后，将规则应用到当前通知栏中已有的通知
        if (result != null) NotificationBlockerService.instance?.applyRulesToActiveNotifications()
        return result
    }

    fun deleteRuleById(id: String): List<BlockerRule> {
        val result = mutate { RuleMutations.applyDelete(it, id) }!!
        // v8.14：删除规则时恢复其冻结的常驻通知——对每个 key 用短时长 re-snooze（100ms）
        // 覆盖原到期时间，短值到期后通知自动回栏（Android 公开 API 无 unSnooze，此法为实测有效的恢复手段）。
        // 服务未运行时 instance==null，安全跳过（此时系统里该规则的 snooze 仍挂到原到期时间，属边界情况）。
        NotificationBlockerService.instance?.restoreSnoozedByRule(id)
        return result
    }

    fun addRules(rules: List<BlockerRule>): List<BlockerRule> {
        val result = mutate { RuleMutations.applyAdd(it, rules) }!!
        // v8.26：规则添加后，将规则应用到当前通知栏中已有的通知
        NotificationBlockerService.instance?.applyRulesToActiveNotifications()
        return result
    }

    fun setEnabledByIds(ruleIds: Set<String>, enabled: Boolean): List<BlockerRule> {
        val result = mutate { RuleMutations.applySetEnabled(it, ruleIds, enabled) }!!
        // v8.26：规则启用/禁用后，将规则应用到当前通知栏中已有的通知
        if (enabled) NotificationBlockerService.instance?.applyRulesToActiveNotifications()
        return result
    }

    fun setAllEnabled(enabled: Boolean): List<BlockerRule> {
        val result = mutate { RuleMutations.applySetAllEnabled(it, enabled) }!!
        // v8.26：规则全部启用/禁用后，将规则应用到当前通知栏中已有的通知
        if (enabled) NotificationBlockerService.instance?.applyRulesToActiveNotifications()
        return result
    }

    fun resetHitCounts() {
        mutate { RuleMutations.applyResetHitCounts(it) }
    }

    fun resetHitCounts(ruleIds: List<String>) {
        mutate { RuleMutations.applyResetHitCounts(it, ruleIds) }
    }

    fun invalidateCache() {
        synchronized(lock) {
            cachedRules = null
        }
    }
}
