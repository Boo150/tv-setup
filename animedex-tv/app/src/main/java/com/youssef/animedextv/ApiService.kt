package com.youssef.animedextv

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiService {

    private val BASE_URL = "https://api100.anime-dex.workers.dev"
    private val PROXY_URL = "https://proxy.techzbots1.workers.dev/?u="

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun get(path: String, useProxy: Boolean = false): String {
        val url = if (useProxy) "$PROXY_URL$BASE_URL$path" else "$BASE_URL$path"
        val request = Request.Builder()
            .url(url)
            .header("referer", "https://animedex.vercel.app")
            .build()
        return client.newCall(request).execute().use { it.body!!.string() }
    }

    private fun safeGet(path: String): String {
        return try { get(path) }
        catch (e: Exception) {
            try { get(path, useProxy = true) }
            catch (e2: Exception) { "{}" }
        }
    }

    fun getHome(): Triple<List<Anime>, List<Anime>, List<Anime>> {
        val raw = safeGet("/home")
        val json = JSONObject(raw)
        val trending = parseTrending(json.optJSONArray("trend") ?: JSONArray())
        val popular = parsePopular(json.optJSONArray("popular") ?: JSONArray())
        val recent = parseRecent(json.optJSONArray("recent") ?: JSONArray())
        return Triple(trending, popular, recent)
    }

    fun getRecent(page: Int = 1): List<Anime> {
        val raw = safeGet("/recent/$page")
        val json = JSONObject(raw)
        return parseRecent(json.optJSONArray("data") ?: JSONArray())
    }

    fun searchAnime(query: String): List<Anime> {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val raw = safeGet("/search/$encoded")
        val json = JSONObject(raw)
        return parsePopular(json.optJSONArray("data") ?: JSONArray())
    }

    fun getAnimeDetails(animeId: String): Pair<Anime?, List<Episode>> {
        val encoded = java.net.URLEncoder.encode(animeId, "UTF-8")
        val raw = safeGet("/anime/$encoded")
        val json = JSONObject(raw)

        val info = json.optJSONObject("info")
        val anime = info?.let {
            val titleObj = it.optJSONObject("title")
            val title = titleObj?.optString("userPreferred") ?: it.optString("title", "Unknown")
            val genres = mutableListOf<String>()
            val genresArr = it.optJSONArray("genres")
            if (genresArr != null) {
                for (i in 0 until genresArr.length()) genres.add(genresArr.getString(i))
            }
            Anime(
                id = animeId,
                title = title,
                image = it.optJSONObject("coverImage")?.optString("extraLarge") ?: "",
                bannerImage = it.optString("bannerImage", ""),
                type = it.optString("format", ""),
                status = it.optString("status", ""),
                genres = genres,
                description = it.optString("description", "").replace(Regex("<[^>]*>"), ""),
                totalEpisodes = it.optInt("episodes", 0)
            )
        }

        val episodes = mutableListOf<Episode>()
        val epsArr = json.optJSONArray("episodes") ?: json.optJSONArray("data")
        if (epsArr != null) {
            for (i in 0 until epsArr.length()) {
                val ep = epsArr.getJSONObject(i)
                episodes.add(Episode(
                    id = ep.optString("id", ""),
                    number = ep.optInt("number", i + 1),
                    title = ep.optString("title", "Episode ${i + 1}")
                ))
            }
        }

        return Pair(anime, episodes)
    }

    fun getStreamSources(episodeId: String): List<StreamSource> {
        val encoded = java.net.URLEncoder.encode(episodeId, "UTF-8")
        val raw = safeGet("/episode/$encoded")
        val json = JSONObject(raw)
        val sources = mutableListOf<StreamSource>()

        // Primary AD-free sources
        val sourcesArr = json.optJSONArray("sources")
        if (sourcesArr != null) {
            for (i in 0 until sourcesArr.length()) {
                val s = sourcesArr.getJSONObject(i)
                val url = s.optString("file", "")
                if (url.isNotEmpty()) {
                    sources.add(StreamSource(
                        url = url,
                        quality = s.optString("label", "HD"),
                        isM3u8 = url.contains(".m3u8")
                    ))
                }
            }
        }

        // Backup sources
        val backupArr = json.optJSONArray("sources_bk")
        if (backupArr != null && sources.isEmpty()) {
            for (i in 0 until backupArr.length()) {
                val s = backupArr.getJSONObject(i)
                val url = s.optString("file", "")
                if (url.isNotEmpty()) {
                    sources.add(StreamSource(
                        url = url,
                        quality = s.optString("label", "HD"),
                        isM3u8 = url.contains(".m3u8")
                    ))
                }
            }
        }

        return sources
    }

    // ---- Parsers ----

    private fun parseTrending(arr: JSONArray): List<Anime> {
        val list = mutableListOf<Anime>()
        for (i in 0 until arr.length()) {
            val a = arr.getJSONObject(i)
            val titleObj = a.optJSONObject("title")
            val title = titleObj?.optString("userPreferred") ?: a.optString("title", "Unknown")
            val genres = mutableListOf<String>()
            val genresArr = a.optJSONArray("genres")
            if (genresArr != null) {
                for (j in 0 until genresArr.length()) genres.add(genresArr.getString(j))
            }
            val poster = a.optString("bannerImage", "").takeIf { it.isNotEmpty() }
                ?: a.optJSONObject("coverImage")?.optString("extraLarge") ?: ""
            list.add(Anime(
                id = title,
                title = title,
                image = a.optJSONObject("coverImage")?.optString("extraLarge") ?: "",
                bannerImage = poster,
                type = a.optString("format", ""),
                status = a.optString("status", ""),
                genres = genres,
                description = a.optString("description", "").replace(Regex("<[^>]*>"), ""),
                totalEpisodes = a.optInt("episodes", 0)
            ))
        }
        return list
    }

    private fun parsePopular(arr: JSONArray): List<Anime> {
        val list = mutableListOf<Anime>()
        for (i in 0 until arr.length()) {
            val a = arr.getJSONObject(i)
            val title = a.optString("title", a.optString("id", "Unknown"))
            list.add(Anime(
                id = a.optString("id", title),
                title = title,
                image = a.optString("image", ""),
                type = if (title.lowercase().contains("dub")) "DUB" else "SUB"
            ))
        }
        return list
    }

    private fun parseRecent(arr: JSONArray): List<Anime> {
        val list = mutableListOf<Anime>()
        for (i in 0 until arr.length()) {
            val a = arr.getJSONObject(i)
            list.add(Anime(
                id = a.optString("id", ""),
                title = a.optString("title", "Unknown"),
                image = a.optString("image", ""),
                type = a.optString("episodeType", "SUB")
            ))
        }
        return list
    }
}

