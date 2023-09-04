package com.brentvatne.exoplayer;

import static com.google.android.exoplayer2.C.CONTENT_TYPE_DASH;
import static com.google.android.exoplayer2.C.CONTENT_TYPE_HLS;
import static com.google.android.exoplayer2.C.CONTENT_TYPE_OTHER;
import static com.google.android.exoplayer2.C.CONTENT_TYPE_SS;
import static com.google.android.exoplayer2.C.TIME_END_OF_SOURCE;
import static com.google.android.exoplayer2.drm.DefaultDrmSessionManager.MODE_PLAYBACK;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.Window;
import android.view.accessibility.CaptioningManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.annotation.WorkerThread;
import androidx.activity.OnBackPressedCallback;

import com.brentvatne.common.Track;
import com.brentvatne.common.VideoTrack;
import com.brentvatne.exoplayer.AudioOutput;
import com.brentvatne.react.R;
import com.brentvatne.receiver.AudioBecomingNoisyReceiver;
import com.brentvatne.receiver.BecomingNoisyListener;
import com.facebook.react.bridge.Dynamic;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.Tracks;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManagerProvider;
import com.google.android.exoplayer2.drm.DrmSessionEventListener;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManagerProvider;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.OfflineLicenseHelper;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DashUtil;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.dash.manifest.DashManifest;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.ExoTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionOverride;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.source.dash.DashUtil;
import com.google.android.exoplayer2.source.dash.manifest.DashManifest;
import com.google.android.exoplayer2.source.dash.manifest.Period;
import com.google.android.exoplayer2.source.dash.manifest.AdaptationSet;
import com.google.android.exoplayer2.source.dash.manifest.Representation;

import com.google.android.exoplayer2.ext.ima.ImaAdsLoader;
import com.google.android.exoplayer2.source.ads.AdsMediaSource;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.ClippingMediaSource;
import com.npaw.youbora.lib6.YouboraLog;
import com.npaw.youbora.lib6.exoplayer2.Exoplayer2Adapter;
import com.npaw.youbora.lib6.plugin.Options;
import com.npaw.youbora.lib6.plugin.Plugin;

import java.io.IOException;

import com.google.common.collect.ImmutableList;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.Map;
import java.lang.Thread;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.lang.Integer;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

@SuppressLint("ViewConstructor")
class ReactExoplayerView extends FrameLayout implements
        LifecycleEventListener,
        Player.Listener,
        BandwidthMeter.EventListener,
        BecomingNoisyListener,
        DrmSessionEventListener,
        AdEvent.AdEventListener {

    public static final double DEFAULT_MAX_HEAP_ALLOCATION_PERCENT = 1;
    public static final double DEFAULT_MIN_BACK_BUFFER_MEMORY_RESERVE = 0;
    public static final double DEFAULT_MIN_BUFFER_MEMORY_RESERVE = 0;

    private static final String TAG = "ReactExoplayerView";

    private static final CookieManager DEFAULT_COOKIE_MANAGER;
    private static final int SHOW_PROGRESS = 1;

    static {
        DEFAULT_COOKIE_MANAGER = new CookieManager();
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    private final VideoEventEmitter eventEmitter;
    private final ReactExoplayerConfig config;
    private final DefaultBandwidthMeter bandwidthMeter;
    private PlayerControlView playerControlView;
    private View playPauseControlContainer;
    private Player.Listener eventListener;

    private HttpDataSource.Factory httpDataSourceFactory;
    private ExoPlayerView exoPlayerView;
    private FullScreenPlayerView fullScreenPlayerView;
    private ImaAdsLoader adsLoader;

    private DataSource.Factory mediaDataSourceFactory;
    private ExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private boolean playerNeedsSource;
    private boolean playerNeedsNewLicence;

    private int resumeWindow;
    private long resumePosition;
    private boolean loadVideoStarted;
    private boolean isFullscreen;
    private boolean isInBackground;
    private boolean isPaused;
    private boolean isBuffering;
    private boolean muted = false;
    private boolean hasAudioFocus = false;
    private float rate = 1f;
    private AudioOutput audioOutput = AudioOutput.SPEAKER;
    private float audioVolume = 1f;
    private int minLoadRetryCount = 3;
    private int maxBitRate = 0;
    private long seekTime = C.TIME_UNSET;
    private boolean hasDrmFailed = false;
    private boolean isUsingContentResolution = false;
    private boolean selectTrackWhenReady = false;

    private int minBufferMs = DefaultLoadControl.DEFAULT_MIN_BUFFER_MS;
    private int maxBufferMs = DefaultLoadControl.DEFAULT_MAX_BUFFER_MS;
    private int bufferForPlaybackMs = DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS;
    private int bufferForPlaybackAfterRebufferMs = DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS;
    private double maxHeapAllocationPercent = ReactExoplayerView.DEFAULT_MAX_HEAP_ALLOCATION_PERCENT;
    private double minBackBufferMemoryReservePercent = ReactExoplayerView.DEFAULT_MIN_BACK_BUFFER_MEMORY_RESERVE;
    private double minBufferMemoryReservePercent = ReactExoplayerView.DEFAULT_MIN_BUFFER_MEMORY_RESERVE;
    private Handler mainHandler;

    // Props from React
    private int backBufferDurationMs = DefaultLoadControl.DEFAULT_BACK_BUFFER_DURATION_MS;
    private Uri srcUri;
    private long startTimeMs = -1;
    private long endTimeMs = -1;
    private long defaultCurrentTimeMs = -1;
    private String extension;
    private boolean repeat;
    private String audioTrackType;
    private Dynamic audioTrackValue;
    private String videoTrackType;
    private Dynamic videoTrackValue;
    private String textTrackType;
    private Dynamic textTrackValue;
    private ReadableArray textTracks;
    private boolean disableFocus;
    private boolean focusable = true;
    private boolean disableBuffering;
    private long contentStartTime = -1L;
    private boolean disableDisconnectError;
    private boolean preventsDisplaySleepDuringVideoPlayback = true;
    private float mProgressUpdateInterval = 250.0f;
    private boolean playInBackground = false;
    private Map<String, String> requestHeaders;
    private boolean mReportBandwidth = false;
    private UUID drmUUID = null;
    private String drmLicenseUrl = null;
    private String[] drmLicenseHeader = null;
    private String assetId = null;
    private boolean licencePersistingEnabled = false;
    private boolean licenceMultiSessionEnabled = false;
    private boolean controls;
    private ReadableMap analyticsMeta;
    private Uri adTagUrl;
    // \ End props

    // React
    private final ThemedReactContext themedReactContext;
    private final AudioManager audioManager;
    private final AudioBecomingNoisyReceiver audioBecomingNoisyReceiver;
    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;
    private Plugin youboraPlugin;
    private String contentId;

    // store last progress event values to avoid sending unnecessary messages
    private long lastPos = -1;
    private long lastBufferDuration = -1;
    private long lastDuration = -1;

    private final Handler progressHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_PROGRESS:
                    if (player != null) {
                        if (playerControlView != null && isPlayingAd() && controls) {
                            playerControlView.hide();
                        }
                        long pos = player.getCurrentPosition();
                        long bufferedDuration = player.getBufferedPercentage() * player.getDuration() / 100;
                        long duration = player.getDuration();

                        if (lastPos != pos
                                || lastBufferDuration != bufferedDuration
                                || lastDuration != duration) {
                            lastPos = pos;
                            lastBufferDuration = bufferedDuration;
                            lastDuration = duration;
                            eventEmitter.progressChanged(pos, bufferedDuration, player.getDuration(), getPositionInFirstPeriodMsForCurrentWindow(pos));
                        }
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, Math.round(mProgressUpdateInterval));
                    }
                    break;
            }
        }
    };

    public double getPositionInFirstPeriodMsForCurrentWindow(long currentPosition) {
        Timeline.Window window = new Timeline.Window();
        if(!player.getCurrentTimeline().isEmpty()) {
            player.getCurrentTimeline().getWindow(player.getCurrentMediaItemIndex(), window);
        }
        return window.windowStartTimeMs + currentPosition;
    }

    public ReactExoplayerView(ThemedReactContext context, ReactExoplayerConfig config) {
        super(context);
        this.themedReactContext = context;
        this.eventEmitter = new VideoEventEmitter(context);
        this.config = config;
        this.bandwidthMeter = config.getBandwidthMeter();

        createViews();

        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        themedReactContext.addLifecycleEventListener(this);
        audioBecomingNoisyReceiver = new AudioBecomingNoisyReceiver(themedReactContext);
        audioFocusChangeListener = new OnAudioFocusChangedListener(this);
    }

    private boolean isPlayingAd() {
        return player != null && player.isPlayingAd();
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        eventEmitter.setViewId(id);
    }

    private void createViews() {
        clearResumePosition();
        LicencesDataStore.init(getContext());
        mediaDataSourceFactory = buildDataSourceFactory(true);
        httpDataSourceFactory = buildHttpDataSourceFactory(false);
        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
        }

        LayoutParams layoutParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        exoPlayerView = new ExoPlayerView(getContext());
        exoPlayerView.setLayoutParams(layoutParams);

        addView(exoPlayerView, 0, layoutParams);

        exoPlayerView.setFocusable(this.focusable);

        mainHandler = new Handler();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        initializePlayer();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        /* We want to be able to continue playing audio when switching tabs.
         * Leave this here in case it causes issues.
         */
        // stopPlayback();
    }

    // LifecycleEventListener implementation

    @Override
    public void onHostResume() {
        if (!playInBackground || !isInBackground) {
            setPlayWhenReady(!isPaused);
        }
        isInBackground = false;
    }

    @Override
    public void onHostPause() {
        isInBackground = true;
        if (playInBackground) {
            return;
        }
        setPlayWhenReady(false);
    }

    @Override
    public void onHostDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        stopPlayback();
    }

    public void cleanUpResources() {
        mainHandler.removeCallbacksAndMessages(null);
        stopPlayback();
    }

    //BandwidthMeter.EventListener implementation
    @Override
    public void onBandwidthSample(int elapsedMs, long bytes, long bitrate) {
        if (mReportBandwidth) {
            if (player == null) {
                eventEmitter.bandwidthReport(bitrate, 0, 0, "-1");
            } else {
                Format videoFormat = player.getVideoFormat();
                int width = videoFormat != null ? videoFormat.width : 0;
                int height = videoFormat != null ? videoFormat.height : 0;
                String trackId = videoFormat != null ? videoFormat.id : "-1";
                eventEmitter.bandwidthReport(bitrate, height, width, trackId);
            }
        }
    }

    // Internal methods

    /**
     * Toggling the visibility of the player control view
     */
    private void togglePlayerControlVisibility() {
        if(player == null) return;
        reLayout(playerControlView);
        if (playerControlView.isVisible()) {
            playerControlView.hide();
        } else {
            playerControlView.show();
        }
    }

    /**
     * Initializing Player control
     */
    private void initializePlayerControl() {
        if (playerControlView == null) {
            playerControlView = new PlayerControlView(getContext());
        }

        if (fullScreenPlayerView == null) {
            fullScreenPlayerView = new FullScreenPlayerView(getContext(), exoPlayerView, playerControlView, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    setFullscreen(false);
                }
            });
        }

        // Setting the player for the playerControlView
        playerControlView.setPlayer(player);
        playPauseControlContainer = playerControlView.findViewById(R.id.exo_play_pause_container);

        // Invoking onClick event for exoplayerView
        exoPlayerView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPlayingAd()) {
                    togglePlayerControlVisibility();
                }
            }
        });

        //Handling the playButton click event
        ImageButton playButton = playerControlView.findViewById(R.id.exo_play);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player != null && player.getPlaybackState() == Player.STATE_ENDED) {
                    player.seekTo(0);
                }
                setPausedModifier(false);
            }
        });

        //Handling the pauseButton click event
        ImageButton pauseButton = playerControlView.findViewById(R.id.exo_pause);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPausedModifier(true);
            }
        });

        //Handling the fullScreenButton click event
        final ImageButton fullScreenButton = playerControlView.findViewById(R.id.exo_fullscreen);
        fullScreenButton.setOnClickListener(v -> setFullscreen(!isFullscreen));
        updateFullScreenButtonVisbility();

        // Invoking onPlaybackStateChanged and onPlayWhenReadyChanged events for Player
        eventListener = new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                View playButton = playerControlView.findViewById(R.id.exo_play);
                View pauseButton = playerControlView.findViewById(R.id.exo_pause);
                if (playButton != null && playButton.getVisibility() == GONE) {
                    playButton.setVisibility(INVISIBLE);
                }
                if (pauseButton != null && pauseButton.getVisibility() == GONE) {
                    pauseButton.setVisibility(INVISIBLE);
                }
                reLayout(playPauseControlContainer);
                //Remove this eventListener once its executed. since UI will work fine once after the reLayout is done
                player.removeListener(eventListener);
            }

            @Override
            public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
                reLayout(playPauseControlContainer);
                //Remove this eventListener once its executed. since UI will work fine once after the reLayout is done
                player.removeListener(eventListener);
            }
        };
        player.addListener(eventListener);
    }

    /**
     * Adding Player control to the frame layout
     */
    private void addPlayerControl() {
        if(playerControlView == null) return;
        LayoutParams layoutParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        playerControlView.setLayoutParams(layoutParams);
        int indexOfPC = indexOfChild(playerControlView);
        if (indexOfPC != -1) {
            removeViewAt(indexOfPC);
        }
        addView(playerControlView, 1, layoutParams);
        reLayout(playerControlView);
    }

    /**
     * Update the layout
     * @param view  view needs to update layout
     *
     * This is a workaround for the open bug in react-native: https://github.com/facebook/react-native/issues/17968
     */
    private void reLayout(View view) {
        if (view == null) return;
        view.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
        view.layout(view.getLeft(), view.getTop(), view.getMeasuredWidth(), view.getMeasuredHeight());
    }

    private void initialiseYoubora() {
        if (analyticsMeta == null) return;

        if (BuildConfig.DEBUG) {
            YouboraLog.setDebugLevel(YouboraLog.Level.VERBOSE);
        }

        contentId = analyticsMeta.getString("contentId");

        Options youboraOptions = new Options();
        if (analyticsMeta.hasKey("accountCode")) {
            youboraOptions.setAccountCode(analyticsMeta.getString("accountCode"));
        }
        if (analyticsMeta.hasKey("enabled")) {
            youboraOptions.setEnabled(analyticsMeta.getBoolean("enabled"));
        }
        if (analyticsMeta.hasKey("username")) {
            youboraOptions.setUsername(analyticsMeta.getString("username"));
        }
        if (analyticsMeta.hasKey("contentTransactionCode")) {
            youboraOptions.setContentTransactionCode(analyticsMeta.getString("contentTransactionCode"));
        }


        if (analyticsMeta.hasKey("contentCustomDimension6")) {
            youboraOptions.setContentCustomDimension6(analyticsMeta.getString("contentCustomDimension6"));
        }
        if (analyticsMeta.hasKey("contentCustomDimension7")) {
            youboraOptions.setContentCustomDimension7(analyticsMeta.getString("contentCustomDimension7"));
        }

        if (analyticsMeta.hasKey("contentIsLive")) {
            youboraOptions.setContentIsLive(analyticsMeta.getBoolean("contentIsLive"));
        }
        if (analyticsMeta.hasKey("contentType")) {
            youboraOptions.setContentType(analyticsMeta.getString("contentType"));
        }
        if (analyticsMeta.hasKey("contentTitle")) {
            youboraOptions.setContentTitle(analyticsMeta.getString("contentTitle"));
        }
        if (analyticsMeta.hasKey("program")) {
            youboraOptions.setProgram(analyticsMeta.getString("program"));
        }
        if (analyticsMeta.hasKey("contentResource")) {
            youboraOptions.setContentResource(analyticsMeta.getString("contentResource"));
        }
        if (analyticsMeta.hasKey("contentSeason")) {
            youboraOptions.setContentSeason(analyticsMeta.getString("contentSeason"));
        }
        if (analyticsMeta.hasKey("contentEpisodeTitle")) {
            youboraOptions.setContentEpisodeTitle(analyticsMeta.getString("contentEpisodeTitle"));
        }
        if (analyticsMeta.hasKey("contentChannel")) {
            youboraOptions.setContentChannel(analyticsMeta.getString("contentChannel"));
        }
        if (analyticsMeta.hasKey("contentId")) {
            youboraOptions.setContentId(analyticsMeta.getString("contentId"));
        }
        if (analyticsMeta.hasKey("contentGenre")) {
            youboraOptions.setContentGenre(analyticsMeta.getString("contentGenre"));
        }
        if (analyticsMeta.hasKey("contentDuration")) {
            youboraOptions.setContentDuration(analyticsMeta.getDouble("contentDuration"));
        }
        if (analyticsMeta.hasKey("appName")) {
            youboraOptions.setAppName(analyticsMeta.getString("appName"));
        }
        if (analyticsMeta.hasKey("appReleaseVersion")) {
            youboraOptions.setAppReleaseVersion(analyticsMeta.getString("appReleaseVersion"));
        }
        if (analyticsMeta.hasKey("contentResource")) {
            youboraOptions.setContentResource(analyticsMeta.getString("contentResource"));
        }
        if (analyticsMeta.hasKey("contentDuration")) {
            youboraOptions.setContentDuration(analyticsMeta.getDouble("contentDuration"));
        }
        if (analyticsMeta.hasKey("contentResource")) {
            youboraOptions.setContentResource(analyticsMeta.getString("contentResource"));
        }
        if (analyticsMeta.hasKey("contentPlaybackType")) {
            youboraOptions.setContentPlaybackType(analyticsMeta.getString("contentPlaybackType"));
        }
        youboraOptions.setAutoDetectBackground(true);

        youboraPlugin = new Plugin(youboraOptions, getContext());

        if (analyticsMeta.hasKey("offline")) {
            youboraOptions.setOffline(analyticsMeta.getBoolean("offline"));

            if (!analyticsMeta.getBoolean("offline")) {
                youboraPlugin.fireOfflineEvents();
            }
        }

        Activity activity = themedReactContext.getCurrentActivity();
        if (activity == null) return;

        youboraPlugin.setActivity(activity);
        if (player != null && youboraPlugin != null && youboraPlugin.getAdapter() == null) {
            Exoplayer2Adapter adapter = new Exoplayer2Adapter(player);
            youboraPlugin.setAdapter(adapter);
            youboraPlugin.getAdapter().fireStart();
        }
    }

    private class RNVLoadControl extends DefaultLoadControl {
        private int availableHeapInBytes = 0;
        private Runtime runtime;
        public RNVLoadControl(DefaultAllocator allocator, int minBufferMs, int maxBufferMs, int bufferForPlaybackMs, int bufferForPlaybackAfterRebufferMs, int targetBufferBytes, boolean prioritizeTimeOverSizeThresholds, int backBufferDurationMs, boolean retainBackBufferFromKeyframe) {
            super(allocator,
                    minBufferMs,
                    maxBufferMs,
                    bufferForPlaybackMs,
                    bufferForPlaybackAfterRebufferMs,
                    targetBufferBytes,
                    prioritizeTimeOverSizeThresholds,
                    backBufferDurationMs,
                    retainBackBufferFromKeyframe);
            runtime = Runtime.getRuntime();
            ActivityManager activityManager = (ActivityManager) themedReactContext.getSystemService(themedReactContext.ACTIVITY_SERVICE);
            availableHeapInBytes = (int) Math.floor(activityManager.getMemoryClass() * maxHeapAllocationPercent * 1024 * 1024);
        }

        @Override
        public boolean shouldContinueLoading(long playbackPositionUs, long bufferedDurationUs, float playbackSpeed) {
            if (ReactExoplayerView.this.disableBuffering) {
                return false;
            }
            int loadedBytes = getAllocator().getTotalBytesAllocated();
            boolean isHeapReached = availableHeapInBytes > 0 && loadedBytes >= availableHeapInBytes;
            if (isHeapReached) {
                return false;
            }
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long freeMemory = runtime.maxMemory() - usedMemory;
            long reserveMemory = (long)minBufferMemoryReservePercent * runtime.maxMemory();
            long bufferedMs = bufferedDurationUs / (long)1000;
            if (reserveMemory > freeMemory && bufferedMs > 2000) {
                // We don't have enough memory in reserve so we stop buffering to allow other components to use it instead
                return false;
            }
            if (runtime.freeMemory() == 0) {
                Log.w("ExoPlayer Warning", "Free memory reached 0, forcing garbage collection");
                runtime.gc();
                return false;
            }
            return super.shouldContinueLoading(playbackPositionUs, bufferedDurationUs, playbackSpeed);
        }
    }

    private void startBufferCheckTimer() {
        Player player = this.player;
        VideoEventEmitter eventEmitter = this.eventEmitter;
        Handler mainHandler = this.mainHandler;

    }

    private void initializePlayer() {
        ReactExoplayerView self = this;
        Activity activity = themedReactContext.getCurrentActivity();
        // This ensures all props have been settled, to avoid async racing conditions.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (player == null) {
                        // Initialize core configuration and listeners
                        initializePlayerCore(self);
                    }
                    if (playerNeedsSource && srcUri != null) {
                        exoPlayerView.invalidateAspectRatio();

                        cacheLicense();

                        // DRM session manager creation must be done on a different thread to prevent crashes so we start a new thread
                        ExecutorService es = Executors.newSingleThreadExecutor();
                        es.execute(new Runnable() {
                            @Override
                            public void run() {
                                // DRM initialization must run on a different thread
                                DrmSessionManager drmSessionManager = initializePlayerDrm(self);
                                if (drmSessionManager == null && self.drmUUID != null) {
                                    // Failed to intialize DRM session manager - cannot continue
                                    Log.e("ExoPlayer Exception", "Failed to initialize DRM Session Manager Framework!");
                                    eventEmitter.error("Failed to initialize DRM Session Manager Framework!", new Exception("DRM Session Manager Framework failure!"), "3003");
                                    return;
                                }

                                if (activity == null) {
                                    Log.e("ExoPlayer Exception", "Failed to initialize Player!");
                                    eventEmitter.error("Failed to initialize Player!", new Exception("Current Activity is null!"), "1001");
                                    return;
                                }

                                // Initialize handler to run on the main thread
                                activity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        try {
                                            // Source initialization must run on the main thread
                                            initializePlayerSource(self, drmSessionManager);
                                        } catch (Exception ex) {
                                            self.playerNeedsSource = true;
                                            Log.e("ExoPlayer Exception", "Failed to initialize Player!");
                                            Log.e("ExoPlayer Exception", ex.toString());
                                            self.eventEmitter.error(ex.toString(), ex, "1001");
                                        }
                                    }
                                });
                            }
                        });
                    } else if (srcUri != null) {
                        initializePlayerSource(self, null);
                    }
                } catch (Exception ex) {
                    self.playerNeedsSource = true;
                    Log.e("ExoPlayer Exception", "Failed to initialize Player!");
                    Log.e("ExoPlayer Exception", ex.toString());
                    eventEmitter.error(ex.toString(), ex, "1001");
                }
            }
        }, 1);

    }

    private void initializePlayerCore(ReactExoplayerView self) {
        ExoTrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory();
        self.trackSelector = new DefaultTrackSelector(getContext(), videoTrackSelectionFactory);
        self.trackSelector.setParameters(trackSelector.buildUponParameters()
                .setMaxVideoBitrate(maxBitRate == 0 ? Integer.MAX_VALUE : maxBitRate));

        DefaultAllocator allocator = new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE);
        RNVLoadControl loadControl = new RNVLoadControl(
                allocator,
                minBufferMs,
                maxBufferMs,
                bufferForPlaybackMs,
                bufferForPlaybackAfterRebufferMs,
                -1,
                true,
                backBufferDurationMs,
                DefaultLoadControl.DEFAULT_RETAIN_BACK_BUFFER_FROM_KEYFRAME
        );
        DefaultRenderersFactory renderersFactory =
                new DefaultRenderersFactory(getContext())
                        .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF).setEnableDecoderFallback(true);

        // Create an AdsLoader.
        adsLoader = new ImaAdsLoader.Builder(themedReactContext).setAdEventListener(this).build();

        MediaSource.Factory mediaSourceFactory = new DefaultMediaSourceFactory(mediaDataSourceFactory)
            .setLocalAdInsertionComponents(unusedAdTagUri -> adsLoader, exoPlayerView);

        player = new ExoPlayer.Builder(getContext(), renderersFactory)
                    .setTrackSelector(self.trackSelector)
                    .setBandwidthMeter(bandwidthMeter)
                    .setLoadControl(loadControl)
                    .setSeekParameters(SeekParameters.CLOSEST_SYNC)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .build();
        player.addListener(self);
        exoPlayerView.setPlayer(player);
        if (adsLoader != null) {
            adsLoader.setPlayer(player);
        }
        audioBecomingNoisyReceiver.setListener(self);
        bandwidthMeter.addEventListener(new Handler(), self);
        setPlayWhenReady(!isPaused);
        playerNeedsSource = true;

        PlaybackParameters params = new PlaybackParameters(rate, 1f);
        player.setPlaybackParameters(params);
        if (analyticsMeta != null) {
            initialiseYoubora();
        }
        changeAudioOutput(this.audioOutput);
    }

    private DrmSessionManager initializePlayerDrm(ReactExoplayerView self) {
        DrmSessionManager drmSessionManager = null;
        if (self.drmUUID != null) {
            try {
                drmSessionManager = self.buildDrmSessionManager(self.drmUUID, self.drmLicenseUrl,
                        self.drmLicenseHeader);
            } catch (UnsupportedDrmException e) {
                int errorStringId = Util.SDK_INT < 18 ? R.string.error_drm_not_supported
                        : (e.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
                        ? R.string.error_drm_unsupported_scheme : R.string.error_drm_unknown);
                eventEmitter.error(getResources().getString(errorStringId), e, "3003");
                return null;
            }
        }
        return drmSessionManager;
    }

    private void initializePlayerSource(ReactExoplayerView self, DrmSessionManager drmSessionManager) {
        ArrayList<MediaSource> mediaSourceList = buildTextSources();
        MediaSource videoSource = buildMediaSource(self.srcUri, self.extension, drmSessionManager, startTimeMs, endTimeMs);
        MediaSource mediaSourceWithAds = null;
        if (adTagUrl != null) {
            MediaSource.Factory mediaSourceFactory = new DefaultMediaSourceFactory(mediaDataSourceFactory)
                    .setLocalAdInsertionComponents(unusedAdTagUri -> adsLoader, exoPlayerView);
            DataSpec adTagDataSpec = new DataSpec(adTagUrl);
            mediaSourceWithAds = new AdsMediaSource(videoSource, adTagDataSpec, ImmutableList.of(srcUri, adTagUrl), mediaSourceFactory, adsLoader, exoPlayerView);
        }
        MediaSource mediaSource;
        if (mediaSourceList.size() == 0) {
            if (mediaSourceWithAds != null) {
                mediaSource = mediaSourceWithAds;
            } else {
                mediaSource = videoSource;
            }
        } else {
            if (mediaSourceWithAds != null) {
                mediaSourceList.add(0, mediaSourceWithAds);
            } else {
                mediaSourceList.add(0, videoSource);
            }
            MediaSource[] textSourceArray = mediaSourceList.toArray(
                    new MediaSource[mediaSourceList.size()]
            );
            mediaSource = new MergingMediaSource(textSourceArray);
        }

        // wait for player to be set
        while (player == null) {
            try {
                wait();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                Log.e("ExoPlayer Exception", ex.toString());
            }
        }

//                    boolean haveResumePosition = resumeWindow != C.INDEX_UNSET;
//                    if (haveResumePosition) {
//                        player.seekTo(resumeWindow, resumePosition);
//                    }

        boolean haveResumePosition = defaultCurrentTimeMs > 0;
        if (haveResumePosition) {
            player.seekTo(defaultCurrentTimeMs);
        }
        player.prepare(mediaSource, !haveResumePosition, false);
        playerNeedsSource = false;

        reLayout(exoPlayerView);
        eventEmitter.loadStart();
        loadVideoStarted = true;

        finishPlayerInitialization();
    }

    private void finishPlayerInitialization() {
        // Initializing the playerControlView
        initializePlayerControl();
        setControls(controls);
        applyModifiers();
        startBufferCheckTimer();
    }

    private DrmSessionManager buildDrmSessionManager(UUID uuid, String licenseUrl, String[] keyRequestPropertiesArray) throws UnsupportedDrmException {
        return buildDrmSessionManager(uuid, licenseUrl, keyRequestPropertiesArray, 0);
    }

    private DrmSessionManager buildDrmSessionManager(UUID uuid, String licenseUrl, String[] keyRequestPropertiesArray, int retryCount) throws UnsupportedDrmException {
        if (Util.SDK_INT < 18) {
            return null;
        }
        try {
            HttpMediaDrmCallback drmCallback = new HttpMediaDrmCallback(licenseUrl,
                    buildHttpDataSourceFactory(false));
            FrameworkMediaDrm mediaDrm = FrameworkMediaDrm.newInstance(uuid);
            if (keyRequestPropertiesArray != null) {
                for (int i = 0; i < keyRequestPropertiesArray.length - 1; i += 2) {
                    drmCallback.setKeyRequestProperty(keyRequestPropertiesArray[i], keyRequestPropertiesArray[i + 1]);
                }
            }
            FrameworkMediaDrm mediaDrm = FrameworkMediaDrm.newInstance(uuid);
            if (hasDrmFailed) {
                // When DRM fails using L1 we want to switch to L3
                mediaDrm.setPropertyString("securityLevel", "L3");
            }
            DefaultDrmSessionManager drmSessionManager = new DefaultDrmSessionManager(uuid, mediaDrm, drmCallback,
                    null, this.licenceMultiSessionEnabled, 3);

            if (licencePersistingEnabled) {
                byte[] key = LicencesDataStore.getLicence(this.assetId);
                if (key != null) {
                    drmSessionManager.setMode(MODE_PLAYBACK, key);
                }
            }
            return drmSessionManager;
        } catch(UnsupportedDrmException ex) {
            // Unsupported DRM exceptions are handled by the calling method
            throw ex;
        } catch (Exception ex) {
            if (retryCount < 3) {
                // Attempt retry 3 times in case where the OS Media DRM Framework fails for whatever reason
                return buildDrmSessionManager(uuid, licenseUrl, keyRequestPropertiesArray, ++retryCount);
            }
            // Handle the unknow exception and emit to JS
            eventEmitter.error(ex.toString(), ex, "3006");
            return null;
        }
    }

    private MediaSource buildMediaSource(Uri uri, String overrideExtension, DrmSessionManager drmSessionManager, long startTimeMs, long endTimeMs) {
        if (uri == null) {
            throw new IllegalStateException("Invalid video uri");
        }
        int type = Util.inferContentType(!TextUtils.isEmpty(overrideExtension) ? "." + overrideExtension
                : uri.getLastPathSegment());
        config.setDisableDisconnectError(this.disableDisconnectError);

        MediaItem.Builder mediaItemBuilder = new MediaItem.Builder().setUri(uri);

        if (adTagUrl != null) {
            mediaItemBuilder.setAdsConfiguration(
                    new MediaItem.AdsConfiguration.Builder(adTagUrl).build()
            );
        }

        MediaItem mediaItem = mediaItemBuilder.build();
        MediaSource mediaSource = null;
        DrmSessionManagerProvider drmProvider = null;
        if (drmSessionManager != null) {
            drmProvider = new DrmSessionManagerProvider() {
                @Override
                public DrmSessionManager get(MediaItem mediaItem) {
                    return drmSessionManager;
                }
            };
        } else {
            drmProvider = new DefaultDrmSessionManagerProvider();
        }
        switch (type) {
            case CONTENT_TYPE_SS:
                mediaSource = new SsMediaSource.Factory(
                        new DefaultSsChunkSource.Factory(mediaDataSourceFactory),
                        buildDataSourceFactory(false)
                ).setDrmSessionManagerProvider(drmProvider)
                 .setLoadErrorHandlingPolicy(
                        config.buildLoadErrorHandlingPolicy(minLoadRetryCount)
                ).createMediaSource(mediaItem);
                break;
            case CONTENT_TYPE_DASH:
                mediaSource = new DashMediaSource.Factory(
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory),
                        buildDataSourceFactory(false)
                ).setDrmSessionManagerProvider(drmProvider)
                 .setLoadErrorHandlingPolicy(
                        config.buildLoadErrorHandlingPolicy(minLoadRetryCount)
                ).createMediaSource(mediaItem);
                break;
            case CONTENT_TYPE_HLS:
                mediaSource = new HlsMediaSource.Factory(
                        mediaDataSourceFactory
                ).setDrmSessionManagerProvider(drmProvider)
                 .setLoadErrorHandlingPolicy(
                        config.buildLoadErrorHandlingPolicy(minLoadRetryCount)
                ).createMediaSource(mediaItem);
                break;
            case CONTENT_TYPE_OTHER:
                mediaSource = new ProgressiveMediaSource.Factory(
                        mediaDataSourceFactory
                ).setDrmSessionManagerProvider(drmProvider)
                 .setLoadErrorHandlingPolicy(
                        config.buildLoadErrorHandlingPolicy(minLoadRetryCount)
                ).createMediaSource(mediaItem);
                break;
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }

        if (startTimeMs >= 0 && endTimeMs >= 0)
        {
            return new ClippingMediaSource(mediaSource, startTimeMs * 1000, endTimeMs * 1000);
        } else if (startTimeMs >= 0) {
            return new ClippingMediaSource(mediaSource, startTimeMs * 1000, TIME_END_OF_SOURCE);
        } else if (endTimeMs >= 0) {
            return new ClippingMediaSource(mediaSource, 0, endTimeMs * 1000);
        }

        return mediaSource;
    }

    private ArrayList<MediaSource> buildTextSources() {
        ArrayList<MediaSource> textSources = new ArrayList<>();
        if (textTracks == null) {
            return textSources;
        }

        for (int i = 0; i < textTracks.size(); ++i) {
            ReadableMap textTrack = textTracks.getMap(i);
            String language = textTrack.getString("language");
            String title = textTrack.hasKey("title")
                    ? textTrack.getString("title") : language + " " + i;
            Uri uri = Uri.parse(textTrack.getString("uri"));
            MediaSource textSource = buildTextSource(title, uri, textTrack.getString("type"),
                    language);
            if (textSource != null) {
                textSources.add(textSource);
            }
        }
        return textSources;
    }

    private MediaSource buildTextSource(String title, Uri uri, String mimeType, String language) {
        MediaItem.SubtitleConfiguration subtitleConfiguration = new MediaItem.SubtitleConfiguration.Builder(uri)
                .setMimeType(mimeType)
                .setLanguage(language)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .setRoleFlags(C.ROLE_FLAG_SUBTITLE)
                .setLabel(title)
                .build();
        return new SingleSampleMediaSource.Factory(mediaDataSourceFactory)
                .createMediaSource(subtitleConfiguration, C.TIME_UNSET);
    }

    private void releasePlayer() {
        if (player != null) {
            if (adsLoader != null) {
                adsLoader.setPlayer(null);
            }
            updateResumePosition();
            player.release();
            player.removeListener(this);
            trackSelector = null;
            player = null;
            if (youboraPlugin != null) {
                youboraPlugin.getAdapter().fireStop();
            }
        }
        if (adsLoader != null) {
            adsLoader.release();
        }
        adsLoader = null;
        progressHandler.removeMessages(SHOW_PROGRESS);
        themedReactContext.removeLifecycleEventListener(this);
        audioBecomingNoisyReceiver.removeListener();
        bandwidthMeter.removeEventListener(this);
    }

    private static class OnAudioFocusChangedListener implements AudioManager.OnAudioFocusChangeListener {
        private final ReactExoplayerView view;

        private OnAudioFocusChangedListener(ReactExoplayerView view) {
            this.view = view;
        }

        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    view.hasAudioFocus = false;
                    view.eventEmitter.audioFocusChanged(false);
                    view.pausePlayback();
                    view.audioManager.abandonAudioFocus(this);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    view.eventEmitter.audioFocusChanged(false);
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    view.hasAudioFocus = true;
                    view.eventEmitter.audioFocusChanged(true);
                    break;
                default:
                    break;
            }

            if (view.player != null) {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                    // Lower the volume
                    if (!view.muted) {
                        view.player.setVolume(view.audioVolume * 0.8f);
                    }
                } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                    // Raise it back to normal
                    if (!view.muted) {
                        view.player.setVolume(view.audioVolume * 1);
                    }
                }
            }
        }
    }

    private boolean requestAudioFocus() {
        if (disableFocus || srcUri == null || this.hasAudioFocus) {
            return true;
        }
        int result = audioManager.requestAudioFocus(audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void setPlayWhenReady(boolean playWhenReady) {
        if (player == null) {
            return;
        }

        if (playWhenReady) {
            this.hasAudioFocus = requestAudioFocus();
            if (this.hasAudioFocus) {
                player.setPlayWhenReady(true);
            }
        } else {
            // ensure playback is not ENDED, else it will trigger another ended event
            if (player.getPlaybackState() != Player.STATE_ENDED) {
                player.setPlayWhenReady(false);
            }
        }
    }

    private void startPlayback() {
        if (player != null) {
            switch (player.getPlaybackState()) {
                case Player.STATE_IDLE:
                case Player.STATE_ENDED:
                    initializePlayer();
                    break;
                case Player.STATE_BUFFERING:
                case Player.STATE_READY:
                    if (!player.getPlayWhenReady()) {
                        setPlayWhenReady(true);
                    }
                    break;
                default:
                    break;
            }
        } else {
            initializePlayer();
        }
        if (!disableFocus) {
            setKeepScreenOn(preventsDisplaySleepDuringVideoPlayback);
        }
    }

    private void pausePlayback() {
        if (player != null) {
            if (player.getPlayWhenReady()) {
                setPlayWhenReady(false);
            }
        }
        setKeepScreenOn(false);
    }

    private void stopPlayback() {
        onStopPlayback();
        releasePlayer();
    }

    private void onStopPlayback() {
        if (isFullscreen) {
            setFullscreen(false);
        }
        audioManager.abandonAudioFocus(audioFocusChangeListener);
    }

    private void updateResumePosition() {
        resumeWindow = player.getCurrentMediaItemIndex();
        resumePosition = player.isCurrentMediaItemSeekable() ? Math.max(0, player.getCurrentPosition())
                : C.TIME_UNSET;
    }

    private void clearResumePosition() {
        resumeWindow = C.INDEX_UNSET;
        resumePosition = C.TIME_UNSET;
    }

    /**
     * Returns a new DataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #bandwidthMeter} as a listener to the new
     *                          DataSource factory.
     * @return A new DataSource factory.
     */
    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        return DataSourceUtil.getDefaultDataSourceFactory(this.themedReactContext,
                useBandwidthMeter ? bandwidthMeter : null, requestHeaders);
    }

    /**
     * Returns a new HttpDataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #bandwidthMeter} as a listener to the new
     *     DataSource factory.
     * @return A new HttpDataSource factory.
     */
    private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
        return DataSourceUtil.getDefaultHttpDataSourceFactory(this.themedReactContext, useBandwidthMeter ? bandwidthMeter : null, requestHeaders);
    }


    // AudioBecomingNoisyListener implementation

    @Override
    public void onAudioBecomingNoisy() {
        eventEmitter.audioBecomingNoisy();
    }

    // Player.Listener implementation

    @Override
    public void onIsLoadingChanged(boolean isLoading) {
        // Do nothing.
    }

    @Override
    public void onEvents(Player player, Player.Events events) {
        if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) || events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED)) {
            int playbackState = player.getPlaybackState();
            boolean playWhenReady = player.getPlayWhenReady();
            String text = "onStateChanged: playWhenReady=" + playWhenReady + ", playbackState=";
            eventEmitter.playbackRateChange(playWhenReady && playbackState == ExoPlayer.STATE_READY ? 1.0f : 0.0f);
            switch (playbackState) {
                case Player.STATE_IDLE:
                    text += "idle";
                    eventEmitter.idle();
                    clearProgressMessageHandler();
                    if (!player.getPlayWhenReady()) {
                        setKeepScreenOn(false);
                    }
                    break;
            case Player.STATE_BUFFERING:
                text += "buffering";
                onBuffering(true);
                clearProgressMessageHandler();
                setKeepScreenOn(preventsDisplaySleepDuringVideoPlayback);
                break;
            case Player.STATE_READY:
                text += "ready";
                eventEmitter.ready();
                onBuffering(false);
                clearProgressMessageHandler(); // ensure there is no other message
                startProgressHandler();
                videoLoaded();
                if (selectTrackWhenReady && isUsingContentResolution) {
                    selectTrackWhenReady = false;
                    setSelectedTrack(C.TRACK_TYPE_VIDEO, videoTrackType, videoTrackValue);
                }
                // Setting the visibility for the playerControlView
                if (playerControlView != null) {
                    playerControlView.show();
                }
                setKeepScreenOn(preventsDisplaySleepDuringVideoPlayback);
                break;
            case Player.STATE_ENDED:
                text += "ended";
                eventEmitter.end();
                onStopPlayback();
                setKeepScreenOn(false);
                break;
            default:
                text += "unknown";
                break;
            }
        }
    }

    private void startProgressHandler() {
        progressHandler.sendEmptyMessage(SHOW_PROGRESS);
    }

    /*
        The progress message handler will duplicate recursions of the onProgressMessage handler
        on change of player state from any state to STATE_READY with playWhenReady is true (when
        the video is not paused). This clears all existing messages.
     */
    private void clearProgressMessageHandler() {
         progressHandler.removeMessages(SHOW_PROGRESS);
    }

    private void videoLoaded() {
        if (!player.isPlayingAd() && loadVideoStarted) {
            loadVideoStarted = false;
            if (audioTrackType != null) {
                setSelectedAudioTrack(audioTrackType, audioTrackValue);
            }
            if (videoTrackType != null) {
                setSelectedVideoTrack(videoTrackType, videoTrackValue);
            }
            if (textTrackType != null) {
                setSelectedTextTrack(textTrackType, textTrackValue);
            }
            Format videoFormat = player.getVideoFormat();
            int width = videoFormat != null ? videoFormat.width : 0;
            int height = videoFormat != null ? videoFormat.height : 0;
            String trackId = videoFormat != null ? videoFormat.id : "-1";

            // Properties that must be accessed on the main thread
            long duration = player.getDuration();
            long currentPosition = player.getCurrentPosition();
            ArrayList<Track> audioTracks = getAudioTrackInfo();
            ArrayList<Track> textTracks  = getTextTrackInfo();

            if (this.contentStartTime != -1L) {
                ExecutorService es = Executors.newSingleThreadExecutor();
                es.execute(new Runnable() {
                    @Override
                    public void run() {
                        // To prevent ANRs caused by getVideoTrackInfo we run this on a different thread and notify the player only when we're done
                        ArrayList<VideoTrack> videoTracks = getVideoTrackInfoFromManifest();
                        if (videoTracks != null) {
                            isUsingContentResolution = true;
                        }
                        eventEmitter.load(duration, currentPosition, width, height,
                                audioTracks, textTracks, videoTracks, trackId );

                    }
                });
                return;
            }

            ArrayList<VideoTrack> videoTracks = getVideoTrackInfo();

            eventEmitter.load(duration, currentPosition, width, height,
                    audioTracks, textTracks, videoTracks, trackId);
        }
    }

    private static boolean isTrackSelected(TrackSelection selection, TrackGroup group,
                                           int trackIndex){
        return selection != null && selection.getTrackGroup() == group
                && selection.indexOf( trackIndex ) != C.INDEX_UNSET;
    }

    private ArrayList<Track> getAudioTrackInfo() {
        ArrayList<Track> audioTracks = new ArrayList<>();
        if (trackSelector == null) {
            // Likely player is unmounting so no audio tracks are available anymore
            return audioTracks;
        }

        MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
        int index = getTrackRendererIndex(C.TRACK_TYPE_AUDIO);
        if (info == null || index == C.INDEX_UNSET) {
            return audioTracks;
        }
        TrackGroupArray groups = info.getTrackGroups(index);
        TrackSelectionArray selectionArray = player.getCurrentTrackSelections();
        TrackSelection selection = selectionArray.get( C.TRACK_TYPE_AUDIO );

        for (int i = 0; i < groups.length; ++i) {
            TrackGroup group = groups.get(i);
            Format format = group.getFormat(0);
            Track audioTrack = new Track();
            audioTrack.m_index = i;
            audioTrack.m_title = format.id != null ? format.id : "";
            audioTrack.m_mimeType = format.sampleMimeType;
            audioTrack.m_language = format.language != null ? format.language : "";
            audioTrack.m_bitrate = format.bitrate == Format.NO_VALUE ? 0 : format.bitrate;
            audioTrack.m_isSelected = isTrackSelected(selection, group, 0 );
            audioTracks.add(audioTrack);
        }
        return audioTracks;
    }

    private ArrayList<VideoTrack> getVideoTrackInfo() {
        ArrayList<VideoTrack> videoTracks = new ArrayList<>();
        if (trackSelector == null) {
            // Likely player is unmounting so no audio tracks are available anymore
            return videoTracks;
        }
        MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
        int index = getTrackRendererIndex(C.TRACK_TYPE_VIDEO);
        if (info == null || index == C.INDEX_UNSET) {
            return videoTracks;
        }

        TrackGroupArray groups = info.getTrackGroups(index);
        for (int i = 0; i < groups.length; ++i) {
            TrackGroup group = groups.get(i);

            for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                Format format = group.getFormat(trackIndex);
                if (isFormatSupported(format)) {
                    VideoTrack videoTrack = new VideoTrack();
                    videoTrack.m_width = format.width == Format.NO_VALUE ? 0 : format.width;
                    videoTrack.m_height = format.height == Format.NO_VALUE ? 0 : format.height;
                    videoTrack.m_bitrate = format.bitrate == Format.NO_VALUE ? 0 : format.bitrate;
                    videoTrack.m_codecs = format.codecs != null ? format.codecs : "";
                    videoTrack.m_trackId = format.id == null ? String.valueOf(trackIndex) : format.id;
                    videoTracks.add(videoTrack);
                }
            }
        }
        return videoTracks;
    }

    private ArrayList<VideoTrack> getVideoTrackInfoFromManifest() {
        return this.getVideoTrackInfoFromManifest(0);
    }

    // We need retry count to in case where minefest request fails from poor network conditions
    @WorkerThread
    private ArrayList<VideoTrack> getVideoTrackInfoFromManifest(int retryCount) {
        ExecutorService es = Executors.newSingleThreadExecutor();
        final DataSource dataSource = this.mediaDataSourceFactory.createDataSource();
        final Uri sourceUri = this.srcUri;
        final long startTime = this.contentStartTime * 1000 - 100; // s -> ms with 100ms offset

        Future<ArrayList<VideoTrack>> result = es.submit(new Callable<ArrayList<VideoTrack>>() {
            DataSource ds = dataSource;
            Uri uri = sourceUri;
            long startTimeUs = startTime * 1000; // ms -> us

            public ArrayList<VideoTrack> call() throws Exception {
                ArrayList<VideoTrack> videoTracks = new ArrayList<>();
                try  {
                    DashManifest manifest = DashUtil.loadManifest(this.ds, this.uri);
                    int periodCount = manifest.getPeriodCount();
                    for (int i = 0; i < periodCount; i++) {
                        Period period = manifest.getPeriod(i);
                        for (int adaptationIndex = 0; adaptationIndex < period.adaptationSets.size(); adaptationIndex++) {
                            AdaptationSet adaptation = period.adaptationSets.get(adaptationIndex);
                            if (adaptation.type != C.TRACK_TYPE_VIDEO) {
                                continue;
                            }
                            boolean hasFoundContentPeriod = false;
                            for (int representationIndex = 0; representationIndex < adaptation.representations.size(); representationIndex++) {
                                Representation representation = adaptation.representations.get(representationIndex);
                                Format format = representation.format;
                                if (isFormatSupported(format)) {
                                    if (representation.presentationTimeOffsetUs <= startTimeUs) {
                                        break;
                                    }
                                    hasFoundContentPeriod = true;
                                    VideoTrack videoTrack = new VideoTrack();
                                    videoTrack.m_width = format.width == Format.NO_VALUE ? 0 : format.width;
                                    videoTrack.m_height = format.height == Format.NO_VALUE ? 0 : format.height;
                                    videoTrack.m_bitrate = format.bitrate == Format.NO_VALUE ? 0 : format.bitrate;
                                    videoTrack.m_codecs = format.codecs != null ? format.codecs : "";
                                    videoTrack.m_trackId = format.id == null ? String.valueOf(representationIndex) : format.id;
                                    videoTracks.add(videoTrack);
                                }
                            }
                            if (hasFoundContentPeriod) {
                                return videoTracks;
                            }
                        }
                    }
                } catch (Exception e) {}
                return null;
            }
        });

        try {
            ArrayList<VideoTrack> results = result.get(3000, TimeUnit.MILLISECONDS);
            if (results == null && retryCount < 1) {
                return this.getVideoTrackInfoFromManifest(++retryCount);
            }
            es.shutdown();
            return results;
        } catch (Exception e) {}

        return null;
    }

    private ArrayList<Track> getTextTrackInfo() {
        ArrayList<Track> textTracks = new ArrayList<>();
        if (trackSelector == null) {
            return textTracks;
        }
        MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
        int index = getTrackRendererIndex(C.TRACK_TYPE_TEXT);
        if (info == null || index == C.INDEX_UNSET) {
            return textTracks;
        }
        TrackSelectionArray selectionArray = player.getCurrentTrackSelections();
        TrackSelection selection = selectionArray.get( C.TRACK_TYPE_VIDEO );
        TrackGroupArray groups = info.getTrackGroups(index);

        for (int i = 0; i < groups.length; ++i) {
            TrackGroup group = groups.get(i);
            Format format = group.getFormat(0);

            Track textTrack = new Track();
            textTrack.m_index = i;
            textTrack.m_title = format.id != null ? format.id : "";
            textTrack.m_label = format.label != null ? format.label : "";
            textTrack.m_mimeType = format.sampleMimeType;
            textTrack.m_language = format.language != null ? format.language : "";
            textTrack.m_isSelected = isTrackSelected(selection, group, 0 );
            textTracks.add(textTrack);
        }
        return textTracks;
    }

    private void onBuffering(boolean buffering) {
        if (isBuffering == buffering) {
            return;
        }

        isBuffering = buffering;
        if (buffering) {
            eventEmitter.buffering(true);
        } else {
            eventEmitter.buffering(false);
        }
    }

    @Override
    public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
        if (playerNeedsSource) {
            // This will only occur if the user has performed a seek whilst in the error state. Update the
            // resume position so that if the user then retries, playback will resume from the position to
            // which they seeked.
            updateResumePosition();
        }
        if (isUsingContentResolution) {
            // Discontinuity events might have a different track list so we update the selected track
            setSelectedTrack(C.TRACK_TYPE_VIDEO, videoTrackType, videoTrackValue);
            selectTrackWhenReady = true;
        }
        // When repeat is turned on, reaching the end of the video will not cause a state change
        // so we need to explicitly detect it.
        if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION
                && player.getRepeatMode() == Player.REPEAT_MODE_ONE) {
            eventEmitter.end();
        }

    }

    @Override
    public void onTimelineChanged(Timeline timeline, int reason) {
        // Do nothing.
    }

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        if (playbackState == Player.STATE_READY && seekTime != C.TIME_UNSET) {
            eventEmitter.seek(player.getCurrentPosition(), seekTime);
            seekTime = C.TIME_UNSET;
            if (isUsingContentResolution) {
                // We need to update the selected track to make sure that it still matches user selection if track list has changed in this period
                setSelectedTrack(C.TRACK_TYPE_VIDEO, videoTrackType, videoTrackValue);
            }
        }
    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
        // Do nothing.
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
        // Do nothing.
    }

    @Override
    public void onTracksChanged(Tracks tracks) {
        eventEmitter.textTracks(getTextTrackInfo());
        eventEmitter.audioTracks(getAudioTrackInfo());
        eventEmitter.videoTracks(getVideoTrackInfo());
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters params) {
        eventEmitter.playbackRateChange(params.speed);
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        eventEmitter.playbackStateChanged(isPlaying);
    }

    @Override
    public void onPlayerError(PlaybackException e) {
        if (e == null) {
            return;
        }
        String errorString = "ExoPlaybackException: " + PlaybackException.getErrorCodeName(e.errorCode);
        String errorCode = "2" + String.valueOf(e.errorCode);
        boolean needsReInitialization = false;
        switch(e.errorCode) {
            case PlaybackException.ERROR_CODE_DRM_DEVICE_REVOKED:
            case PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED:
            case PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED:
            case PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR:
            case PlaybackException.ERROR_CODE_DRM_UNSPECIFIED:
                if (!hasDrmFailed) {
                    // When DRM fails to reach the app level certificate server it will fail with a source error so we assume that it is DRM related and try one more time
                    hasDrmFailed = true;
                    playerNeedsSource = true;
                    updateResumePosition();
                    initializePlayer();
                    setPlayWhenReady(true);
                    return;
                }
                break;
            default:
                break;
        }
        eventEmitter.error(errorString, e, errorCode);
        playerNeedsSource = true;
        playerNeedsNewLicence = true;
        if (isBehindLiveWindow(e)) {
            clearResumePosition();
            initializePlayer();
        } else {
            updateResumePosition();
            if (needsReInitialization) {
                initializePlayer();
            }
        }
    }

    private static boolean isBehindLiveWindow(PlaybackException e) {
        return e.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW;
    }

    public int getTrackRendererIndex(int trackType) {
        if (player != null) {
            int rendererCount = player.getRendererCount();
            for (int rendererIndex = 0; rendererIndex < rendererCount; rendererIndex++) {
                if (player.getRendererType(rendererIndex) == trackType) {
                    return rendererIndex;
                }
            }
        }
        return C.INDEX_UNSET;
    }

    @Override
    public void onMetadata(Metadata metadata) {
        eventEmitter.timedMetadata(metadata);
    }

    // ReactExoplayerViewManager public api

    public void setSrc(final Uri uri, final long startTimeMs, final long endTimeMs, final String extension, Map<String, String> headers, final long defaultCurrentTimeMs) {
        if (uri != null) {
            boolean isSourceEqual = uri.equals(srcUri) && startTimeMs == this.startTimeMs && endTimeMs == this.endTimeMs;

            if (srcUri == null && player != null) {
                exoPlayerView.updateSurfaceView();
                player.play();
            }

            hasDrmFailed = false;
            this.srcUri = uri;
            this.startTimeMs = startTimeMs;
            this.endTimeMs = endTimeMs;
            this.defaultCurrentTimeMs = defaultCurrentTimeMs;
            this.extension = extension;
            this.requestHeaders = headers;
            this.mediaDataSourceFactory =
                    DataSourceUtil.getDefaultDataSourceFactory(this.themedReactContext, bandwidthMeter,
                            this.requestHeaders);

            if (!isSourceEqual) {
                analyticsMeta = null;
                if (youboraPlugin != null) {
                    youboraPlugin.getAdapter().unregisterListeners();
                    youboraPlugin.getAdapter().fireStop();
                    youboraPlugin = null;
                }
                reloadSource();
            }
        }
    }

    public void clearSrc() {
        if (srcUri != null) {
            exoPlayerView.clearVideoView();
            if (player != null) {
                player.stop(true);
            }
            player.clearMediaItems();
            this.srcUri = null;
            this.startTimeMs = -1;
            this.endTimeMs = -1;
            this.extension = null;
            this.requestHeaders = null;
            this.mediaDataSourceFactory = null;
            clearResumePosition();
        }
    }

    public void setProgressUpdateInterval(final float progressUpdateInterval) {
        mProgressUpdateInterval = progressUpdateInterval;
        progressHandler.removeCallbacksAndMessages(null);
        Message msg = Message.obtain(progressHandler, SHOW_PROGRESS);
        progressHandler.sendMessageDelayed(msg, Math.round(mProgressUpdateInterval));
    }

    public void setReportBandwidth(boolean reportBandwidth) {
        mReportBandwidth = reportBandwidth;
    }

    public void setAdTagUrl(final Uri uri) {
        adTagUrl = uri;
    }

    public void setRawSrc(final Uri uri, final String extension) {
        if (uri != null) {
            boolean isSourceEqual = uri.equals(srcUri);
            this.srcUri = uri;
            this.extension = extension;
            this.mediaDataSourceFactory = buildDataSourceFactory(true);

            if (!isSourceEqual) {
                reloadSource();
            }
        }
    }

    public void setTextTracks(ReadableArray textTracks) {
        this.textTracks = textTracks;
        reloadSource();
    }

    private void reloadSource() {
        playerNeedsSource = true;
        initializePlayer();
    }

    public void setResizeModeModifier(@ResizeMode.Mode int resizeMode) {
        exoPlayerView.setResizeMode(resizeMode);
    }

    private void applyModifiers() {
        setRepeatModifier(repeat);
        setMutedModifier(muted);
    }

    public void setRepeatModifier(boolean repeat) {
        if (player != null) {
            if (repeat) {
                player.setRepeatMode(Player.REPEAT_MODE_ONE);
            } else {
                player.setRepeatMode(Player.REPEAT_MODE_OFF);
            }
        }
        this.repeat = repeat;
    }

    public void setPreventsDisplaySleepDuringVideoPlayback(boolean preventsDisplaySleepDuringVideoPlayback) {
        this.preventsDisplaySleepDuringVideoPlayback = preventsDisplaySleepDuringVideoPlayback;
    }

    public void disableTrack(int rendererIndex) {
        DefaultTrackSelector.Parameters disableParameters = trackSelector.getParameters()
                .buildUpon()
                .setRendererDisabled(rendererIndex, true)
                .build();
        trackSelector.setParameters(disableParameters);
    }

    public void setSelectedTrack(int trackType, String type, Dynamic value) {
        if (player == null) return;
        int rendererIndex = getTrackRendererIndex(trackType);
        if (rendererIndex == C.INDEX_UNSET) {
            return;
        }
        MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
        if (info == null) {
            return;
        }

        TrackGroupArray groups = info.getTrackGroups(rendererIndex);
        int groupIndex = C.INDEX_UNSET;
        List<Integer> tracks = new ArrayList<>();
        tracks.add(0);

        if (TextUtils.isEmpty(type)) {
            type = "default";
        }

        if (type.equals("disabled")) {
            disableTrack(rendererIndex);
            return;
        } else if (type.equals("language")) {
            for (int i = 0; i < groups.length; ++i) {
                Format format = groups.get(i).getFormat(0);
                if (format.language != null && format.language.equals(value.asString())) {
                    groupIndex = i;
                    break;
                }
            }
        } else if (type.equals("title")) {
            for (int i = 0; i < groups.length; ++i) {
                Format format = groups.get(i).getFormat(0);
                if (format.id != null && format.id.equals(value.asString())) {
                    groupIndex = i;
                    break;
                }
            }
        } else if (type.equals("index")) {
            if (value.asInt() < groups.length) {
                groupIndex = value.asInt();
            }
        } else if (type.equals("resolution")) {
            int height = value.asInt();
            for (int i = 0; i < groups.length; ++i) { // Search for the exact height
                TrackGroup group = groups.get(i);
                Format closestFormat = null;
                int closestTrackIndex = -1;
                boolean usingExactMatch = false;
                for (int j = 0; j < group.length; j++) {
                    Format format = group.getFormat(j);
                    if (format.height == height) {
                        groupIndex = i;
                        tracks.set(0, j);
                        closestFormat = null;
                        closestTrackIndex = -1;
                        usingExactMatch = true;
                        break;
                    } else if (isUsingContentResolution) {
                        // When using content resolution rather than ads, we need to try and find the closest match if there is no exact match
                        if (closestFormat != null) {
                            if ((format.bitrate > closestFormat.bitrate || format.height > closestFormat.height) && format.height < height) {
                                // Higher quality match
                                closestFormat = format;
                                closestTrackIndex = j;
                            }
                        } else if(format.height < height) {
                            closestFormat = format;
                            closestTrackIndex = j;
                        }
                    }
                }
                // This is a fallback if the new period contains only higher resolutions than the user has selected
                if (closestFormat == null && isUsingContentResolution && !usingExactMatch) {
                    // No close match found - so we pick the lowest quality
                    int minHeight = Integer.MAX_VALUE;
                    for (int j = 0; j < group.length; j++) {
                        Format format = group.getFormat(j);
                        if (format.height < minHeight) {
                            minHeight = format.height;
                            groupIndex = i;
                            tracks.set(0, j);
                        }
                    }
                }
                // Selecting the closest match found
                if (closestFormat != null && closestTrackIndex != -1) {
                    // We found the closest match instead of an exact one
                    groupIndex = i;
                    tracks.set(0, closestTrackIndex);
                }
            }
        } else if (trackType == C.TRACK_TYPE_TEXT && Util.SDK_INT > 18) { // Text default
            // Use system settings if possible
            CaptioningManager captioningManager
                    = (CaptioningManager)themedReactContext.getSystemService(Context.CAPTIONING_SERVICE);
            if (captioningManager != null && captioningManager.isEnabled()) {
                groupIndex = getGroupIndexForDefaultLocale(groups);
            }
        } else if (rendererIndex == C.TRACK_TYPE_AUDIO) { // Audio default
            groupIndex = getGroupIndexForDefaultLocale(groups);
        }

        if (groupIndex == C.INDEX_UNSET && trackType == C.TRACK_TYPE_VIDEO && groups.length != 0) { // Video auto
            // Add all tracks as valid options for ABR to choose from
            TrackGroup group = groups.get(0);
            tracks = new ArrayList<Integer>(group.length);
            ArrayList<Integer> allTracks = new ArrayList<Integer>(group.length);
            groupIndex = 0;
            for (int j = 0; j < group.length; j++) {
                allTracks.add(j);
            }

            // Valiate list of all tracks and add only supported formats
            int supportedFormatLength = 0;
            ArrayList<Integer> supportedTrackList = new ArrayList<Integer>();
            for (int g = 0; g < allTracks.size(); g++) {
                Format format = group.getFormat(g);
                if (isFormatSupported(format)) {
                    supportedFormatLength++;
                }
            }
            if (allTracks.size() == 1) {
                // With only one tracks we can't remove any tracks so attempt to play it anyway
                tracks = allTracks;
            } else {
                tracks =  new ArrayList<>(supportedFormatLength + 1);
                for (int k = 0; k < allTracks.size(); k++) {
                    Format format = group.getFormat(k);
                    if (isFormatSupported(format)) {
                        tracks.add(allTracks.get(k));
                        supportedTrackList.add(allTracks.get(k));
                    }
                }
            }
        }

        if (groupIndex == C.INDEX_UNSET) {
            disableTrack(rendererIndex);
            return;
        }

        TrackSelectionOverride selectionOverride = new TrackSelectionOverride(groups.get(groupIndex), tracks);

        DefaultTrackSelector.Parameters selectionParameters = trackSelector.getParameters()
            .buildUpon()
            .setRendererDisabled(rendererIndex, false)
            .clearOverridesOfType(selectionOverride.getType())
            .addOverride(selectionOverride)
            .build();
        trackSelector.setParameters(selectionParameters);
    }

    private boolean isFormatSupported(Format format) {
        int width = format.width == Format.NO_VALUE ? 0 : format.width;
        int height = format.height == Format.NO_VALUE ? 0 : format.height;
        float frameRate = format.frameRate == Format.NO_VALUE ? 0 : format.frameRate;
        String mimeType = format.sampleMimeType;
        if (mimeType == null) {
            return true;
        }
        boolean isSupported = false;
        try {
            MediaCodecInfo codecInfo = MediaCodecUtil.getDecoderInfo(mimeType, false, false);
            isSupported = codecInfo.isVideoSizeAndRateSupportedV21(width, height, frameRate);
        } catch (Exception e) {
            // Failed to get decoder info - assume it is supported
            isSupported = true;
        }
        return isSupported;
    }

    private int getGroupIndexForDefaultLocale(TrackGroupArray groups) {
        if (groups.length == 0){
            return C.INDEX_UNSET;
        }

        int groupIndex = 0; // default if no match
        String locale2 = Locale.getDefault().getLanguage(); // 2 letter code
        String locale3 = Locale.getDefault().getISO3Language(); // 3 letter code
        for (int i = 0; i < groups.length; ++i) {
            Format format = groups.get(i).getFormat(0);
            String language = format.language;
            if (language != null && (language.equals(locale2) || language.equals(locale3))) {
                groupIndex = i;
                break;
            }
        }
        return groupIndex;
    }

    public void setSelectedVideoTrack(String type, Dynamic value) {
        videoTrackType = type;
        videoTrackValue = value;
        setSelectedTrack(C.TRACK_TYPE_VIDEO, videoTrackType, videoTrackValue);
    }

    public void setSelectedAudioTrack(String type, Dynamic value) {
        audioTrackType = type;
        audioTrackValue = value;
        setSelectedTrack(C.TRACK_TYPE_AUDIO, audioTrackType, audioTrackValue);
    }

    public void setSelectedTextTrack(String type, Dynamic value) {
        textTrackType = type;
        textTrackValue = value;
        setSelectedTrack(C.TRACK_TYPE_TEXT, textTrackType, textTrackValue);
    }

    public void setPausedModifier(boolean paused) {
        isPaused = paused;
        if (player != null) {
            if (!paused) {
                startPlayback();
            } else {
                pausePlayback();
            }
        }
    }

    public void setMutedModifier(boolean muted) {
        this.muted = muted;
        if (player != null) {
            player.setVolume(muted ? 0.f : audioVolume);
        }
    }

    private void changeAudioOutput(AudioOutput output) {
        if (player != null) {
            int usage = Util.getAudioUsageForStreamType(audioOutput.streamType);
            int contentType = Util.getAudioContentTypeForStreamType(audioOutput.streamType);
            AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(usage)
                    .setContentType(contentType)
                    .build();
            player.setAudioAttributes(audioAttributes, false);
            AudioManager audioManager = (AudioManager) themedReactContext.getSystemService(Context.AUDIO_SERVICE);
            audioManager.setMode(
                    audioOutput == AudioOutput.SPEAKER ? AudioManager.MODE_NORMAL
                            : AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setSpeakerphoneOn(audioOutput == AudioOutput.SPEAKER);
        }
    }

    public void setAudioOutput(AudioOutput output) {
        if (audioOutput != output) {
            this.audioOutput = output;
            changeAudioOutput(output);
        }
    }

    public void setVolumeModifier(float volume) {
        audioVolume = volume;
        if (player != null) {
            player.setVolume(audioVolume);
        }
    }

    public void seekTo(long positionMs) {
        if (player != null) {
            player.seekTo(positionMs);
            eventEmitter.seek(player.getCurrentPosition(), positionMs);
        }
    }

    public void setRateModifier(float newRate) {
        rate = newRate;

        if (player != null) {
            PlaybackParameters params = new PlaybackParameters(rate, 1f);
            player.setPlaybackParameters(params);
        }
    }

    public void setMaxBitRateModifier(int newMaxBitRate) {
        maxBitRate = newMaxBitRate;
        if (player != null) {
            trackSelector.setParameters(trackSelector.buildUponParameters()
                    .setMaxVideoBitrate(maxBitRate == 0 ? Integer.MAX_VALUE : maxBitRate));
        }
    }

    public void setMinLoadRetryCountModifier(int newMinLoadRetryCount) {
        minLoadRetryCount = newMinLoadRetryCount;
        releasePlayer();
        initializePlayer();
    }

    public void setPlayInBackground(boolean playInBackground) {
        this.playInBackground = playInBackground;
    }

    public void setDisableFocus(boolean disableFocus) {
        this.disableFocus = disableFocus;
    }

    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
        exoPlayerView.setFocusable(this.focusable);
    }

    public void setBackBufferDurationMs(int backBufferDurationMs) {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long freeMemory = runtime.maxMemory() - usedMemory;
        long reserveMemory = (long)minBackBufferMemoryReservePercent * runtime.maxMemory();
        if (reserveMemory > freeMemory) {
            // We don't have enough memory in reserve so we will
            Log.w("ExoPlayer Warning", "Not enough reserve memory, setting back buffer to 0ms to reduce memory pressure!");
            this.backBufferDurationMs = 0;
            return;
        }
        this.backBufferDurationMs = backBufferDurationMs;
    }

    public void setContentStartTime(int contentStartTime) {
        this.contentStartTime = (long)contentStartTime;
    }

    public void setDisableBuffering(boolean disableBuffering) {
        this.disableBuffering = disableBuffering;
    }

    private void updateFullScreenButtonVisbility() {
        if (playerControlView != null) {
            final ImageButton fullScreenButton = playerControlView.findViewById(R.id.exo_fullscreen);
            if (controls) {
                //Handling the fullScreenButton click event
                if (isFullscreen && fullScreenPlayerView != null && !fullScreenPlayerView.isShowing()) {
                    fullScreenButton.setVisibility(GONE);
                } else {
                    fullScreenButton.setVisibility(VISIBLE);
                }
            } else {
                fullScreenButton.setVisibility(GONE);
            }
        }
    }

    public void setDisableDisconnectError(boolean disableDisconnectError) {
        this.disableDisconnectError = disableDisconnectError;
    }

    public void setFullscreen(boolean fullscreen) {
        if (fullscreen == isFullscreen) {
            return; // Avoid generating events when nothing is changing
        }
        isFullscreen = fullscreen;

        Activity activity = themedReactContext.getCurrentActivity();
        if (activity == null) {
            return;
        }

        Window window = activity.getWindow();
        View decorView = window.getDecorView();
        int uiOptions;
        if (isFullscreen) {
            if (Util.SDK_INT >= 19) { // 4.4+
                uiOptions = SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | SYSTEM_UI_FLAG_FULLSCREEN;
            } else {
                uiOptions = SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | SYSTEM_UI_FLAG_FULLSCREEN;
            }
            eventEmitter.fullscreenWillPresent();
            if (controls && fullScreenPlayerView != null) {
                fullScreenPlayerView.show();
            }
            post(() -> {
                decorView.setSystemUiVisibility(uiOptions);
                eventEmitter.fullscreenDidPresent();
            });
        } else {
            uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
            eventEmitter.fullscreenWillDismiss();
            if (controls && fullScreenPlayerView != null) {
                fullScreenPlayerView.dismiss();
                reLayout(exoPlayerView);
            }
            post(() -> {
                decorView.setSystemUiVisibility(uiOptions);
                eventEmitter.fullscreenDidDismiss();
            });
        }
        // need to be done at the end to avoid hiding fullscreen control button when fullScreenPlayerView is shown
        updateFullScreenButtonVisbility();
    }

    public void setUseTextureView(boolean useTextureView) {
        boolean finallyUseTextureView = useTextureView && this.drmUUID == null;
        exoPlayerView.setUseTextureView(finallyUseTextureView);
    }

    public void useSecureView(boolean useSecureView) {
        exoPlayerView.useSecureView(useSecureView);
    }

    public void setHideShutterView(boolean hideShutterView) {
        exoPlayerView.setHideShutterView(hideShutterView);
    }

    public void setBufferConfig(int newMinBufferMs, int newMaxBufferMs, int newBufferForPlaybackMs, int newBufferForPlaybackAfterRebufferMs, double newMaxHeapAllocationPercent, double newMinBackBufferMemoryReservePercent, double newMinBufferMemoryReservePercent) {
        minBufferMs = newMinBufferMs;
        maxBufferMs = newMaxBufferMs;
        bufferForPlaybackMs = newBufferForPlaybackMs;
        bufferForPlaybackAfterRebufferMs = newBufferForPlaybackAfterRebufferMs;
        maxHeapAllocationPercent = newMaxHeapAllocationPercent;
        minBackBufferMemoryReservePercent = newMinBackBufferMemoryReservePercent;
        minBufferMemoryReservePercent = newMinBufferMemoryReservePercent;
        releasePlayer();
        initializePlayer();
    }

    public void setDrmType(UUID drmType) {
        this.drmUUID = drmType;
    }

    public void setDrmLicenseUrl(String licenseUrl){
        this.drmLicenseUrl = licenseUrl;
    }

    public void setDrmLicenseHeader(String[] header){
        this.drmLicenseHeader = header;
    }

    public void setAnalyticsMeta(ReadableMap analyticsData) {
        this.analyticsMeta = analyticsData;
        if (player != null && analyticsData != null && youboraPlugin == null && contentId != analyticsData.getString("contentId")) {
            initialiseYoubora();
        }
        if (player != null && analyticsData != null && youboraPlugin != null && youboraPlugin.getAdapter() != null && contentId != analyticsData.getString("contentId")) {
            youboraPlugin.getAdapter().unregisterListeners();
            youboraPlugin.getAdapter().fireStop();
            youboraPlugin = null;
            initialiseYoubora();
        }
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public void setLicencePersistingEnabled(boolean isEnabled) {
        this.licencePersistingEnabled = isEnabled;
    }

    public void setLicenceMultiSessionEnabled(boolean isEnabled) {
        this.licenceMultiSessionEnabled = isEnabled;
    }

    @Override
    public void onDrmKeysLoaded(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        Log.d("DRM Info", "onDrmKeysLoaded");
    }

    @Override
    public void onDrmSessionManagerError(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, Exception e) {
        Log.d("DRM Info", "onDrmSessionManagerError");
        eventEmitter.error("onDrmSessionManagerError", e, "3002");
    }

    @Override
    public void onDrmKeysRestored(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        Log.d("DRM Info", "onDrmKeysRestored");
    }

    @Override
    public void onDrmKeysRemoved(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        Log.d("DRM Info", "onDrmKeysRemoved");
    }

    /**
     * Handling controls prop
     *
     * @param controls  Controls prop, if true enable controls, if false disable them
     */
    public void setControls(boolean controls) {
        this.controls = controls;
        if (controls) {
            addPlayerControl();
            updateFullScreenButtonVisbility();
        } else {
            int indexOfPC = indexOfChild(playerControlView);
            if (indexOfPC != -1) {
                removeViewAt(indexOfPC);
            }
        }
    }

    @SuppressLint("CheckResult")
    private void cacheLicense() {
        if (licencePersistingEnabled) {
            Map<String, String> existingLicense = LicencesDataStore.getCachedLicence(assetId);
            boolean licenceServerChanged = existingLicense != null && !drmLicenseUrl.equals(existingLicense.get(LicencesDataStore.LICENCE_URL));
            if (existingLicense == null || playerNeedsNewLicence || licenceServerChanged) {
                Single.fromCallable(() -> {
                            try {
                                HttpDataSource httpDataSource = httpDataSourceFactory.createDataSource();
                                DashManifest dashManifest = DashUtil.loadManifest(httpDataSource, srcUri);
                                Format drmInitDataFormat = DashUtil.loadFormatWithDrmInitData(httpDataSource, dashManifest.getPeriod(0));
                                OfflineLicenseHelper offlineLicenseHelper = OfflineLicenseHelper
                                        .newWidevineInstance(drmLicenseUrl, true, httpDataSourceFactory, new DrmSessionEventListener.EventDispatcher());

                                if (assetId != null && !"".equals(assetId) && drmInitDataFormat != null) {
                                    byte[] key = offlineLicenseHelper.downloadLicense(drmInitDataFormat);
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                                        Pair<Long, Long> licenseDuration = offlineLicenseHelper.getLicenseDurationRemainingSec(key);
                                    }
                                    return key;
                                }
                                return null;

                            } catch (Exception e) {
                                Log.i("DRM LICENCE CACHING", "Failed to acquire offline license", e);
                            }

                            return null;
                        }).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread()).subscribe(new DisposableSingleObserver<byte[]>() {
                            @Override
                            public void onSuccess(byte[] license) {
                                LicencesDataStore.storeLicense(assetId, drmLicenseUrl, license);
                                playerNeedsNewLicence = false;
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.i("DRM LICENCE CACHING", "Failed to acquire offline license", e);
                                if (existingLicense != null) {
                                    LicencesDataStore.removeLicence(assetId);
                                }
                            }
                        });
            }
        }
    }

    public void setSubtitleStyle(SubtitleStyle style) {
        exoPlayerView.setSubtitleStyle(style);
    }

    public void fastForwardOrRewind(long incrementMs, long lastPosition) {
        mainHandler.removeCallbacksAndMessages(null);
        if (player == null || incrementMs == 1000L) {
            return;
        }

        long currentPosition = lastPosition;
        if (currentPosition < 0) {
            currentPosition = player.getCurrentPosition();
        }
        long newPosition = currentPosition + incrementMs;

        // Ensure newPosition is within valid range
        long mediaDuration = player.getDuration();
        newPosition = Math.max(0, Math.min(newPosition, mediaDuration));
        seekTo(newPosition);
        final long lastSeekPosition = newPosition;
        if (newPosition > 0 && newPosition < mediaDuration) {
            mainHandler.postDelayed(() -> fastForwardOrRewind(incrementMs, lastSeekPosition), 1000);
        }
    }

    public void setShutterColor(Integer color) {
        exoPlayerView.setShutterColor(color);
    }

    @Override
    public void onAdEvent(AdEvent adEvent) {
        eventEmitter.receiveAdEvent(adEvent.getType().name());
    }
}
