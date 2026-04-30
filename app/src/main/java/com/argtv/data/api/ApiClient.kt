package com.argtv.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ApiClient(private val baseUrl: String = "https://arg-tv.vercel.app") {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getChannels(): List<ChannelApi> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/channels")
            .build()
        
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: "[]"
            val jsonArray = JSONArray(body)
            val channels = mutableListOf<ChannelApi>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                channels.add(ChannelApi(
                    id = obj.getString("slug"),
                    name = obj.getString("name"),
                    url = "", // se obtiene después via /api/streams/:slug
                    logo = obj.optString("logo", ""),
                    category = obj.optString("category", "general")
                ))
            }
            channels
        }
    }

    suspend fun getStreamUrl(slug: String): StreamResponse = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/streams/$slug")
            .build()
        
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: "{}"
            val obj = JSONObject(body)
            val url = obj.getString("url")
            val headersObj = obj.optJSONObject("headers")
            val headers = mutableMapOf<String, String>()
            if (headersObj != null) {
                headersObj.keys().forEach { key ->
                    headers[key] = headersObj.getString(key)
                }
            }
            StreamResponse(url, headers)
        }
    }

    data class StreamResponse(val url: String, val headers: Map<String, String>)

    suspend fun getMovies(page: Int = 1): List<MediaApi> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/movies?page=$page")
            .build()
        
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: "[]"
            parseMediaList(body)
        }
    }

    suspend fun searchMovies(query: String): List<MediaApi> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/movies/search?q=$query")
            .build()
        
        client.newCall(request).execute().use { response ->
            parseMediaList(response.body?.string() ?: "[]")
        }
    }

    suspend fun getMovieStreamData(tmdbId: String): StreamData? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/movies/$tmdbId/stream")
                .build()
            
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: "{}"
                val obj = JSONObject(body)
                val url = obj.optString("url").takeIf { it.isNotEmpty() && it != "null" }
                val quality = obj.optString("quality")
                val headersObj = obj.optJSONObject("headers")
                val headers = mutableMapOf<String, String>()
                if (headersObj != null) {
                    headersObj.keys().forEach { key ->
                        headers[key] = headersObj.getString(key)
                    }
                }
                if (url != null) StreamData(url, quality, headers) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getSeriesStreamData(tmdbId: String, season: String = "1", episode: String = "1"): StreamData? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/series/$tmdbId/stream?season=$season&episode=$episode")
                .build()
            
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: "{}"
                val obj = JSONObject(body)
                val url = obj.optString("url").takeIf { it.isNotEmpty() && it != "null" }
                val quality = obj.optString("quality")
                val headersObj = obj.optJSONObject("headers")
                val headers = mutableMapOf<String, String>()
                if (headersObj != null) {
                    headersObj.keys().forEach { key ->
                        headers[key] = headersObj.getString(key)
                    }
                }
                if (url != null) StreamData(url, quality, headers) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    data class StreamData(val url: String, val quality: String, val headers: Map<String, String>)

    suspend fun getSeries(page: Int = 1): List<MediaApi> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/series?page=$page")
            .build()
        
        client.newCall(request).execute().use { response ->
            parseMediaList(response.body?.string() ?: "[]")
        }
    }

    suspend fun getMovieStreamUrl(id: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/movies/$id/play")
                .build()
            
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: "{}"
                val obj = JSONObject(body)
                obj.optString("url").takeIf { it.isNotEmpty() && it != "null" }
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getSeriesStreamUrl(id: String, season: String = "1", episode: String = "1"): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/series/$id/$season/$episode/play")
                .build()
            
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: "{}"
                val obj = JSONObject(body)
                obj.optString("url").takeIf { it.isNotEmpty() && it != "null" }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseMediaList(json: String): List<MediaApi> {
        val jsonArray = JSONArray(json)
        val items = mutableListOf<MediaApi>()
        
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            items.add(MediaApi(
                id = obj.getInt("id").toString(),
                title = obj.getString("title"),
                poster = obj.optString("poster", ""),
                year = obj.optString("year", ""),
                overview = obj.optString("overview", ""),
                type = obj.optString("type", "movie")
            ))
        }
        return items
    }

    data class ChannelApi(
        val id: String,
        val name: String,
        val url: String,
        val logo: String,
        val category: String
    )

    data class MediaApi(
        val id: String,
        val title: String,
        val poster: String,
        val year: String,
        val overview: String,
        val type: String
    )
}