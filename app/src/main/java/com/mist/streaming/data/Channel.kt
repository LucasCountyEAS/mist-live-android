package com.mist.streaming.data

import java.io.Serializable

/**
 * Represents a single channel in the Mist TV Guide.
 */
data class Channel(
    val id: String,
    val name: String,
    val description: String,
    val thumbnailUrl: String,
    val logoUrl: String,
    val streamUrl: String,
    val category: String,
    val isLive: Boolean = true,
    val viewership: Int = 0
) : Serializable
