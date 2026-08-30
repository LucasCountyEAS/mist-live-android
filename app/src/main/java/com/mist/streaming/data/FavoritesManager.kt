package com.mist.streaming.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages the "Favorites" channel list using SharedPreferences.
 */
object FavoritesManager {
    private const val PREFS_NAME = "mist_tv_prefs"
    private const val KEY_FAVORITES = "favorite_channels"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Toggles a channel's favorite status.
     * Returns true if now favorited, false if removed.
     */
    fun toggleFavorite(context: Context, channelId: String): Boolean {
        val prefs = getPrefs(context)
        val favoritesJson = prefs.getString(KEY_FAVORITES, "") ?: ""
        val favoritesList = if (favoritesJson.isEmpty()) mutableSetOf() else favoritesJson.split(",").toMutableSet()

        val isAdded = if (favoritesList.contains(channelId)) {
            favoritesList.remove(channelId)
            false
        } else {
            favoritesList.add(channelId)
            true
        }

        prefs.edit().putString(KEY_FAVORITES, favoritesList.joinToString(",")).apply()
        return isAdded
    }

    /**
     * Returns the list of favorite channel IDs.
     */
    fun getFavoriteChannelIds(context: Context): List<String> {
        val prefs = getPrefs(context)
        val favoritesJson = prefs.getString(KEY_FAVORITES, "") ?: ""
        return if (favoritesJson.isEmpty()) emptyList() else favoritesJson.split(",")
    }

    fun isFavorite(context: Context, channelId: String): Boolean {
        return getFavoriteChannelIds(context).contains(channelId)
    }
}
