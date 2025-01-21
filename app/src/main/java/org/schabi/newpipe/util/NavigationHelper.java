package org.schabi.newpipe.util;

import static org.schabi.newpipe.util.external_communication.ShareUtils.installApp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.jakewharton.processphoenix.ProcessPhoenix;

import org.schabi.newpipe.MainActivity;
import org.schabi.newpipe.NewPipeDatabase;
import org.schabi.newpipe.R;
import org.schabi.newpipe.RouterActivity;
import org.schabi.newpipe.about.AboutActivity;
import org.schabi.newpipe.about.UploadActivity;
import org.schabi.newpipe.database.feed.model.FeedGroupEntity;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.fragments.MainFragment;
import org.schabi.newpipe.fragments.detail.VideoDetailFragment;
import org.schabi.newpipe.fragments.list.channel.ChannelFragment;
import org.schabi.newpipe.fragments.list.kiosk.KioskFragment;
import org.schabi.newpipe.fragments.list.playlist.PlaylistFragment;
import org.schabi.newpipe.fragments.list.search.SearchFragment;
import org.schabi.newpipe.local.bookmark.BookmarkFragment;
import org.schabi.newpipe.local.feed.FeedFragment;
import org.schabi.newpipe.local.history.StatisticsPlaylistFragment;
import org.schabi.newpipe.local.playlist.LocalPlaylistFragment;
import org.schabi.newpipe.local.subscription.SubscriptionFragment;
import org.schabi.newpipe.local.subscription.SubscriptionsImportFragment;
import org.schabi.newpipe.player.PlayerService;
import org.schabi.newpipe.player.PlayQueueActivity;
import org.schabi.newpipe.player.Player;
import org.schabi.newpipe.player.PlayerType;
import org.schabi.newpipe.player.helper.PlayerHelper;
import org.schabi.newpipe.player.helper.PlayerHolder;
import org.schabi.newpipe.player.playqueue.PlayQueue;
import org.schabi.newpipe.player.playqueue.PlayQueueItem;
import org.schabi.newpipe.settings.SettingsActivity;

public final class NavigationHelper {
    public static final String MAIN_FRAGMENT_TAG = "main_fragment_tag";
    public static final String SEARCH_FRAGMENT_TAG = "search_fragment_tag";

    private static final String TAG = NavigationHelper.class.getSimpleName();

    private NavigationHelper() {
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Players
    //////////////////////////////////////////////////////////////////////////*/
    /* INTENT */
//    called 5 times on startup / when stopped
//    3 times when paused / already playing
//    once on queued video
    @NonNull
    public static <T> Intent getPlayerIntent(@NonNull final Context context,
                                             @NonNull final Class<T> targetClazz,
                                             @Nullable final PlayQueue playQueue,
                                             final boolean resumePlayback) {
        final Intent intent = new Intent(context, targetClazz);

        if (playQueue != null) {
            final String cacheKey = SerializedCache.getInstance().put(playQueue, PlayQueue.class);
            if (cacheKey != null) {
                intent.putExtra(Player.PLAY_QUEUE_KEY, cacheKey);
            }
        }
        intent.putExtra(Player.PLAYER_TYPE, PlayerType.MAIN.valueForIntent());
        intent.putExtra(Player.RESUME_PLAYBACK, resumePlayback);
        return intent;
    }

//    only called when creating the player
    @NonNull
    public static <T> Intent getPlayerIntent(@NonNull final Context context,
                                             @NonNull final Class<T> targetClazz,
                                             @Nullable final PlayQueue playQueue,
                                             final boolean resumePlayback,
                                             final boolean playWhenReady) {
        return getPlayerIntent(context, targetClazz, playQueue, resumePlayback)
                .putExtra(Player.PLAY_WHEN_READY, playWhenReady);
    }

    @NonNull
    public static <T> Intent getPlayerEnqueueIntent(@NonNull final Context context,
                                                    @NonNull final Class<T> targetClazz,
                                                    @Nullable final PlayQueue playQueue) {
        // when enqueueing `resumePlayback` is always `false` since:
        // - if there is a video already playing, the value of `resumePlayback` just doesn't make
        //   any difference.
        // - if there is nothing already playing, it is useful for the enqueue action to have a
        //   slightly different behaviour than the normal play action: the latter resumes playback,
        //   the former doesn't. (note that enqueue can be triggered when nothing is playing only
        //   by long pressing the video detail fragment, playlist or channel controls
        return getPlayerIntent(context, targetClazz, playQueue, false)
                .putExtra(Player.ENQUEUE, true);
    }

    @NonNull
    public static <T> Intent getPlayerEnqueueNextIntent(@NonNull final Context context,
                                                        @NonNull final Class<T> targetClazz,
                                                        @Nullable final PlayQueue playQueue) {
        // see comment in `getPlayerEnqueueIntent` as to why `resumePlayback` is false
        return getPlayerIntent(context, targetClazz, playQueue, false)
                .putExtra(Player.ENQUEUE_NEXT, true);
    }

    /* PLAY */
    public static void playOnMainPlayer(final AppCompatActivity activity,
                                        @NonNull final PlayQueue playQueue) {
        final PlayQueueItem item = playQueue.getItem();
        if (item != null) {
            openVideoDetailFragment(activity, activity.getSupportFragmentManager(),
                    item.getServiceId(), item.getUrl(), item.getTitle(), playQueue,
                    false);
        }
    }

    public static void playOnMainPlayer(final Context context,
                                        @NonNull final PlayQueue playQueue,
                                        final boolean switchingPlayers) {
        final PlayQueueItem item = playQueue.getItem();
        if (item != null) {
            openVideoDetail(context,
                    item.getServiceId(), item.getUrl(), item.getTitle(), playQueue,
                    switchingPlayers);
        }
    }

    public static void playOnPopupPlayer(final Context context,
                                         final PlayQueue queue,
                                         final boolean resumePlayback) {
        if (!PermissionHelper.isPopupEnabled(context)) {
            PermissionHelper.showPopupEnablementToast(context);
            return;
        }

        Toast.makeText(context, R.string.popup_playing_toast, Toast.LENGTH_SHORT).show();
        final TimeLogger tl = TimeLogger.Companion.getInstance(context.getApplicationContext());
        tl.ignoreNextAutoplay();

        final Intent intent = getPlayerIntent(context, PlayerService.class, queue, resumePlayback);
        intent.putExtra(Player.PLAYER_TYPE, PlayerType.POPUP.valueForIntent());
        ContextCompat.startForegroundService(context, intent);
    }

    public static void playOnBackgroundPlayer(final Context context,
                                              final PlayQueue queue,
                                              final boolean resumePlayback) {
        Toast.makeText(context, R.string.background_player_playing_toast, Toast.LENGTH_SHORT)
                .show();

        final Intent intent = getPlayerIntent(context, PlayerService.class, queue, resumePlayback);
        intent.putExtra(Player.PLAYER_TYPE, PlayerType.AUDIO.valueForIntent());
        ContextCompat.startForegroundService(context, intent);
    }

    /* ENQUEUE */
    public static void enqueueOnPlayer(final Context context,
                                       final PlayQueue queue,
                                       final PlayerType playerType) {
        Toast.makeText(context, R.string.enqueued, Toast.LENGTH_SHORT).show();
        final Intent intent = getPlayerEnqueueIntent(context, PlayerService.class, queue);

        intent.putExtra(Player.PLAYER_TYPE, playerType.valueForIntent());
        ContextCompat.startForegroundService(context, intent);
    }

    public static void enqueueOnPlayer(final Context context, final PlayQueue queue) {
        PlayerType playerType = PlayerHolder.getInstance().getType();
        if (playerType == null) {
            Log.e(TAG, "Enqueueing but no player is open; defaulting to background player");
            playerType = PlayerType.AUDIO;
        }

        enqueueOnPlayer(context, queue, playerType);
    }

    /* ENQUEUE NEXT */
    public static void enqueueNextOnPlayer(final Context context, final PlayQueue queue) {
        PlayerType playerType = PlayerHolder.getInstance().getType();
        if (playerType == null) {
            Log.e(TAG, "Enqueueing next but no player is open; defaulting to background player");
            playerType = PlayerType.AUDIO;
        }
        Toast.makeText(context, R.string.enqueued_next, Toast.LENGTH_SHORT).show();
        final Intent intent = getPlayerEnqueueNextIntent(context, PlayerService.class, queue);

        intent.putExtra(Player.PLAYER_TYPE, playerType.valueForIntent());
        ContextCompat.startForegroundService(context, intent);
    }
    /*//////////////////////////////////////////////////////////////////////////
    // Through FragmentManager
    //////////////////////////////////////////////////////////////////////////*/

    @SuppressLint("CommitTransaction")
    private static FragmentTransaction defaultTransaction(final FragmentManager fragmentManager) {
        return fragmentManager.beginTransaction()
                .setCustomAnimations(R.animator.custom_fade_in, R.animator.custom_fade_out,
                        R.animator.custom_fade_in, R.animator.custom_fade_out);
    }

    public static void gotoMainFragment(final FragmentManager fragmentManager) {
        final boolean popped = fragmentManager.popBackStackImmediate(MAIN_FRAGMENT_TAG, 0);
        if (!popped) {
            openMainFragment(fragmentManager);
        }
    }

    public static void openMainFragment(final FragmentManager fragmentManager) {
        InfoCache.getInstance().trimCache();

        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, new MainFragment())
                .addToBackStack(MAIN_FRAGMENT_TAG)
                .commit();
    }

    public static boolean tryGotoSearchFragment(final FragmentManager fragmentManager) {
        if (MainActivity.DEBUG) {
            for (int i = 0; i < fragmentManager.getBackStackEntryCount(); i++) {
                Log.d("NavigationHelper", "tryGoToSearchFragment() [" + i + "]"
                        + " = [" + fragmentManager.getBackStackEntryAt(i) + "]");
            }
        }

        return fragmentManager.popBackStackImmediate(SEARCH_FRAGMENT_TAG, 0);
    }

    public static void openSearchFragment(final FragmentManager fragmentManager,
                                          final int serviceId, final String searchString) {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, SearchFragment.getInstance(serviceId, searchString))
                .addToBackStack(SEARCH_FRAGMENT_TAG)
                .commit();
    }

    public static void expandMainPlayer(final Context context) {
        context.sendBroadcast(new Intent(VideoDetailFragment.ACTION_SHOW_MAIN_PLAYER));
    }

    public static void sendPlayerStartedEvent(final Context context) {
        context.sendBroadcast(new Intent(VideoDetailFragment.ACTION_PLAYER_STARTED));
    }

    public static void showMiniPlayer(final FragmentManager fragmentManager) {
        final VideoDetailFragment instance = VideoDetailFragment.getInstanceInCollapsedState();
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_player_holder, instance)
                .runOnCommit(() -> sendPlayerStartedEvent(instance.requireActivity()))
                .commitAllowingStateLoss();
    }

    private interface RunnableWithVideoDetailFragment {
        void run(VideoDetailFragment detailFragment);
    }

    public static void openVideoDetailFragment(@NonNull final Context context,
                                               @NonNull final FragmentManager fragmentManager,
                                               final int serviceId,
                                               @Nullable final String url,
                                               @NonNull final String title,
                                               @Nullable final PlayQueue playQueue,
                                               final boolean switchingPlayers) {

        final boolean autoPlay;
        @Nullable final PlayerType playerType = PlayerHolder.getInstance().getType();
        if (playerType == null) {
            // no player open
            autoPlay = PlayerHelper.isAutoplayAllowedByUser(context);
        } else if (switchingPlayers) {
            // switching player to main player
            autoPlay = PlayerHolder.getInstance().isPlaying(); // keep play/pause state
        } else if (playerType == PlayerType.MAIN) {
            // opening new stream while already playing in main player
            autoPlay = PlayerHelper.isAutoplayAllowedByUser(context);
        } else {
            // opening new stream while already playing in another player
            autoPlay = false;
        }

        final RunnableWithVideoDetailFragment onVideoDetailFragmentReady = detailFragment -> {
            expandMainPlayer(detailFragment.requireActivity());
            detailFragment.setAutoPlay(autoPlay);
            if (switchingPlayers) {
                // Situation when user switches from players to main player. All needed data is
                // here, we can start watching (assuming newQueue equals playQueue).
                // Starting directly in fullscreen if the previous player type was popup.
                detailFragment.openVideoPlayer(playerType == PlayerType.POPUP
                        || PlayerHelper.isStartMainPlayerFullscreenEnabled(context));
            } else {
                detailFragment.selectAndLoadVideo(serviceId, url, title, playQueue);
            }
            detailFragment.scrollToTop();
        };

        final Fragment fragment = fragmentManager.findFragmentById(R.id.fragment_player_holder);
        if (fragment instanceof VideoDetailFragment && fragment.isVisible()) {
            onVideoDetailFragmentReady.run((VideoDetailFragment) fragment);
        } else {
            final VideoDetailFragment instance = VideoDetailFragment
                    .getInstance(serviceId, url, title, playQueue);
            instance.setAutoPlay(autoPlay);

            defaultTransaction(fragmentManager)
                    .replace(R.id.fragment_player_holder, instance)
                    .runOnCommit(() -> onVideoDetailFragmentReady.run(instance))
                    .commit();
        }
    }

    public static void openChannelFragment(final FragmentManager fragmentManager,
                                           final int serviceId, final String url,
                                           @NonNull final String name, final String ingress) {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, ChannelFragment.getInstance(serviceId, url, name,
                        ingress))
                .addToBackStack(null)
                .commit();
    }

    public static void openChannelFragment(@NonNull final Fragment fragment,
                                           @NonNull final StreamInfoItem item,
                                           final String uploaderUrl, final String ingress) {
        // For some reason `getParentFragmentManager()` doesn't work, but this does.
        openChannelFragment(
                fragment.requireActivity().getSupportFragmentManager(),
                item.getServiceId(), uploaderUrl, item.getUploaderName(), ingress);
    }

    public static void openPlaylistFragment(final FragmentManager fragmentManager,
                                            final int serviceId, final String url,
                                            @NonNull final String name, final String ingress) {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, PlaylistFragment.getInstance(serviceId, url, name,
                        ingress))
                .addToBackStack(null)
                .commit();
    }

    public static void openFeedFragment(final FragmentManager fragmentManager) {
        openFeedFragment(fragmentManager, FeedGroupEntity.GROUP_ALL_ID, null);
    }

    public static void openFeedFragment(final FragmentManager fragmentManager, final long groupId,
                                        @Nullable final String groupName) {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, FeedFragment.newInstance(groupId, groupName))
                .addToBackStack(null)
                .commit();
    }

    public static void openBookmarksFragment(final FragmentManager fragmentManager) {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, new BookmarkFragment())
                .addToBackStack(null)
                .commit();
    }

    public static void openSubscriptionFragment(final FragmentManager fragmentManager) {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, new SubscriptionFragment())
                .addToBackStack(null)
                .commit();
    }

    public static void openKioskFragment(final FragmentManager fragmentManager, final int serviceId,
                                         final String kioskId) throws ExtractionException {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, KioskFragment.getInstance(serviceId, kioskId))
                .addToBackStack(null)
                .commit();
    }

    public static void openLocalPlaylistFragment(final FragmentManager fragmentManager,
                                                 final long playlistId, final String name) {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, LocalPlaylistFragment.getInstance(playlistId,
                        name == null ? "" : name))
                .addToBackStack(null)
                .commit();
    }

    public static void openStatisticFragment(final FragmentManager fragmentManager) {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, new StatisticsPlaylistFragment())
                .addToBackStack(null)
                .commit();
    }

    public static void openSubscriptionsImportFragment(final FragmentManager fragmentManager,
                                                       final int serviceId) {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, SubscriptionsImportFragment.getInstance(serviceId))
                .addToBackStack(null)
                .commit();
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Through Intents
    //////////////////////////////////////////////////////////////////////////*/

    public static void openSearch(final Context context, final int serviceId,
                                  final String searchString) {
        final Intent mIntent = new Intent(context, MainActivity.class);
        mIntent.putExtra(Constants.KEY_SERVICE_ID, serviceId);
        mIntent.putExtra(Constants.KEY_SEARCH_STRING, searchString);
        mIntent.putExtra(Constants.KEY_OPEN_SEARCH, true);
        context.startActivity(mIntent);
    }

    public static void openVideoDetail(final Context context,
                                       final int serviceId,
                                       final String url,
                                       @NonNull final String title,
                                       @Nullable final PlayQueue playQueue,
                                       final boolean switchingPlayers) {

        final Intent intent = getOpenIntent(context, url, serviceId,
                StreamingService.LinkType.STREAM);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Constants.KEY_TITLE, title);
        intent.putExtra(VideoDetailFragment.KEY_SWITCHING_PLAYERS, switchingPlayers);

        if (playQueue != null) {
            final String cacheKey = SerializedCache.getInstance().put(playQueue, PlayQueue.class);
            if (cacheKey != null) {
                intent.putExtra(Player.PLAY_QUEUE_KEY, cacheKey);
            }
        }
        context.startActivity(intent);
    }

    /**
     * Opens {@link ChannelFragment}.
     * Use this instead of {@link #openChannelFragment(FragmentManager, int, String, String)}
     * when no fragments are used / no FragmentManager is available.
     * @param context
     * @param serviceId
     * @param url
     * @param title
     */
    public static void openChannelFragmentUsingIntent(final Context context,
                                                      final int serviceId,
                                                      final String url,
                                                      @NonNull final String title) {
        final Intent intent = getOpenIntent(context, url, serviceId,
                StreamingService.LinkType.CHANNEL);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Constants.KEY_TITLE, title);

        context.startActivity(intent);
    }

    public static void openMainActivity(final Context context) {
        final Intent mIntent = new Intent(context, MainActivity.class);
        mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(mIntent);
    }

    public static void openRouterActivity(final Context context, final String url) {
        final Intent mIntent = new Intent(context, RouterActivity.class);
        mIntent.setData(Uri.parse(url));
        context.startActivity(mIntent);
    }

    public static void openAbout(final Context context) {
        final Intent intent = new Intent(context, AboutActivity.class);
        context.startActivity(intent);
    }

    public static void openUpload(final Context context) {
        final Intent intent = new Intent(context, UploadActivity.class);
        context.startActivity(intent);
    }

    public static void openSettings(final Context context) {
        final Intent intent = new Intent(context, SettingsActivity.class);
        context.startActivity(intent);
    }

    public static Intent getPlayQueueActivityIntent(final Context context) {
        final Intent intent = new Intent(context, PlayQueueActivity.class);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        return intent;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Link handling
    //////////////////////////////////////////////////////////////////////////*/

    private static Intent getOpenIntent(final Context context, final String url,
                                        final int serviceId, final StreamingService.LinkType type) {
        final Intent mIntent = new Intent(context, MainActivity.class);
        mIntent.putExtra(Constants.KEY_SERVICE_ID, serviceId);
        mIntent.putExtra(Constants.KEY_URL, url);
        mIntent.putExtra(Constants.KEY_LINK_TYPE, type);
        return mIntent;
    }

    public static Intent getIntentByLink(final Context context, final String url)
            throws ExtractionException {
        return getIntentByLink(context, NewPipe.getServiceByUrl(url), url);
    }

    public static Intent getIntentByLink(final Context context,
                                         final StreamingService service,
                                         final String url) throws ExtractionException {
        final StreamingService.LinkType linkType = service.getLinkTypeByUrl(url);

        if (linkType == StreamingService.LinkType.NONE) {
            throw new ExtractionException("Url not known to service. service=" + service
                    + " url=" + url);
        }

        return getOpenIntent(context, url, service.getServiceId(), linkType);
    }

    public static Intent getChannelIntent(final Context context,
                                          final int serviceId,
                                          final String url) {
        return getOpenIntent(context, url, serviceId, StreamingService.LinkType.CHANNEL);
    }

    /**
     * Start an activity to install Kore.
     *
     * @param context the context
     */
    public static void installKore(final Context context) {
        installApp(context, context.getString(R.string.kore_package));
    }

    /**
     * Finish this <code>Activity</code> as well as all <code>Activities</code> running below it
     * and then start <code>MainActivity</code>.
     *
     * @param activity the activity to finish
     */
    public static void restartApp(final Activity activity) {
        NewPipeDatabase.close();

        ProcessPhoenix.triggerRebirth(activity.getApplicationContext());
    }
}
