package com.youssef.animedextv

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.ui.StyledPlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerActivity : FragmentActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: StyledPlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        setTheme(R.style.Theme_AnimeDexTV_Player)

        playerView = findViewById(R.id.player_view)
        playerView.keepScreenOn = true

        val episodeId = intent.getStringExtra("episode_id") ?: ""
        val title = intent.getStringExtra("episode_title") ?: ""
        val animeTitle = intent.getStringExtra("anime_title") ?: ""

        if (episodeId.isNotEmpty()) {
            loadAndPlay(episodeId)
        } else {
            Toast.makeText(this, "No episode ID", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadAndPlay(episodeId: String) {
        lifecycleScope.launch {
            try {
                val sources = withContext(Dispatchers.IO) {
                    ApiService.getStreamSources(episodeId)
                }
                if (sources.isEmpty()) {
                    Toast.makeText(this@PlayerActivity, "No stream found", Toast.LENGTH_LONG).show()
                    return@launch
                }
                playUrl(sources[0].url, sources[0].isM3u8)
            } catch (e: Exception) {
                Toast.makeText(this@PlayerActivity, "Stream error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun playUrl(url: String, isHls: Boolean) {
        player = ExoPlayer.Builder(this).build().also { exo ->
            playerView.player = exo

            val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
                setDefaultRequestProperties(mapOf("referer" to "https://animedex.vercel.app"))
            }

            val mediaItem = MediaItem.fromUri(Uri.parse(url))
            if (isHls) {
                val source = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
                exo.setMediaSource(source)
            } else {
                exo.setMediaItem(mediaItem)
            }

            exo.prepare()
            exo.playWhenReady = true

            exo.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Toast.makeText(this@PlayerActivity, "Playback error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent): Boolean {
        return playerView.dispatchKeyEvent(event) || super.onKeyDown(keyCode, event)
    }
}

