package com.android.kiyori.browser.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class BrowserPluginEntry(
    val id: String,
    val name: String,
    val matchRule: String,
    val sourceUrl: String,
    val createdAt: Long
)

data class BrowserPluginStore(
    val title: String,
    val url: String
)

class BrowserPluginRepository(context: Context) {
    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun getPlugins(): List<BrowserPluginEntry> {
        return readPlugins().sortedByDescending { it.createdAt }
    }

    fun addPlugin(
        name: String,
        matchRule: String,
        sourceUrl: String = ""
    ): BrowserPluginEntry {
        val entry = BrowserPluginEntry(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            matchRule = matchRule.trim().ifBlank { "global" },
            sourceUrl = sourceUrl.trim(),
            createdAt = System.currentTimeMillis()
        )
        val updatedEntries = readPlugins().apply {
            add(entry)
        }
        writePlugins(updatedEntries)
        return entry
    }

    fun deletePlugin(id: String) {
        val updatedEntries = readPlugins().filterNot { it.id == id }
        writePlugins(updatedEntries)
    }

    private fun readPlugins(): MutableList<BrowserPluginEntry> {
        val raw = sharedPreferences.getString(KEY_PLUGIN_ENTRIES, "").orEmpty().trim()
        if (raw.isBlank()) {
            return mutableListOf()
        }
        return runCatching {
            val jsonArray = JSONArray(raw)
            MutableList(jsonArray.length()) { index ->
                val item = jsonArray.getJSONObject(index)
                BrowserPluginEntry(
                    id = item.optString("id"),
                    name = item.optString("name"),
                    matchRule = item.optString("matchRule"),
                    sourceUrl = item.optString("sourceUrl"),
                    createdAt = item.optLong("createdAt")
                )
            }.filter { it.id.isNotBlank() && it.name.isNotBlank() }.toMutableList()
        }.getOrDefault(mutableListOf())
    }

    private fun writePlugins(entries: List<BrowserPluginEntry>) {
        val jsonArray = JSONArray()
        entries.forEach { entry ->
            jsonArray.put(
                JSONObject().apply {
                    put("id", entry.id)
                    put("name", entry.name)
                    put("matchRule", entry.matchRule)
                    put("sourceUrl", entry.sourceUrl)
                    put("createdAt", entry.createdAt)
                }
            )
        }
        sharedPreferences.edit().putString(KEY_PLUGIN_ENTRIES, jsonArray.toString()).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "browser_plugin_repository"
        private const val KEY_PLUGIN_ENTRIES = "plugin_entries"

        val storeEntries: List<BrowserPluginStore> = listOf(
            BrowserPluginStore("Greasy Fork", "https://greasyfork.org/"),
            BrowserPluginStore("Script Cat", "https://scriptcat.org/en/"),
            BrowserPluginStore("GFMirror", "https://greasyfork.org/"),
            BrowserPluginStore("GFork", "https://home.greasyfork.org.cn/"),
            BrowserPluginStore("OpenUserJS", "https://openuserjs.org/"),
            BrowserPluginStore("Userscript Zone", "https://www.userscript.zone/"),
            BrowserPluginStore("Github", "https://github.com/topics/userscript")
        )
    }
}
