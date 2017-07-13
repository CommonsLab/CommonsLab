package com.commonslab.commonslab.AudioPlayer;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.audionowdigital.android.openplayer.Player;
import com.audionowdigital.android.openplayer.PlayerEvents;
import com.commonslab.commonslab.R;

import static com.commonslab.commonslab.Activities.MainActivity.PAUSE_OPUS;
import static com.commonslab.commonslab.Activities.MainActivity.PLAY_OPUS;
import static com.commonslab.commonslab.Activities.MainActivity.SEEKBAR_UPDATE_OPUS;
import static com.commonslab.commonslab.Activities.MainActivity.STOP_OPUS;

/**
 * Created by Valdio Veliu on 25/03/2017.
 */

public class OpusPlayer {

    Intent broadcastIntent = new Intent(STOP_OPUS);
    private Player player;
    // Playback handler for callbacks
    private Handler playbackHandler;

    private int LENGTH = 0;

    private Player.DecoderType type = Player.DecoderType.OPUS;
    private String TAG = "OPUS_MEDIA";
    private String mediaURL;
    private Context context;
    private Intent seekBarIntent = new Intent(SEEKBAR_UPDATE_OPUS);


    public OpusPlayer(Context context, int LENGTH, String mediaURL) {
        this.LENGTH = LENGTH;
        this.mediaURL = mediaURL;
        this.context = context;
        init();
    }

    private void init() {

        playbackHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case PlayerEvents.PLAYING_FAILED:
                        showToastMessage(context.getString(R.string.the_decoder_failed_to_playback_the_file));
                        context.sendBroadcast(broadcastIntent);

                        Log.d(TAG, "The decoder failed to playback the file, check logs for more details");
                        break;
                    case PlayerEvents.PLAYING_FINISHED:
                        Log.d(TAG, "The decoder finished successfully");
                        context.sendBroadcast(broadcastIntent);
                        break;
                    case PlayerEvents.READING_HEADER:
                        Log.d(TAG, "Starting to read header");
                        break;
                    case PlayerEvents.READY_TO_PLAY:
                        Log.d(TAG, "READY to play - press play :)");
                        if (player != null && player.isReadyToPlay()) {
                            player.play();
                            Intent intent = new Intent(PLAY_OPUS);
                            context.sendBroadcast(intent);
                        }
                        break;
                    case PlayerEvents.PLAY_UPDATE:
                        Log.d(TAG, "Playing:" + (msg.arg1 / 60) + ":" + (msg.arg1 % 60) + " (" + (msg.arg1) + "s)");
                        seekBarIntent.putExtra("progressPosition", String.valueOf((int) (msg.arg1 * 100 / player.getDuration())));
                        context.sendBroadcast(seekBarIntent);
                        break;
                    case PlayerEvents.TRACK_INFO:
                        Bundle data = msg.getData();
                        Log.d(TAG, "title:" + data.getString("title") + " artist:" + data.getString("artist") + " album:" + data.getString("album") +
                                " date:" + data.getString("date") + " track:" + data.getString("track"));
                        break;
                }
            }
        };
        // quick test for a quick player
        player = new Player(playbackHandler, type);
    }

    public void toggleOpusPlayer() {
        if (player.isPlaying()) {
            pauseOpusMedia();
            Intent broadcastIntent = new Intent(PAUSE_OPUS);
            context.sendBroadcast(broadcastIntent);
        } else if (player != null) {
            player.play();
            Intent broadcastIntent = new Intent(PLAY_OPUS);
            context.sendBroadcast(broadcastIntent);
        }
    }

    public void playOpusMedia() {
        Log.d(TAG, "Set source: " + mediaURL);
        new Thread(new Runnable() {
            @Override
            public void run() {
                player.setDataSource(mediaURL, LENGTH);
            }
        }).start();
    }

    public void pauseOpusMedia() {
        if (player != null)
            if (player.isPlaying())
                player.pause();
    }

    public void stopOpusMedia() {
        if (player != null)
            player.stop();
    }

    public void seekToPositionOpusMedia(int position) {
        if (player != null)
            player.setPosition(position);
    }

    private void showToastMessage(String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
