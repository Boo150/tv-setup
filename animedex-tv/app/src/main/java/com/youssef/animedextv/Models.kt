package com.youssef.animedextv

data class Anime(
    val id: String,
    val title: String,
    val image: String,
    val type: String = "",
    val status: String = "",
    val genres: List<String> = emptyList(),
    val description: String = "",
    val totalEpisodes: Int = 0,
    val bannerImage: String = ""
)

data class Episode(
    val id: String,
    val number: Int,
    val title: String = "Episode $number"
)

data class StreamSource(
    val url: String,
    val quality: String = "HD",
    val isM3u8: Boolean = true
)

