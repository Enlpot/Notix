package com.enlpot.notix

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 常驻通知合并设置存储（v8.48.3）。
 *
 * 用于控制"常驻通知（ongoing）生命周期合并"行为：
 * - 全局开关（默认开）：所有常驻通知按生命周期合并（同一 sbnKey 生命周期内高频刷新合并为一条，
 *   只有移除后重新出现（新生命周期）才记录一条，保留连接状态变化痕迹）。
 * - 按包名例外：单 App 可独立设置"合并/不合并"，覆盖全局开关。
 *   判定优先级：按包名明确设置 > 全局开关。
 */
class OngoingMergeStorage(context: Context) {

    companion object {
        private val lock = Any()
        @Volatile
        private var cachedGlobal: Boolean? = null
        @Volatile
        private var cachedMerge: Set<String>? = null
        @Volatile
        private var cachedNoMerge: Set<String>? = null
    }

    private val prefs: SharedPreferences = context.getSharedPreferences("ongoing_merge_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val keyGlobal = "merge_global_enabled"
    private val keyMerge = "merge_by_app"
    private val keyNoMerge = "no_merge_by_app"

    /** 全局开关（默认开）。 */
    fun isGlobalEnabled(): Boolean {
        cachedGlobal?.let { return it }
        synchronized(lock) {
            cachedGlobal?.let { return it }
            val v = prefs.getBoolean(keyGlobal, true)
            cachedGlobal = v
            return v
        }
    }

    fun setGlobalEnabled(enabled: Boolean) {
        synchronized(lock) {
            prefs.edit().putBoolean(keyGlobal, enabled).apply()
            cachedGlobal = enabled
        }
    }

    /** 明确"合并"的包名集合。 */
    fun getMergeApps(): Set<String> {
        cachedMerge?.let { return it }
        synchronized(lock) {
            cachedMerge?.let { return it }
            val json = prefs.getString(keyMerge, null) ?: return emptySet<String>().also { cachedMerge = it }
            val type = object : TypeToken<Set<String>>() {}.type
            val apps: Set<String> = gson.fromJson(json, type)
            cachedMerge = apps
            return apps
        }
    }

    /** 明确"不合并"的包名集合。 */
    fun getNoMergeApps(): Set<String> {
        cachedNoMerge?.let { return it }
        synchronized(lock) {
            cachedNoMerge?.let { return it }
            val json = prefs.getString(keyNoMerge, null) ?: return emptySet<String>().also { cachedNoMerge = it }
            val type = object : TypeToken<Set<String>>() {}.type
            val apps: Set<String> = gson.fromJson(json, type)
            cachedNoMerge = apps
            return apps
        }
    }

    /**
     * 设置某包名的合并行为。
     * @param merge true=合并（移除 noMerge 记录），false=不合并（移除 merge 记录）
     */
    fun setMergeApp(packageName: String, merge: Boolean) {
        synchronized(lock) {
            val mergeApps = (cachedMerge ?: getMergeApps()).toMutableSet()
            val noMergeApps = (cachedNoMerge ?: getNoMergeApps()).toMutableSet()
            if (merge) {
                mergeApps.add(packageName)
                noMergeApps.remove(packageName)
            } else {
                noMergeApps.add(packageName)
                mergeApps.remove(packageName)
            }
            saveSets(mergeApps, noMergeApps)
        }
    }

    /** 移除某包名的例外，恢复跟随全局开关。 */
    fun resetAppOverride(packageName: String) {
        synchronized(lock) {
            val mergeApps = (cachedMerge ?: getMergeApps()).toMutableSet()
            val noMergeApps = (cachedNoMerge ?: getNoMergeApps()).toMutableSet()
            mergeApps.remove(packageName)
            noMergeApps.remove(packageName)
            saveSets(mergeApps, noMergeApps)
        }
    }

    /** 某包名是否处于"明确例外"状态。 */
    fun isOverridden(packageName: String): Boolean {
        return getMergeApps().contains(packageName) || getNoMergeApps().contains(packageName)
    }

    /** 某包名是否合并（判定优先级：包名例外 > 全局开关）。 */
    fun shouldMerge(packageName: String): Boolean {
        if (getMergeApps().contains(packageName)) return true
        if (getNoMergeApps().contains(packageName)) return false
        return isGlobalEnabled()
    }

    private fun saveSets(merge: Set<String>, noMerge: Set<String>) {
        prefs.edit()
            .putString(keyMerge, gson.toJson(merge))
            .putString(keyNoMerge, gson.toJson(noMerge))
            .apply()
        cachedMerge = merge
        cachedNoMerge = noMerge
    }
}
