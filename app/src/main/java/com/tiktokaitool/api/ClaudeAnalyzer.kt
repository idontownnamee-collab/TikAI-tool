package com.tiktokaitool.api

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class ClaudeAnalyzer(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val API_URL = "https://api.anthropic.com/v1/messages"
        private const val MODEL = "claude-opus-4-20250514"

        val DEFAULT_PROMPT = """
You are an expert AI identifier. The user has cropped a section from a TikTok video they are watching.
Analyze the image carefully and identify EVERYTHING visible.

Respond in this exact format:

## 🔍 What I Found
[Clear, direct identification of the main subject]

## 📛 Name / Title
[Exact name, character name, item name, game element name, etc.]

## 🎮 Category
[Game / Anime / Movie / Product / Person / Animal / Food / etc.]

## ℹ️ Details
[Detailed information: what it is, where it's from, its significance, rarity, how to get it, etc.]

## 🔗 Where to Find / Get It
[If applicable: how to obtain, website, game mode, season, etc.]

## 💡 Extra Info
[Any other useful facts, tips, or related items]

Be as specific and helpful as possible. If you see a game avatar, skin, or item — give the exact name and how to get it.
If you see an anime character — give their full name, series, and key facts.
If you see a product — give the name, brand, and where to buy.
        """.trimIndent()
    }

    /**
     * Send cropped bitmap to Claude API for analysis
     */
    suspend fun analyzeImage(
        bitmap: Bitmap,
        customPrompt: String = DEFAULT_PROMPT
    ): AnalysisResult = withContext(Dispatchers.IO) {

        // Compress bitmap to base64
        val base64Image = bitmapToBase64(bitmap)

        // Build request JSON
        val content = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "image")
                put("source", JSONObject().apply {
                    put("type", "base64")
                    put("media_type", "image/jpeg")
                    put("data", base64Image)
                })
            })
            put(JSONObject().apply {
                put("type", "text")
                put("text", customPrompt)
            })
        }

        val body = JSONObject().apply {
            put("model", MODEL)
            put("max_tokens", 1500)
            put("messages", JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", content)
                }
            ))
        }

        val request = Request.Builder()
            .url(API_URL)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")

        if (!response.isSuccessful) {
            val err = try { JSONObject(responseBody).optString("error", responseBody) }
            catch (e: Exception) { responseBody }
            throw Exception("API Error ${response.code}: $err")
        }

        val json = JSONObject(responseBody)
        val text = json.getJSONArray("content")
            .getJSONObject(0)
            .getString("text")

        AnalysisResult(
            rawText = text,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        // Resize if too large
        val resized = if (bitmap.width > 1024 || bitmap.height > 1024) {
            val ratio = minOf(1024f / bitmap.width, 1024f / bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt(),
                (bitmap.height * ratio).toInt(),
                true
            )
        } else bitmap

        resized.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    data class AnalysisResult(
        val rawText: String,
        val timestamp: Long
    )
}
