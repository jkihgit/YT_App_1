package org.schabi.newpipe.ktx.feed.service

data class FeedLoadState(
    val updateDescription: String,
    val maxProgress: Int,
    val currentProgress: Int,
)
