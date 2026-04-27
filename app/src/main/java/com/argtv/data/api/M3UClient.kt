package com.argtv.data.api

import com.argtv.data.model.Channel
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern

class M3UClient {

    private val client = OkHttpClient()

    fun fetchChannels(): Result<List<Channel>> {
        return try {
            val request = Request.Builder()
                .url("https://iptv-org.github.io/iptv/countries/ar.m3u")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return Result.failure(Exception("Empty response"))

            val channels = parseM3U(body)
            Result.success(channels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseM3U(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.split("\n")
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("#EXTINF:")) {
                var name = ""
                var logo = ""
                var category = "general"
                
                // Parse attributes from EXTINF line
                val attrs = line.removePrefix("#EXTINF:").split(",").first()
                attrs.split(" ").forEach { attr ->
                    if (attr.startsWith("tvg-name=")) {
                        name = attr.substringAfter("=\"").substringBefore("\"")
                    } else if (attr.startsWith("tvg-logo=")) {
                        logo = attr.substringAfter("=\"").substringBefore("\"")
                    } else if (attr.startsWith("group-title=")) {
                        category = attr.substringAfter("=\"").substringBefore("\"")
                    }
                }
                
                // Fallback name
                if (name.isEmpty()) {
                    val parts = line.removePrefix("#EXTINF:").split(",")
                    if (parts.size > 1) name = parts.last().trim()
                }
                
                // Next line is URL
                if (i + 1 < lines.size) {
                    val url = lines[i + 1].trim()
                    if (url.startsWith("http")) {
                        channels.add(Channel(
                            id = name.lowercase().replace(" ", "_"),
                            name = name,
                            url = url,
                            logo = logo,
                            category = category.ifEmpty { "general" }
                        ))
                    }
                }
            }
            i++
        }
        return channels
    }
}