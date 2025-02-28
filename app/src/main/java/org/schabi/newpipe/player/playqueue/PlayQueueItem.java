package org.schabi.newpipe.player.playqueue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.util.ExtractorHelper;

import java.io.Serializable;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class PlayQueueItem implements Serializable {
    public static final long RECOVERY_UNSET = Long.MIN_VALUE;
    private static final String EMPTY_STRING = "";

    @NonNull
    private final String title;
    @NonNull
    private final String url;
    private final int serviceId;
    private final long duration;
    @NonNull
    private final String thumbnailUrl;
    @NonNull
    private final String uploader;
    private final String uploaderUrl;
    @NonNull
    private final StreamType streamType;

    private boolean isAutoQueued;

    private long recoveryPosition;
    private Throwable error;
    public String ingress;

    PlayQueueItem(@NonNull final StreamInfo info) {
        this(info.getName(), info.getUrl(), info.getServiceId(), info.getDuration(),
                info.getThumbnailUrl(), info.getUploaderName(),
                info.getUploaderUrl(), info.getStreamType());

        if (info.getStartPosition() > 0) {
            setRecoveryPosition(info.getStartPosition() * 1000);
        }
    }

    PlayQueueItem(@NonNull final StreamInfoItem item) {
        this(item.getName(), item.getUrl(), item.getServiceId(), item.getDuration(),
                item.getThumbnailUrl(), item.getUploaderName(),
                item.getUploaderUrl(), item.getStreamType());
    }

    @SuppressWarnings("ParameterNumber")
    private PlayQueueItem(@Nullable final String name, @Nullable final String url,
                          final int serviceId, final long duration,
                          @Nullable final String thumbnailUrl, @Nullable final String uploader,
                          final String uploaderUrl, @NonNull final StreamType streamType) {
        this.title = name != null ? name : EMPTY_STRING;
        this.url = url != null ? url : EMPTY_STRING;
        this.serviceId = serviceId;
        this.duration = duration;
        this.thumbnailUrl = thumbnailUrl != null ? thumbnailUrl : EMPTY_STRING;
        this.uploader = uploader != null ? uploader : EMPTY_STRING;
        this.uploaderUrl = uploaderUrl;
        this.streamType = streamType;

        this.recoveryPosition = RECOVERY_UNSET;
        this.ingress = "unknown";
    }

    public static String getIngressFromSII(final StreamInfoItem sii) {
        final String fixedTitle = sii.getName() != null ? sii.getName() : EMPTY_STRING;
        return fixedTitle.substring(fixedTitle.lastIndexOf('/') + 1);
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getUrl() {
        return url;
    }

    public int getServiceId() {
        return serviceId;
    }

    public long getDuration() {
        return duration;
    }

    @NonNull
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    @NonNull
    public String getUploader() {
        return uploader;
    }

    public String getUploaderUrl() {
        return uploaderUrl;
    }

    @NonNull
    public StreamType getStreamType() {
        return streamType;
    }

    public long getRecoveryPosition() {
        return recoveryPosition;
    }

    /*package-private*/ void setRecoveryPosition(final long recoveryPosition) {
        this.recoveryPosition = recoveryPosition;
    }

    @Nullable
    public Throwable getError() {
        return error;
    }

    @NonNull
    public Single<StreamInfo> getStream() {
        return ExtractorHelper.getStreamInfo(this.serviceId, this.url, false)
                .subscribeOn(Schedulers.io())
                .doOnError(throwable -> error = throwable);
    }

    public boolean isAutoQueued() {
        return isAutoQueued;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Item States, keep external access out
    ////////////////////////////////////////////////////////////////////////////

    public void setAutoQueued(final boolean autoQueued) {
        isAutoQueued = autoQueued;
    }
}
