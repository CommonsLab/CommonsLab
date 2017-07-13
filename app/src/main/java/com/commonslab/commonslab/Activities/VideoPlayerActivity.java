package com.commonslab.commonslab.Activities;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import android.widget.VideoView;

import com.commonslab.commonslab.R;

import apiwrapper.commons.wikimedia.org.Models.Contribution;

public class VideoPlayerActivity extends AppCompatActivity {

    private VideoView videoView;
    private Contribution contribution;
    private String thumbnailURL;

    private String localVideoURI;
    private FrameLayout loadingScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        videoView = (VideoView) findViewById(R.id.videoView);
        loadingScreen = (FrameLayout) findViewById(R.id.progressBarContainer);

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                loadingScreen.setVisibility(View.GONE);
            }
        });

        this.getWindow().addFlags
                (WindowManager.LayoutParams.FLAG_FULLSCREEN);

        int playerLocation = -1;

        if (savedInstanceState != null) {
            if (localVideoURI == null) {
                contribution = savedInstanceState.getParcelable("Contribution");
                thumbnailURL = savedInstanceState.getString("ThumbnailURL");
            } else {
                localVideoURI = savedInstanceState.getString("VideoURI");
            }
            playerLocation = savedInstanceState.getInt("Location");
        } else {
            contribution = getIntent().getExtras().getParcelable("Contribution");
            thumbnailURL = getIntent().getExtras().getString("ThumbnailURL");
            localVideoURI = getIntent().getExtras().getString("LocalVideoURI");
        }

        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);

        if (localVideoURI == null) {
            if (contribution != null) {
                Uri uri = Uri.parse(contribution.getUrl());
                videoView.setMediaController(mediaController);
                videoView.setVideoURI(uri);

                if (playerLocation != -1)
                    videoView.seekTo(playerLocation);
                videoView.start();

            } else
                Toast.makeText(this, R.string.an_error_accourred, Toast.LENGTH_SHORT).show();
        } else {
            //Playing local video
            Uri uri = Uri.parse(localVideoURI);
            videoView.setMediaController(mediaController);
            videoView.setVideoURI(uri);
            if (playerLocation != -1)
                videoView.seekTo(playerLocation);
            videoView.start();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("Location", videoView.getCurrentPosition());
        if (localVideoURI == null) {
            outState.putParcelable("Contribution", contribution);
            outState.putString("ThumbnailURL", thumbnailURL);
        } else {
            outState.putString("VideoURI", localVideoURI);
        }
    }


    //    private void configurationSetup() {
//        RelativeLayout videoPlayer_container = (RelativeLayout) findViewById(R.id.videoPlayer_container);
//
//        Configuration newConfig = getResources().getConfiguration();
//        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
//            // set background for landscape
//        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
//            // Calculate ActionBar height
//            TypedValue typedValue = new TypedValue();
//            if (getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
//                int actionBarHeight = TypedValue.complexToDimensionPixelSize(typedValue.data, getResources().getDisplayMetrics());
//                videoView.setPadding(0, 0, 0, actionBarHeight);
//            }
//            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
//            // set background for portrait
//        }
//    }
}
