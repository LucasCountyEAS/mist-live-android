package com.mist.streaming.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Utility to check if a stream URL is online.
 */
object ChannelStatusChecker {

    suspend fun checkStatus(streamUrl: String): Boolean = withContext(Dispatchers.IO) {
        if (streamUrl.startsWith("rtmp://")) {
            return@withContext true
        }

        try {
            val url = URL(streamUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            val responseCode = connection.responseCode
            responseCode in 200..299
        } catch (e: Exception) {
            false
        }
    }
}
