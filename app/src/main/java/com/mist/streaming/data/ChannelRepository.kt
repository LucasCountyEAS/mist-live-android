package com.mist.streaming.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * Channel repository for Mist Streaming.
 *
 * Replace the sample entries below with your real channel data.
 * streamUrl supports:
 *   - RTMP:  rtmp://your-server/live/stream-key
 *   - HLS:   https://your-server/live/stream.m3u8
 *   - DASH:  https://your-server/live/manifest.mpd
 */
object ChannelRepository {

    private var channels = emptyList<Channel>()
    /** Returns all channels. */
    fun getAll(): List<Channel> = channels

    /** Returns channels grouped by category, with categories sorted alphabetically and channels in each category sorted by name. */
    fun getGroupedByCategory(): Map<String, List<Channel>> {
        return channels.groupBy { it.category }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
            .mapValues { (_, list) ->
                list.sortedBy { it.name }
            }
    }

    /** Find a channel by its ID. */
    fun findById(id: String): Channel? = channels.find { it.id == id }

    /**
     * Fetches the latest channel list from the Mist Streaming API and updates the local repository.
     */
    suspend fun refreshChannels() = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.mistlive.tv/api/v1.5/channels?t=${System.currentTimeMillis()}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.useCaches = false
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)

            // fetch descriptions from the public-channels endpoint and build a channel_id -> description lookup
            val descriptions = fetchDescriptions()

            val timestamp = System.currentTimeMillis()
            val newChannels = mutableListOf<Channel>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val title = obj.getString("title")
                val iconUuid = obj.optString("icon", "")

                // ... (category extraction)
                val categories = obj.optJSONArray("categories")
                val category = if (categories != null && categories.length() > 0) {
                    categories.getString(0)
                } else {
                    "Uncategorized"
                }

                val description = descriptions[id]?.let { stripMarkdown(it) } ?: ""

                // Construct URLs based on known patterns
                val logoUrl = resolveIconUrl(iconUuid)

                newChannels.add(
                    Channel(
                        id = id,
                        name = title,
                        description = description,
                        thumbnailUrl = "https://capture.mistlive.tv/$id.hq.webp?v=$timestamp",
                        logoUrl = logoUrl,
                        streamUrl = "https://watch.mistlive.tv/hls/$id/playlist.m3u8",
                        category = category,
                        isLive = obj.optBoolean("online", true),
                        viewership = obj.optInt("viewership", 0)
                    )
                )
            }

            // Filter out offline channels
            channels = newChannels.filter { it.isLive }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Fetches channel_id -> description pairs from the public-channels endpoint.
     */
    private fun fetchDescriptions(): Map<String, String> {
        return try {
            val url = URL("https://api.mistlive.tv/api/public-channels")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.useCaches = false
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)

            val map = mutableMapOf<String, String>()
            for (i in 0 until jsonArray.length()) {
                val entry = jsonArray.getJSONObject(i)
                val channelId = entry.optString("channel_id", "")
                val channelDescription = entry.optString("channel_description", "")
                if (channelId.isNotEmpty() && channelDescription.isNotEmpty()) {
                    map[channelId] = channelDescription
                }
            }
            map
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    /**
     * Resolves an icon UUID to an image URL, falling back to a default icon if the
     * UUID is missing or if the resolved resource turns out to be.. not supported or something.
     */
    private fun resolveIconUrl(iconUuid: String): String {
        if (iconUuid.isEmpty()) {
            return "https://file.garden/anfFhGxO-geaMQ6-/MistDefaultChannelIconWhite.png"
        }

        val url = "https://api.mistlive.tv/api/v1.5/image/$iconUuid?width=256&height=256&fit=inside"

        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.useCaches = false
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val firstChars = response.take(5).lowercase()
            if (firstChars == "<?xml" || firstChars == "<svg ") {
                "https://file.garden/anfFhGxO-geaMQ6-/MistDefaultChannelIconWhite.png"
            } else {
                url
            }
        } catch (e: Exception) {
            "https://file.garden/anfFhGxO-geaMQ6-/MistDefaultChannelIconWhite.png"
        }
    }

    /**
     * Strips common markdown syntax down to plain text.
     */
    private fun stripMarkdown(text: String): String {
        if (text.isEmpty()) return ""

        var result = text

        // bold/italic: **text** or __text__ or *text* or _text_
        result = result.replace("**", "")
        result = result.replace("__", "")
        result = result.replace("*", "")
        result = result.replace("_", "")

        // headers: leading # symbols
        result = result.replace("#", "")

        // inline code / code blocks
        result = result.replace("`", "")

        // strikethrough
        result = result.replace("~~", "")

        // links: [text](url) -> just show the text
        val linkRegex = Regex("""\[([^\]]*)\]\([^)]*\)""")
        result = result.replace(linkRegex) { it.groupValues[1] }

        return result
    }
}