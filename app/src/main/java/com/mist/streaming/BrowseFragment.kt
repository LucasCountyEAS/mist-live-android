package com.mist.streaming

import android.content.Intent
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.app.BrowseSupportFragment.BrowseTransitionListener
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.DividerRow
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.SearchOrbView
import androidx.leanback.widget.TitleViewAdapter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.mist.streaming.data.Channel
import com.mist.streaming.data.ChannelRepository
import com.mist.streaming.data.RecentChannelsManager
import com.mist.streaming.data.FavoritesManager
import com.mist.streaming.ui.ChannelCardPresenter

/**
 * Custom title view: mistlive icon + left-aligned title text, still includes a search
 * orb so BrowseSupportFragment's search affordance keeps working. Implements
 * TitleViewAdapter.Provider so it can be returned from onInflateTitleView().
 */
class MistTitleView(context: Context) : FrameLayout(context), TitleViewAdapter.Provider {

    private lateinit var searchOrbView: SearchOrbView
    // Headless title text holder — required by TitleViewAdapter's contract even though
    // nothing displays it now that the icon stands alone; keeps setTitle()/getTitle() valid.
    private val titleText: TextView = TextView(context)

    private val titleViewAdapter = object : TitleViewAdapter() {
        override fun getSearchAffordanceView(): View = searchOrbView

        override fun setTitle(title: CharSequence?) {
            titleText.text = title
        }

        override fun getTitle(): CharSequence = titleText.text

        override fun setBadgeDrawable(drawable: Drawable?) {
            // Badge is fixed to the mistlive icon in this layout; ignored.
        }

        override fun getBadgeDrawable(): Drawable? = null
    }

    init {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
            // Overscan-safe margins for older TVs: ~48dp horizontal, ~27dp vertical
            setPadding(dpToPx(context, 48), dpToPx(context, 27), dpToPx(context, 48), dpToPx(context, 27))
        }

        val iconHeightPx = dpToPx(context, 46)
        val iconWidthPx = (iconHeightPx * (326.65f / 83.84f)).toInt()
        val icon = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(iconWidthPx, iconHeightPx)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageDrawable(ContextCompat.getDrawable(context, R.drawable.mistlive))
        }

        container.addView(icon)
        addView(container)

        // Search orb, required by TitleViewAdapter.Provider contract; kept invisible
        // since search isn't wired up in this custom layout.
        searchOrbView = SearchOrbView(context).apply {
            visibility = View.GONE
        }
        addView(searchOrbView)
    }

    override fun getTitleViewAdapter(): TitleViewAdapter = titleViewAdapter

    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}

/**
 * The main TV Guide screen.
 */
class BrowseFragment : BrowseSupportFragment() {

    private lateinit var rowsAdapter: ArrayObjectAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.background = null
        setupUi()
        setupClickListener()
        setupTransitionListener()
    }

    /**
     * Supplies our custom left-aligned title view. Leanback calls this during its own
     * view creation and adds the returned view into the title area itself — calling
     * setTitleView() manually afterward (the previous approach) does NOT insert the view
     * into the hierarchy, it only updates internal bookkeeping, which is why nothing rendered.
     */
    override fun onInflateTitleView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ): View {
        return MistTitleView(inflater.context)
    }

    override fun onResume() {
        super.onResume()
        loadChannels()
    }

    private fun setupTransitionListener() {
        val gradientView = requireActivity().findViewById<View>(R.id.sidebar_gradient_view)
        setBrowseTransitionListener(object : BrowseTransitionListener() {
            override fun onHeadersTransitionStart(withHeaders: Boolean) {
                gradientView?.animate()?.alpha(if (withHeaders) 1f else 0f)?.setDuration(300)?.start()
                // Synchronize viewer count badge visibility with sidebar transition
                ChannelCardPresenter.setSidebarOpen(withHeaders)
            }
        })
    }

    private fun setupUi() {
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        brandColor = android.graphics.Color.TRANSPARENT
        searchAffordanceColor = requireContext().getColor(R.color.mist_accent)
    }

    private fun loadChannels() {
        lifecycleScope.launch {
            ChannelRepository.refreshChannels()

            rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

            // Pass long click listener to presenter
            val cardPresenter = ChannelCardPresenter { channel ->
                val isFavorite = FavoritesManager.toggleFavorite(requireContext(), channel.id)
                val message = if (isFavorite) "Added to Favorites" else "Removed from Favorites"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                loadChannels() // Refresh UI
            }

            var rowIndex = 0L
            var priorityRowAdded = false

            // 1. Favorites (highest priority)
            val favoriteIds = FavoritesManager.getFavoriteChannelIds(requireContext())
            if (favoriteIds.isNotEmpty()) {
                val favoriteChannels = favoriteIds.mapNotNull { id -> ChannelRepository.findById(id) }
                if (favoriteChannels.isNotEmpty()) {
                    val favoriteAdapter = ArrayObjectAdapter(cardPresenter)
                    favoriteChannels.forEach { favoriteAdapter.add(it) }
                    rowsAdapter.add(ListRow(HeaderItem(rowIndex++, "Favorites"), favoriteAdapter))
                    priorityRowAdded = true
                }
            }

            // 2. All Channels, alphabetical
            val allChannelsSorted = ChannelRepository.getAll().sortedBy { it.name.lowercase() }
            if (allChannelsSorted.isNotEmpty()) {
                val allChannelsAdapter = ArrayObjectAdapter(cardPresenter)
                allChannelsSorted.forEach { allChannelsAdapter.add(it) }
                rowsAdapter.add(ListRow(HeaderItem(rowIndex++, "All Channels"), allChannelsAdapter))
                priorityRowAdded = true
            }

            // 3. Recently Watched
            val recentIds = RecentChannelsManager.getRecentChannelIds(requireContext())
            if (recentIds.isNotEmpty()) {
                val recentChannels = recentIds.mapNotNull { id -> ChannelRepository.findById(id) }
                if (recentChannels.isNotEmpty()) {
                    val recentAdapter = ArrayObjectAdapter(cardPresenter)
                    recentChannels.forEach { recentAdapter.add(it) }
                    rowsAdapter.add(ListRow(HeaderItem(rowIndex++, "Recently Watched"), recentAdapter))
                    priorityRowAdded = true
                }
            }

            // Divider separating the priority rows above from the categorical rows below
            if (priorityRowAdded) {
                rowsAdapter.add(DividerRow())
            }

            // 4. Remaining categories
            ChannelRepository.getGroupedByCategory().entries.forEach { (category, channels) ->
                val listRowAdapter = ArrayObjectAdapter(cardPresenter)
                channels.forEach { listRowAdapter.add(it) }
                rowsAdapter.add(ListRow(HeaderItem(rowIndex++, category), listRowAdapter))
            }

            adapter = rowsAdapter
        }
    }

    private fun setupClickListener() {
        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            if (item is Channel) {
                RecentChannelsManager.addRecentChannel(requireContext(), item.id)
                val intent = Intent(requireContext(), PlaybackActivity::class.java).apply {
                    putExtra(PlaybackActivity.EXTRA_CHANNEL, item)
                }
                startActivity(intent)
            }
        }
    }
}