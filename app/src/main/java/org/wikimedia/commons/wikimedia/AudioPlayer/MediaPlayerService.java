package org.wikimedia.commons.wikimedia.AudioPlayer;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.NotificationManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.wikimedia.commons.wikimedia.Activities.MainActivity;
import org.wikimedia.commons.wikimedia.Fragments.AudioFragment;
import org.wikimedia.commons.wikimedia.R;
import org.wikimedia.commons.wikimedia.Utils.SharedPreferencesUtils.StorageUtil;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import apiwrapper.commons.wikimedia.org.Enums.MediaType;
import apiwrapper.commons.wikimedia.org.Models.Contribution;
import apiwrapper.commons.wikimedia.org.Models.FeedItem;

import static org.wikimedia.commons.wikimedia.AudioPlayer.MediaControlsConstants.*;

/**
 * Created by valdio on 06/12/2016.
 */

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener {


    public static final String ACTION_PLAY = "org.wikimedia.commons.wikimedia.ACTION_PLAY";
    public static final String ACTION_PAUSE = "org.wikimedia.commons.wikimedia.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "org.wikimedia.commons.wikimedia.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "org.wikimedia.commons.wikimedia.ACTION_NEXT";
    public static final String ACTION_STOP = "org.wikimedia.commons.wikimedia.ACTION_STOP";

    public static final String SEEKBAR_UPDATE = "org.wikimedia.commons.wikimedia.SEEKBAR_UPDATE";
    private Intent seekBarIntent;

    private Handler handler = new Handler();


    private MediaPlayer mediaPlayer;

    //MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;

    //AudioPlayer notification ID
    public static final int NOTIFICATION_ID = 101;

    //Used to pause/resume MediaPlayer
    private int resumePosition;

    //AudioFocus
    private AudioManager audioManager;

    // Binder given to clients
    private final IBinder iBinder = new LocalBinder();

    //List of available Audio files
    private ArrayList<Contribution> audioList;
    private int audioIndex = -1;
    private Contribution activeAudio; //an object on the currently playing audio


    //Handle incoming phone calls
    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;

    //background image metadata
    Bitmap albumArt = null;

    /**
     * Service lifecycle methods
     */
    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        seekBarIntent = new Intent(SEEKBAR_UPDATE);

        // Perform one-time setup procedures

        // Manage incoming phone calls during playback.
        // Pause MediaPlayer on incoming call,
        // Resume on hangup.
        callStateListener();
        //ACTION_AUDIO_BECOMING_NOISY -- change in audio outputs -- BroadcastReceiver
        registerBecomingNoisyReceiver();
        //Listen for new Audio to play -- BroadcastReceiver
        register_playNewAudio();

        //Listen for media controls from Activity
        registerMediaControlsReceiver();
    }

    //The system calls this method when an activity, requests the service be started
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            //Load data from SharedPreferences
            StorageUtilAudioPlayer storage = new StorageUtilAudioPlayer(getApplicationContext());
            audioList = storage.loadAudio();
            audioIndex = storage.loadAudioIndex();

            if (audioIndex != -1 && audioIndex < audioList.size()) {
                //index is in a valid range
                activeAudio = audioList.get(audioIndex);
            } else {
                stopSelf();
            }
        } catch (NullPointerException e) {
            stopSelf();
        }

        //Request audio focus
        if (requestAudioFocus() == false) {
            //Could not gain focus
            stopSelf();
        }

        if (mediaSessionManager == null) {
            try {
                initMediaSession();
                initMediaPlayer();
            } catch (RemoteException e) {
                e.printStackTrace();
                stopSelf();
            }
            buildNotification(PlaybackStatus.PLAYING);
        }

        //Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent);

        //register SeekBarUpdateReceiver
        registerReceiver(SeekBarUpdateReceiver, new IntentFilter(MainActivity.SEEKBAR_UPDATE_BROADCAST));
        setupHandler();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mediaSession.release();
        removeNotification();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
        }

        //Stop sending SeekBAr updated
        handler.removeCallbacks(SeekBarUpdateRunnable);

        removeAudioFocus();
        //Disable the PhoneStateListener
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        removeNotification();

        //unregister BroadcastReceivers
        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(playNewAudio);
        unregisterReceiver(MediaControlsReceiver);
        unregisterReceiver(SeekBarUpdateReceiver);


        StorageUtilAudioPlayer storage = new StorageUtilAudioPlayer(getApplicationContext());
        storage.clearPlayerFlag();    //Clear the player FLAG
        storage.clearCachedAudioPlaylist();   //clear cached playlist
    }

    /**
     * Service Binder
     */
    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MediaPlayerService.this;
        }
    }


    /**
     * MediaPlayer callback methods
     */
    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        //Invoked indicating buffering status of
        //a media resource being streamed over the network.
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //Invoked when playback of a media source has completed.
        stopMedia();

        removeNotification();
        //stop the service
        stopSelf();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //Invoked when there has been an error during an asynchronous operation
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        //Invoked to communicate some info
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //Invoked when the media source is ready for playback.
        playMedia();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        //Invoked indicating the completion of a seek operation.

        if (!mediaPlayer.isPlaying()) {
            playMedia();
        }
    }

    @Override
    public void onAudioFocusChange(int focusState) {
        //Invoked when the audio focus of the system is updated.
        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mediaPlayer == null) initMediaPlayer();
                else if (!mediaPlayer.isPlaying()) mediaPlayer.start();
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }


    /**
     * AudioFocus
     */
    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }

    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.abandonAudioFocus(this);
    }


    /**
     * MediaPlayer actions
     */
    private void initMediaPlayer() {
        if (mediaPlayer == null)
            mediaPlayer = new MediaPlayer();//new MediaPlayer instance

        //Set up MediaPlayer event listeners
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        //Reset so that the MediaPlayer is not pointing to another data source
        mediaPlayer.reset();


        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            // Set the data source to the mediaFile location
            mediaPlayer.setDataSource(activeAudio.getUrl());
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
        mediaPlayer.prepareAsync();
    }

    private void playMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }

        //Send a broadcast to the Activity
        Intent broadcastIntent = new Intent(MediaServiceActions.MEDIA_SERVICE_ACTION_PLAY);
        sendBroadcast(broadcastIntent);

        //Store the FLAG to notify that the service is playing
        StorageUtilAudioPlayer storage = new StorageUtilAudioPlayer(getApplicationContext());
        storage.setPlayerFlagActive();
    }

    private void stopMedia() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }

        //Send a broadcast to the Activity
        Intent broadcastIntent = new Intent(MediaServiceActions.MEDIA_SERVICE_ACTION_STOP);
        sendBroadcast(broadcastIntent);

        //Clear the player FLAG
        StorageUtilAudioPlayer storage = new StorageUtilAudioPlayer(getApplicationContext());
        storage.clearPlayerFlag();
    }

    private void pauseMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
        }


        //Send a broadcast to the Activity
        Intent broadcastIntent = new Intent(MediaServiceActions.MEDIA_SERVICE_ACTION_PAUSE);
        sendBroadcast(broadcastIntent);
    }

    private void resumeMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
        }

        //Send a broadcast to the Activity
        Intent broadcastIntent = new Intent(MediaServiceActions.MEDIA_SERVICE_ACTION_PLAY);
        sendBroadcast(broadcastIntent);
    }

    private void skipToNext() {
        if (audioIndex == audioList.size() - 1) {
            //if last in playlist
            audioIndex = 0;
            activeAudio = audioList.get(audioIndex);
        } else {
            //get next in playlist
            activeAudio = audioList.get(++audioIndex);
        }

        //Update stored index
        new StorageUtilAudioPlayer(getApplicationContext()).storeAudioIndex(audioIndex);

        stopMedia();
        //reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();


        //Send a broadcast to the Activity
        Intent broadcastIntent = new Intent(MediaServiceActions.MEDIA_SERVICE_ACTION_NEXT);
        sendBroadcast(broadcastIntent);

        //Store the FLAG to notify that the service is playing
        StorageUtilAudioPlayer storage = new StorageUtilAudioPlayer(getApplicationContext());
        storage.setPlayerFlagActive();
    }

    private void skipToPrevious() {

        if (audioIndex == 0) {
            //if first in playlist
            //set index to the last of audioList
            audioIndex = audioList.size() - 1;
            activeAudio = audioList.get(audioIndex);
        } else {
            //get previous in playlist
            activeAudio = audioList.get(--audioIndex);
        }

        //Update stored index
        new StorageUtilAudioPlayer(getApplicationContext()).storeAudioIndex(audioIndex);

        stopMedia();
        //reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();


        //Send a broadcast to the Activity
        Intent broadcastIntent = new Intent(MediaServiceActions.MEDIA_SERVICE_ACTION_PREVIOUS);
        sendBroadcast(broadcastIntent);

        //Store the FLAG to notify that the service is playing
        StorageUtilAudioPlayer storage = new StorageUtilAudioPlayer(getApplicationContext());
        storage.setPlayerFlagActive();
    }


    /**
     * ACTION_AUDIO_BECOMING_NOISY -- change in audio outputs
     */
    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            pauseMedia();
            buildNotification(PlaybackStatus.PAUSED);
        }
    };

    private void registerBecomingNoisyReceiver() {
        //register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }

    /**
     * Handle PhoneState changes
     */
    private void callStateListener() {
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null) {
                            pauseMedia();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle. Start playing.
                        if (mediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false;
                                resumeMedia();
                            }
                        }
                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }

    /**
     * MediaSession and Notification actions
     */
    private void initMediaSession() throws RemoteException {
        if (mediaSessionManager != null) return; //mediaSessionManager exists

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        // Create a new MediaSession
        mediaSession = new MediaSessionCompat(getApplicationContext(), "AudioPlayer");
        //Get MediaSessions transport controls
        transportControls = mediaSession.getController().getTransportControls();
        //set MediaSession -> ready to receive media commands
        mediaSession.setActive(true);
        //indicate that the MediaSession handles transport control commands
        // through its MediaSessionCompat.Callback.
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        //Set mediaSession's MetaData
        updateMetaData();

        // Attach Callback to receive MediaSession updates
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            // Implement callbacks
            @Override
            public void onPlay() {
                super.onPlay();
                resumeMedia();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onPause() {
                super.onPause();
                pauseMedia();
                buildNotification(PlaybackStatus.PAUSED);
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                skipToNext();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                skipToPrevious();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                //Stop the service
                stopSelf();
            }

            @Override
            public void onSeekTo(long position) {
                super.onSeekTo(position);
            }
        });
    }

    private void metadataBackgroundImageLoad() {
        albumArt = BitmapFactory.decodeResource(getResources(),
                R.drawable.bg_image); //replace with medias albumArt

        //Load Image of the day
        StorageUtil storage = new StorageUtil(getApplicationContext());
        final ArrayList<FeedItem> feedItems = storage.retrieveRSS_Feed(MediaType.PICTURE);

        if (feedItems != null) {
            new AsyncTask<Nullable, Nullable, Nullable>() {
                @Override
                protected Nullable doInBackground(Nullable... nullables) {
                    String mediaLink = feedItems.get(0).getMediaLink();
                    try {
                        URL url = new URL(mediaLink);
                        albumArt = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Nullable nullable) {
                    super.onPostExecute(nullable);

                    //When image received update the MetaData
                    updateMetaData();
                }
            }.execute();
        }
    }

    private void updateMetaData() {
        if (albumArt == null)
            metadataBackgroundImageLoad();

        // Update the current metadata
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
//                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, activeAudio.getArtist())
//                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, activeAudio.getAlbum())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, activeAudio.getTitle())
                .build());
    }

    private void buildNotification(PlaybackStatus playbackStatus) {

        /**
         * Notification actions -> playbackAction()
         *  0 -> Play
         *  1 -> Pause
         *  2 -> Next track
         *  3 -> Previous track
         */

        int notificationAction = android.R.drawable.ic_media_pause;//needs to be initialized
        PendingIntent play_pauseAction = null;

        //Build a new notification according to the current state of the MediaPlayer
        if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = android.R.drawable.ic_media_pause;
            //create the pause action
            play_pauseAction = playbackAction(1);
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = android.R.drawable.ic_media_play;
            //create the play action
            play_pauseAction = playbackAction(0);
        }

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(),
                R.drawable.notification_logo);

        // Create a new Notification
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                // Hide the timestamp
                .setShowWhen(false)
                .setOngoing(true)
                // Set the Notification style
                .setStyle(new NotificationCompat.MediaStyle()
                        // Attach our MediaSession token
                        .setMediaSession(mediaSession.getSessionToken())
                        // Show our playback controls in the compat view
                        .setShowActionsInCompactView(0, 1, 2))
                // Set the Notification color
                .setColor(getResources().getColor(R.color.colorPrimary))
                // Set the large and small icons
                .setLargeIcon(largeIcon)
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                // Set Notification content information
//                .setContentText(activeAudio.getTitle())
                .setContentTitle(activeAudio.getTitle().substring(5))
//                .setContentInfo(activeAudio.getTitle())
                // Add playback actions
                .addAction(android.R.drawable.ic_media_previous, "previous", playbackAction(3))
                .addAction(notificationAction, "pause", play_pauseAction)
                .addAction(android.R.drawable.ic_media_next, "next", playbackAction(2));

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notificationBuilder.build());
    }


    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, MediaPlayerService.class);
        switch (actionNumber) {
            case 0:
                // Play
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                // Pause
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                // Next track
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                // Previous track
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            transportControls.play();
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            transportControls.skipToNext();
        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            transportControls.skipToPrevious();
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            transportControls.stop();
        }
    }


    /**
     * Play new Audio
     */
    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.wtf("PLAY_NEW_AUDIO", "Broadcast_PLAY_NEW_AUDIO received");

            //Get the new media index form SharedPreferences
            audioIndex = new StorageUtilAudioPlayer(getApplicationContext()).loadAudioIndex();
            if (audioIndex != -1 && audioIndex < audioList.size()) {
                //index is in a valid range
                activeAudio = audioList.get(audioIndex);
            } else {
                stopSelf();
            }

            //A PLAY_NEW_AUDIO action received
            //reset mediaPlayer to play the new Audio
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            if (mediaPlayer != null)
                mediaPlayer.reset();
            initMediaPlayer();
            updateMetaData();
            buildNotification(PlaybackStatus.PLAYING);
        }
    };

    private void register_playNewAudio() {
        //Register playNewMedia receiver
        IntentFilter filter = new IntentFilter(AudioFragment.Broadcast_PLAY_NEW_AUDIO);
        registerReceiver(playNewAudio, filter);
    }


    /**
     * {@link BroadcastReceiver} to get media controls form the MainActivity
     */
    private BroadcastReceiver MediaControlsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            switch (intent.getAction()) {
                case MEDIA_CONTROLS_ACTION_PLAY:
                    resumeMedia();
                    buildNotification(PlaybackStatus.PLAYING);
                    break;
                case MEDIA_CONTROLS_ACTION_PAUSE:
                    pauseMedia();
                    buildNotification(PlaybackStatus.PAUSED);
                    break;
                case MEDIA_CONTROLS_ACTION_NEXT:
                    skipToNext();
                    updateMetaData();
                    buildNotification(PlaybackStatus.PLAYING);
                    break;
                case MEDIA_CONTROLS_ACTION_PREVIOUS:
                    skipToPrevious();
                    updateMetaData();
                    buildNotification(PlaybackStatus.PLAYING);
                    break;
                case MEDIA_CONTROLS_ACTION_STOP:
                    removeNotification();
                    //Stop the service
                    stopSelf();
                    onDestroy();
                    break;
            }
        }
    };


    private void registerMediaControlsReceiver() {
        //Register playNewMedia receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(MEDIA_CONTROLS_ACTION_PLAY);
        filter.addAction(MEDIA_CONTROLS_ACTION_PAUSE);
        filter.addAction(MEDIA_CONTROLS_ACTION_NEXT);
        filter.addAction(MEDIA_CONTROLS_ACTION_PREVIOUS);
        filter.addAction(MEDIA_CONTROLS_ACTION_STOP);
        registerReceiver(MediaControlsReceiver, filter);
    }


    /**
     * SeekBAr update
     */
    private void updateSeekBar() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            String duration = activeAudio.getDuration();
            seekBarIntent.putExtra("mediaPosition", String.valueOf(mediaPlayer.getCurrentPosition()));
            if (duration.contains("."))
                seekBarIntent.putExtra("mediaDuration", String.valueOf(
                        Integer.parseInt(duration.substring(0, duration.indexOf("."))) * 1000));
            else
                seekBarIntent.putExtra("mediaDuration", String.valueOf(Integer.parseInt(duration) * 1000));
            sendBroadcast(seekBarIntent);
        }
    }


    // Handler setup, for seek bar updated
    private void setupHandler() {
        handler.removeCallbacks(SeekBarUpdateRunnable);
        handler.postDelayed(SeekBarUpdateRunnable, 1000); // update in 1 second intervals
    }

    private Runnable SeekBarUpdateRunnable = new Runnable() {
        public void run() {
            updateSeekBar();
            //every 1 second
            handler.postDelayed(this, 1000);
        }
    };

    // BroadcastReceiver to receive SeekBAr updated from the UI
    private BroadcastReceiver SeekBarUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Update the position of the player
            int seekPos = intent.getIntExtra("SeekBarPosition", 0);
            if (mediaPlayer.isPlaying()) {
                handler.removeCallbacks(SeekBarUpdateRunnable);
                mediaPlayer.seekTo(seekPos);
                setupHandler();
            }
        }
    };

}