package com.youssef.animedextv

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import androidx.leanback.widget.*
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnimeDetailActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        if (savedInstanceState == null) {
            val frag = AnimeDetailFragment().apply {
                arguments = intent.extras
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.detail_fragment, frag)
                .commitNow()
        }
    }
}

class AnimeDetailFragment : DetailsSupportFragment() {

    private lateinit var bgController: DetailsSupportFragmentBackgroundController
    private val rowsAdapter = ArrayObjectAdapter()
    private lateinit var detailsPresenter: FullWidthDetailsOverviewRowPresenter
    private var episodes: List<Episode> = emptyList()
    private var animeId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bgController = DetailsSupportFragmentBackgroundController(this)

        animeId = arguments?.getString("anime_id") ?: ""
        val title = arguments?.getString("anime_title") ?: ""
        val image = arguments?.getString("anime_image") ?: ""
        val banner = arguments?.getString("anime_banner") ?: image
        val type = arguments?.getString("anime_type") ?: ""
        val status = arguments?.getString("anime_status") ?: ""
        val desc = arguments?.getString("anime_desc") ?: ""
        val genres = arguments?.getStringArrayList("anime_genres") ?: arrayListOf()

        // Placeholder anime while loading
        val anime = Anime(animeId, title, image, type, status, genres, desc, 0, banner)

        setupUI(anime)
        loadEpisodesAndDetails(animeId, anime)
    }

    private fun setupUI(anime: Anime) {
        detailsPresenter = FullWidthDetailsOverviewRowPresenter(DetailsDescriptionPresenter())
        detailsPresenter.backgroundColor = resources.getColor(R.color.card_bg, null)

        val detailsRow = DetailsOverviewRow(anime)

        // Load poster into details row
        if (anime.image.isNotEmpty()) {
            Glide.with(this)
                .asBitmap()
                .load(anime.image)
                .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                    override fun onResourceReady(r: android.graphics.Bitmap, t: com.bumptech.glide.request.transition.Transition<in android.graphics.Bitmap>?) {
                        detailsRow.imageDrawable = android.graphics.drawable.BitmapDrawable(resources, r)
                    }
                    override fun onLoadCleared(p: android.graphics.drawable.Drawable?) {}
                })
        }

        // Load banner background
        val bannerUrl = anime.bannerImage.takeIf { it.isNotEmpty() } ?: anime.image
        if (bannerUrl.isNotEmpty()) {
            bgController.enableParallax()
            Glide.with(this)
                .asBitmap()
                .load(bannerUrl)
                .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                    override fun onResourceReady(r: android.graphics.Bitmap, t: com.bumptech.glide.request.transition.Transition<in android.graphics.Bitmap>?) {
                        bgController.coverBitmap = r
                    }
                    override fun onLoadCleared(p: android.graphics.drawable.Drawable?) {}
                })
        }

        val actionAdapter = ArrayObjectAdapter()
        actionAdapter.add(Action(1, "▶  Watch Now"))
        actionAdapter.add(Action(2, "📋  Episodes"))
        detailsRow.actionsAdapter = actionAdapter

        val classPresenterSelector = ClassPresenterSelector().apply {
            addClassPresenter(DetailsOverviewRow::class.java, detailsPresenter)
            addClassPresenter(ListRow::class.java, ListRowPresenter())
        }
        rowsAdapter.setPresenterSelector(classPresenterSelector)
        rowsAdapter.add(detailsRow)
        adapter = rowsAdapter

        detailsPresenter.setOnActionClickedListener { action ->
            when (action.id) {
                1L -> playFirstEpisode()
                2L -> scrollToEpisodes()
            }
        }

        setOnItemViewClickedListener { _, item, _, _ ->
            if (item is Episode) {
                startPlayer(item)
            }
        }
    }

    private fun loadEpisodesAndDetails(animeId: String, placeholder: Anime) {
        lifecycleScope.launch {
            try {
                val (anime, eps) = withContext(Dispatchers.IO) {
                    ApiService.getAnimeDetails(animeId)
                }
                episodes = eps

                if (eps.isNotEmpty()) {
                    val epPresenter = EpisodePresenter()
                    val epAdapter = ArrayObjectAdapter(epPresenter)
                    eps.take(100).forEach { epAdapter.add(it) }
                    val header = HeaderItem("Episodes (${eps.size})")
                    rowsAdapter.add(ListRow(header, epAdapter))
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load episodes", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun playFirstEpisode() {
        if (episodes.isNotEmpty()) startPlayer(episodes[0])
        else Toast.makeText(requireContext(), "No episodes found", Toast.LENGTH_SHORT).show()
    }

    private fun scrollToEpisodes() { /* episodes are already visible in row */ }

    private fun startPlayer(ep: Episode) {
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra("episode_id", ep.id)
            putExtra("episode_title", ep.title)
            putExtra("anime_title", (rowsAdapter.get(0) as? DetailsOverviewRow)?.let { (it.item as? Anime)?.title } ?: "")
        }
        startActivity(intent)
    }
}

class DetailsDescriptionPresenter : AbstractDetailsDescriptionPresenter() {
    override fun onBindDescription(viewHolder: ViewHolder, item: Any) {
        val anime = item as Anime
        viewHolder.title.text = anime.title
        viewHolder.subtitle.text = listOfNotNull(
            anime.type.takeIf { it.isNotEmpty() },
            anime.status.takeIf { it.isNotEmpty() },
            anime.genres.take(3).joinToString(", ").takeIf { it.isNotEmpty() }
        ).joinToString(" · ")
        viewHolder.body.text = anime.description.ifEmpty { "No description available." }
    }
}

class EpisodePresenter : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_episode, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val ep = item as Episode
        viewHolder.view.findViewById<android.widget.TextView>(R.id.ep_number).text = "EP ${ep.number}"
        viewHolder.view.findViewById<android.widget.TextView>(R.id.ep_title).text = ep.title
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {}
}
