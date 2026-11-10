package com.youssef.animedextv

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

class AnimeCardPresenter : Presenter() {

    companion object {
        private const val CARD_WIDTH = 220
        private const val CARD_HEIGHT = 310
    }

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = ImageCardView(parent.context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
            setMainImageScaleType(ImageView.ScaleType.CENTER_CROP)
        }
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val anime = item as Anime
        val cardView = viewHolder.view as ImageCardView
        cardView.titleText = anime.title
        cardView.contentText = listOfNotNull(
            anime.type.takeIf { it.isNotEmpty() },
            anime.status.takeIf { it.isNotEmpty() }
        ).joinToString(" · ")

        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)

        if (anime.image.isNotEmpty()) {
            Glide.with(cardView.context)
                .load(anime.image)
                .centerCrop()
                .error(R.drawable.placeholder)
                .into(cardView.mainImageView!!)
        } else {
            cardView.mainImageView?.setImageResource(R.drawable.placeholder)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        cardView.badgeImage = null
        cardView.mainImage = null
    }
}

