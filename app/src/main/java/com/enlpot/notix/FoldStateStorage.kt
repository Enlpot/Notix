package com.enlpot.notix

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 折叠段展开状态持久化存储。
 * 用有序 List 保持展开顺序，上限 MAX_EXPANDED 个，超出时移除最旧的。
 */
class FoldStateStorage(context: Context) {

    companion object {
        private const val MAX_EXPANDED = 20
        private val lock = Any()
        @Volatile
        private var cachedKeys: List<String>? = null
    }

    private val prefs: SharedPreferences = context.getSharedPreferences("fold_state_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "expanded_fold_keys"

    fun getExpandedKeys(): List<String> {
        cachedKeys?.let { return it }
        synchronized(lock) {
            cachedKeys?.let { return it }
            val json = prefs.getString(key, null) ?: return emptyList<String>().also { cachedKeys = it }
            val type = object : TypeToken<List<String>>() {}.type
            val keys: List<String> = gson.fromJson(json, type)
            cachedKeys = keys
            return keys
        }
    }

    /** 切换某个 foldKey 的展开状态，返回切换后的展开状态 */
    fun toggle(key: String): Boolean {
        synchronized(lock) {
            val current = (cachedKeys ?: getExpandedKeys()).toMutableList()
            return if (current.contains(key)) {
                current.remove(key)
                saveKeys(current)
                false
            } else {
                current.add(key)
                // 超出上限时移除最旧的
                while (current.size > MAX_EXPANDED) {
                    current.removeAt(0)
                }
                saveKeys(current)
                true
            }
        }
    }

    /**
     * 删除通知时迁移展开状态：如果被删 key 是某个展开段的旧 key，
     * 且该段有新 key，则把展开状态从旧 key 迁移到新 key。
     */
    fun migrateKey(oldKey: String, newKey: String) {
        synchronized(lock) {
            val current = (cachedKeys ?: getExpandedKeys()).toMutableList()
            val idx = current.indexOf(oldKey)
            if (idx >= 0 && !current.contains(newKey)) {
                current[idx] = newKey
                saveKeys(current)
            }
        }
    }

    fun clear() {
        synchronized(lock) {
            saveKeys(emptyList())
        }
    }

    private fun saveKeys(keys: List<String>) {
        val json = gson.toJson(keys)
        prefs.edit().putString(key, json).apply()
        cachedKeys = keys
    }
}
