package com.youssef.animedextv

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainFragment : BrowseSupportFragment() {

    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        title = "ANIMEDEX"
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true

        brandColor = resources.getColor(R.color.background, null)
        searchAffordanceColor = resources.getColor(R.color.accent_cyan, null)

        adapter = rowsAdapter

        setOnItemViewClickedListener { _, item, _, _ ->
            if (item is Anime) {
                val intent = Intent(requireContext(), AnimeDetailActivity::class.java).apply {
                    putExtra("anime_id", item.id)
                    putExtra("anime_title", item.title)
                    putExtra("anime_image", item.image)
                    putExtra("anime_banner", item.bannerImage)
                    putExtra("anime_type", item.type)
                    putExtra("anime_status", item.status)
                    putExtra("anime_desc", item.description)
                    putStringArrayListExtra("anime_genres", ArrayList(item.genres))
                }
                startActivity(intent)
            }
        }

        setOnSearchClickedListener {
            startActivity(Intent(requireContext(), SearchActivity::class.java))
        }

        loadContent()
    }

    private fun loadContent() {
        lifecycleScope.launch {
            try {
                val (trending, popular, recent) = withContext(Dispatchers.IO) {
                    ApiService.getHome()
                }

                rowsAdapter.clear()

                if (trending.isNotEmpty()) addRow("🔥 Trending", trending)
                if (popular.isNotEmpty()) addRow("⭐ Popular", popular)
                if (recent.isNotEmpty()) addRow("🆕 Recently Updated", recent)

                // Load extra recent pages
                if (recent.isEmpty()) {
                    val recentPage1 = withContext(Dispatchers.IO) { ApiService.getRecent(1) }
                    if (recentPage1.isNotEmpty()) addRow("🆕 Recently Updated", recentPage1)
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun addRow(title: String, animes: List<Anime>) {
        val presenter = AnimeCardPresenter()
        val listRowAdapter = ArrayObjectAdapter(presenter)
        animes.forEach { listRowAdapter.add(it) }
        val header = HeaderItem(title)
        rowsAdapter.add(ListRow(header, listRowAdapter))
    }
}

