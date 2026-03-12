package com.tiktokaitool.utils

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tikai_prefs", Context.MODE_PRIVATE)

    fun getApiKey(): String = prefs.getString("api_key", "") ?: ""
    fun saveApiKey(key: String) = prefs.edit().putString("api_key", key).apply()

    fun getAnalysisPrompt(): String = prefs.getString("analysis_prompt", "") ?: ""
    fun saveAnalysisPrompt(prompt: String) = prefs.edit().putString("analysis_prompt", prompt).apply()

    fun isOverlayEnabled(): Boolean = prefs.getBoolean("overlay_enabled", true)
    fun setOverlayEnabled(v: Boolean) = prefs.edit().putBoolean("overlay_enabled", v).apply()

    fun isAutoOverlayOnTikTok(): Boolean = prefs.getBoolean("auto_overlay", false)
    fun setAutoOverlayOnTikTok(v: Boolean) = prefs.edit().putBoolean("auto_overlay", v).apply()
}

data class DownloadRecord(
    val url: String,
    val filePath: String,
    val timestamp: Long
)

class HistoryManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("tikai_history", Context.MODE_PRIVATE)

    fun saveEntry(result: String, imagePath: String?) {
        val entries = getEntries().toMutableList()
        val entry = mapOf(
            "result" to result,
            "image" to (imagePath ?: ""),
            "time" to System.currentTimeMillis().toString()
        )
        entries.add(0, org.json.JSONObject(entry).toString())
        // Keep last 50 entries
        val trimmed = entries.take(50)
        prefs.edit().putString("entries", org.json.JSONArray(trimmed).toString()).apply()
    }

    fun getEntries(): List<String> {
        val raw = prefs.getString("entries", "[]") ?: "[]"
        val arr = org.json.JSONArray(raw)
        return (0 until arr.length()).map { arr.getString(it) }
    }

    fun saveDownload(url: String, filePath: String) {
        val records = getDownloads().toMutableList()
        records.add(0, DownloadRecord(url, filePath, System.currentTimeMillis()))
        val jsonArr = org.json.JSONArray(records.map { r ->
            org.json.JSONObject().apply {
                put("url", r.url)
                put("path", r.filePath)
                put("time", r.timestamp)
            }
        })
        prefs.edit().putString("downloads", jsonArr.toString()).apply()
    }

    fun getDownloads(): List<DownloadRecord> {
        val raw = prefs.getString("downloads", "[]") ?: "[]"
        val arr = org.json.JSONArray(raw)
        return (0 until arr.length()).map {
            val obj = arr.getJSONObject(it)
            DownloadRecord(
                url = obj.getString("url"),
                filePath = obj.getString("path"),
                timestamp = obj.getLong("time")
            )
        }
    }
}
