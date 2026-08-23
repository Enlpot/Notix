package com.enlpot.notix

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class UnmonitoredAppsStorage(context: Context) {

    companion object {
        private val lock = Any()
        @Volatile
        private var cachedApps: Set<String>? = null
    }

    private val prefs: SharedPreferences = context.getSharedPreferences("unmonitored_apps_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "unmonitored_apps"

    fun getUnmonitoredApps(): Set<String> {
        cachedApps?.let { return it }
        synchronized(lock) {
            cachedApps?.let { return it }
            val json = prefs.getString(key, null) ?: return emptySet<String>().also { cachedApps = it }
            val type = object : TypeToken<Set<String>>() {}.type
            val apps: Set<String> = gson.fromJson(json, type)
            cachedApps = apps
            return apps
        }
    }

    fun addApp(packageName: String) {
        // v8.0：读-改-写整体加锁，避免并发 add/remove 时基于同一缓存副本修改后互相覆盖导致丢 app
        synchronized(lock) {
            val currentApps = (cachedApps ?: getUnmonitoredApps()).toMutableSet()
            currentApps.add(packageName)
            saveApps(currentApps)
        }
    }

    fun removeApp(packageName: String) {
        synchronized(lock) {
            val currentApps = (cachedApps ?: getUnmonitoredApps()).toMutableSet()
            currentApps.remove(packageName)
            saveApps(currentApps)
        }
    }

    fun isAppUnmonitored(packageName: String): Boolean {
        return getUnmonitoredApps().contains(packageName)
    }

    private fun saveApps(apps: Set<String>) {
        val json = gson.toJson(apps)
        prefs.edit().putString(key, json).apply()
        cachedApps = apps
    }
}
