package com.youssef.animedextv

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.fragment.app.FragmentActivity

class SearchActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.search_fragment, AnimeSearchFragment())
                .commitNow()
        }
    }
}

class AnimeSearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider {

    private val resultsAdapter = ArrayObjectAdapter(ListRowPresenter())
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSearchResultProvider(this)
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
    }

    override fun getResultsAdapter(): ObjectAdapter = resultsAdapter

    override fun onQueryTextChange(newQuery: String): Boolean {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            delay(500) // debounce
            search(newQuery)
        }
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch { search(query) }
        return true
    }

    private suspend fun search(query: String) {
        if (query.length < 2) return
        try {
            val results = withContext(Dispatchers.IO) { ApiService.searchAnime(query) }
            resultsAdapter.clear()
            if (results.isNotEmpty()) {
                val cardPresenter = AnimeCardPresenter()
                val listAdapter = ArrayObjectAdapter(cardPresenter)
                results.forEach { listAdapter.add(it) }
                resultsAdapter.add(ListRow(HeaderItem("Results for \"$query\""), listAdapter))
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Search error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

