package com.tiktokaitool.utils

import okhttp3.OkHttpClient
import okhttp3.Request

object TikTokUtils {

    private val TIKTOK_URL_REGEX = Regex(
        "(https?://)?(www\\.)?(vm\\.tiktok\\.com|tiktok\\.com|m\\.tiktok\\.com)/.+"
    )

    fun isValidTikTokUrl(url: String): Boolean {
        return TIKTOK_URL_REGEX.matches(url.trim())
    }

    /**
     * Resolves a TikTok share URL to the direct video URL.
     * TikTok uses aweme API internally. This follows redirects and
     * extracts the video source from the page/API response.
     */
    suspend fun resolveVideoUrl(shareUrl: String, client: OkHttpClient): String? {
        return try {
            // Step 1: Follow redirect to get canonical URL
            val expandedUrl = expandUrl(shareUrl, client) ?: shareUrl

            // Step 2: Extract video ID from URL
            val videoId = extractVideoId(expandedUrl) ?: return null

            // Step 3: Call TikTok's API endpoint
            val apiUrl = "https://api2.musical.ly/aweme/v1/feed/?aweme_id=$videoId"
            val req = Request.Builder()
                .url(apiUrl)
                .addHeader("User-Agent", "TikTok 26.2.0 rv:262018 (iPhone; iOS 14.4.2; en_US) Cronet")
                .build()

            val resp = client.newCall(req).execute()
            val body = resp.body?.string() ?: return null

            // Parse the direct video URL from JSON response
            val json = org.json.JSONObject(body)
            val awemeList = json.optJSONArray("aweme_list") ?: return null
            if (awemeList.length() == 0) return null

            val video = awemeList.getJSONObject(0).getJSONObject("video")
            val downloadAddr = video.getJSONObject("download_addr")
            downloadAddr.getJSONArray("url_list").getString(0)

        } catch (e: Exception) {
            null
        }
    }

    private fun expandUrl(url: String, client: OkHttpClient): String? {
        return try {
            val req = Request.Builder()
                .url(url)
                .head()
                .build()
            val resp = client.newCall(req).execute()
            resp.request.url.toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun extractVideoId(url: String): String? {
        // Matches /video/1234567890123456789
        val match = Regex("/video/(\\d+)").find(url)
        return match?.groupValues?.get(1)
    }
}
