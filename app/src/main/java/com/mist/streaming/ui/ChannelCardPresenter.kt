package com.mist.streaming.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.mist.streaming.R
import com.mist.streaming.data.Channel

/**
 * Leanback Presenter that renders each Channel as an [ImageCardView] with a custom
 * info row (logo + title/description) below it, since Leanback's built-in badge
 * slot cannot be repositioned next to the title.
 */
class ChannelCardPresenter(private val onLongClickListener: ((Channel) -> Unit)? = null) : Presenter() {

    companion object {
        private const val CARD_WIDTH_DP = 320
        private const val CARD_HEIGHT_DP = 180
        private const val LOGO_SIZE_DP = 32
        private const val PLACEHOLDER_DELAY_MS = 5000L

        private var isSidebarOpen = false
        private val vhList = mutableSetOf<ChannelViewHolder>()

        fun setSidebarOpen(open: Boolean) {
            isSidebarOpen = open
            vhList.forEach { vh -> vh.updateBadgeVisibility(isSidebarOpen) }
        }
    }

    private class ChannelViewHolder(
        rootView: View,
        val cardView: ImageCardView,
        val logoView: ImageView,
        val titleView: TextView,
        val descriptionView: TextView
    ) : ViewHolder(rootView) {
        val viewerCountView: TextView = cardView.findViewWithTag("viewer_count")
        var viewership: Int = 0

        fun updateBadgeVisibility(sidebarOpen: Boolean) {
            viewerCountView.text = "● $viewership"
            viewerCountView.visibility = if (sidebarOpen) View.GONE else View.VISIBLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val context = parent.context

        val cardView = ImageCardView(context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            elevation = 0f
            stateListAnimator = null
            setMainImageDimensions(
                dpToPx(context, CARD_WIDTH_DP),
                dpToPx(context, CARD_HEIGHT_DP)
            )
            setBackgroundColor(ContextCompat.getColor(context, R.color.mist_card_bg))
            setCardType(ImageCardView.CARD_TYPE_MAIN_ONLY)
        }
        // ImageCardView's internal mainImageView defaults to CENTER_CROP regardless of
        // Glide's own transform, which is why non-16:9 thumbnails were still filling/cropping
        // the box. Overriding it here to letterbox instead.
        cardView.mainImageView.scaleType = ImageView.ScaleType.FIT_CENTER

        val viewerCountView = TextView(context).apply {
            tag = "viewer_count"
            setTextColor(Color.WHITE)
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            val paddingH = dpToPx(context, 6)
            val paddingV = dpToPx(context, 2)
            setPadding(paddingH, paddingV, paddingH, paddingV)
            background = ContextCompat.getDrawable(context, R.drawable.live_badge_bg)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                setMargins(0, dpToPx(context, 8), dpToPx(context, 8), 0)
            }
            visibility = if (isSidebarOpen) View.GONE else View.VISIBLE
        }
        cardView.addView(viewerCountView)

        // Custom info row: logo on the left, title + description stacked on the right
        val logoSizePx = dpToPx(context, LOGO_SIZE_DP)
        val logoView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(logoSizePx, logoSizePx).apply {
                marginEnd = dpToPx(context, 10)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        val titleView = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            maxLines = 1
        }

        val descriptionView = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.mist_text_secondary))
            textSize = 12f
            maxLines = 2
        }

        val textStack = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(titleView)
            addView(descriptionView)
        }

        val infoRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(context, CARD_WIDTH_DP),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            background = ContextCompat.getDrawable(context, R.drawable.info_row_outline)
            setPadding(dpToPx(context, 8), dpToPx(context, 8), dpToPx(context, 8), dpToPx(context, 8))
            addView(logoView)
            addView(textStack)
        }

        // Root just stacks the original (untouched) cardView above our info row.
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(cardView)
            addView(infoRow)
        }

        // Raise the whole root above sibling rows while focused, so the card's
        // built-in focus-zoom doesn't get visually underlapped by the row below it.
        cardView.setOnFocusChangeListener { _, hasFocus ->
            root.z = if (hasFocus) 10f else 0f
        }

        return ChannelViewHolder(root, cardView, logoView, titleView, descriptionView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val channel = item as Channel
        val vh = viewHolder as ChannelViewHolder
        val cardView = vh.cardView
        val context = cardView.context

        vh.viewership = channel.viewership
        vh.titleView.text = channel.name
        vh.descriptionView.text = channel.description

        Glide.with(context)
            .load(channel.logoUrl)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(vh.logoView)

        if (onLongClickListener != null) {
            cardView.setOnLongClickListener {
                onLongClickListener.invoke(channel)
                true
            }
        }

        vhList.add(vh)
        vh.updateBadgeVisibility(isSidebarOpen)

        cancelPlaceholder(cardView)

        val placeholderRunnable = Runnable {
            cardView.mainImage = ContextCompat.getDrawable(context, R.drawable.placeholder_channel)
        }
        cardView.tag = placeholderRunnable
        cardView.postDelayed(placeholderRunnable, PLACEHOLDER_DELAY_MS)

        Glide.with(context)
            .load(channel.thumbnailUrl)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    cancelPlaceholder(cardView)
                    cardView.mainImage = resource
                }
                override fun onLoadStarted(placeholder: Drawable?) {}
                override fun onLoadFailed(errorDrawable: Drawable?) {}
                override fun onLoadCleared(placeholder: Drawable?) {
                    cancelPlaceholder(cardView)
                    cardView.mainImage = null
                }
            })
    }

    private fun cancelPlaceholder(cardView: ImageCardView) {
        val runnable = cardView.tag as? Runnable
        if (runnable != null) {
            cardView.removeCallbacks(runnable)
            cardView.tag = null
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val vh = viewHolder as ChannelViewHolder
        vhList.remove(vh)
        val cardView = vh.cardView
        cancelPlaceholder(cardView)
        cardView.mainImage = null
        cardView.setOnLongClickListener(null)
        Glide.with(cardView.context).clear(cardView.mainImageView)
        Glide.with(vh.logoView.context).clear(vh.logoView)
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}