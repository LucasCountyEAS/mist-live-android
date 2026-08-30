package com.mist.streaming.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages the "Recently Watched" channel list using SharedPreferences.
 */
object RecentChannelsManager {
    private const val PREFS_NAME = "mist_tv_prefs"
    private const val KEY_RECENT_CHANNELS = "recent_channels"
    private const val MAX_RECENT = 10

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Adds a channel ID to the top of the recent list.
     */
    fun addRecentChannel(context: Context, channelId: String) {
        val prefs = getPrefs(context)
        val recentJson = prefs.getString(KEY_RECENT_CHANNELS, "") ?: ""
        val recentList = if (recentJson.isEmpty()) mutableListOf() else recentJson.split(",").toMutableList()

        // Remove if already exists to move it to top
        recentList.remove(channelId)
        recentList.add(0, channelId)

        // Limit size
        val limitedList = recentList.take(MAX_RECENT)
        prefs.edit().putString(KEY_RECENT_CHANNELS, limitedList.joinToString(",")).apply()
    }

    /**
     * Returns the list of recently watched channel IDs.
     */
    fun getRecentChannelIds(context: Context): List<String> {
        val prefs = getPrefs(context)
        val recentJson = prefs.getString(KEY_RECENT_CHANNELS, "") ?: ""
        return if (recentJson.isEmpty()) emptyList() else recentJson.split(",")
    }
}
