package org.schabi.newpipe.ktx.feed

import androidx.annotation.StringRes
import org.schabi.newpipe.ktx.feed.item.StreamItem
import java.time.OffsetDateTime

sealed class FeedState {
    data class ProgressState(
        val currentProgress: Int = -1,
        val maxProgress: Int = -1,
        @StringRes val progressMessage: Int = 0
    ) : FeedState()

    data class LoadedState(
        val items: List<StreamItem>,
        val oldestUpdate: OffsetDateTime? = null,
        val notLoadedCount: Long,
        val itemsErrors: List<Throwable> = emptyList()
    ) : FeedState()

    data class ErrorState(
        val error: Throwable? = null
    ) : FeedState()
}
