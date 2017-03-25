package org.wikimedia.commons.wikimedia.Fragments;

import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.wikimedia.commons.wikimedia.R;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import apiwrapper.commons.wikimedia.org.Enums.ContributionType;
import top.oply.opuslib.OpusRecorder;

public class AudioRegisterFragment extends Fragment {
    private static final String TAG = "AudioRecorder";
    // minimum storage space for record (512KB).
    // Need to check before starting recording and during recording to avoid
    // recording keeps going but there is no free space in sdcard.
    public static final long LOW_SPACE_THRESHOLD = 512 * 1024;

    private Toolbar toolbar;
    private Button recordButton;
    private Button cancelButton;
    private Button doneButton;
    private LinearLayout linearLayout;


    //    private static MediaPlayer mediaPlayer;
    private static String audioFilePath;
    private boolean isRecording = false;

    Handler handler;//used for animation
    private ImageView animationImageView1;
    private ImageView animationImageView2;
    private ImageView animationImageView3;
    private ImageView animationImageView4;
    private ImageView animationImageView5;
    private RelativeLayout animationContainerLayout;
    private OpusRecorder opusRecorder;

    public static AudioRegisterFragment newInstance() {
        AudioRegisterFragment fragment = new AudioRegisterFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        opusRecorder = OpusRecorder.getInstance();
        return inflater.inflate(R.layout.fragment_audio_register, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        audioFilePath =
                Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/commonsAudio_" + timeStamp + ".ogg";

        handler = new Handler(Looper.getMainLooper());//used for animation
        setupLayout();
        setupHandler();
    }

    /**
     * Get the phone storage path
     *
     * @return The phone storage path
     */
    public static String getDefaultStoragePath() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    /**
     * Check if has enough space for record
     *
     * @param recordingSdcard The recording sdcard path
     * @return true if has enough space for record
     */
    public static boolean hasEnoughSpace(String recordingSdcard) {
        boolean ret = false;
        try {
            StatFs fs = new StatFs(recordingSdcard);
            long blocks = fs.getAvailableBlocks();
            long blockSize = fs.getBlockSize();
            long spaceLeft = blocks * blockSize;
            ret = spaceLeft > LOW_SPACE_THRESHOLD ? true : false;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "hasEnoughSpace, sdcard may be unmounted:" + recordingSdcard);
        }
        return ret;
    }

    public void recordAudio() throws IOException {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Log.e(TAG, "startRecording, no external storage available");
            return;
        }
        String recordingSdcard = getDefaultStoragePath();
        // check whether have sufficient storage space, if not will notify
        // caller error message
        if (!hasEnoughSpace(recordingSdcard)) {
            Log.e(TAG, "startRecording, SD card does not have sufficient space!!");
            return;
        }
        opusRecorder.startRecording(audioFilePath);

        isRecording = true;
//        try {
//            mediaRecorder = new MediaRecorder();
//            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.WEBM);
//            mediaRecorder.setOutputFile(audioFilePath);
//            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.VORBIS);
//            mediaRecorder.prepare();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        mediaRecorder.start();

    }

    public void stopRecording() {
        if (isRecording) {
            opusRecorder.stopRecording();
            opusRecorder.release();
            isRecording = false;
        } else {
            opusRecorder.release();
            recordButton.setEnabled(true);
        }
    }

    protected boolean hasMicrophone() {
        PackageManager packageManager = getActivity().getPackageManager();
        return packageManager.hasSystemFeature(
                PackageManager.FEATURE_MICROPHONE);
    }


    private void setupLayout() {
        //animation views
        animationImageView1 = (ImageView) getActivity().findViewById(R.id.animationImageView1);
        animationImageView2 = (ImageView) getActivity().findViewById(R.id.animationImageView2);
        animationImageView3 = (ImageView) getActivity().findViewById(R.id.animationImageView3);
        animationImageView4 = (ImageView) getActivity().findViewById(R.id.animationImageView4);
        animationImageView5 = (ImageView) getActivity().findViewById(R.id.animationImageView5);
        animationContainerLayout = (RelativeLayout) getActivity().findViewById(R.id.animationContainerLayout);

        toolbar = (Toolbar) getActivity().findViewById(R.id.record_toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_clear);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .remove(AudioRegisterFragment.this)
                        .commit();
            }
        });

        recordButton = (Button) getActivity().findViewById(R.id.record_button);
        doneButton = (Button) getActivity().findViewById(R.id.record_done);
        cancelButton = (Button) getActivity().findViewById(R.id.record_cancel);
        linearLayout = (LinearLayout) getActivity().findViewById(R.id.record_layout_container);

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (hasMicrophone()) {
                    linearLayout.setVisibility(View.VISIBLE);
                    recordButton.setVisibility(View.GONE);
                    animationContainerLayout.setVisibility(View.VISIBLE);
                    try {
                        if (!isRecording)
                            recordAudio();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(getActivity(), "Device has no microphone", Toast.LENGTH_LONG).show();
                }
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                linearLayout.setVisibility(View.GONE);
                recordButton.setVisibility(View.VISIBLE);
                animationContainerLayout.setVisibility(View.GONE);

                stopRecording();

            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRecording();

                //Close this fragment
                getActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .remove(AudioRegisterFragment.this)
                        .commit();

                Fragment uploadToCommonsFragment = UploadToCommonsFragment.newInstance(audioFilePath, false, ContributionType.AUDIO);
                android.support.v4.app.FragmentTransaction transaction1 = getActivity().getSupportFragmentManager().beginTransaction();
                transaction1.replace(R.id.drawer_layout, uploadToCommonsFragment, "UploadToCommonsFragment");// give your fragment container id in first parameter
                transaction1.addToBackStack(null);  // if written, this transaction will be added to backstack
                transaction1.commit();

            }
        });
    }


    //Animate methods
    private Runnable animation = new Runnable() {
        public void run() {
            try {
                animateView();
                handler.removeCallbacks(animation);
                handler.postDelayed(this, 300);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private void animateView() {
        ViewAnimation resizeAnimation1 = new ViewAnimation(animationImageView1);
        resizeAnimation1.setDuration(300);
        animationImageView1.startAnimation(resizeAnimation1);

        ViewAnimation resizeAnimation2 = new ViewAnimation(animationImageView2);
        resizeAnimation2.setDuration(300);
        animationImageView2.startAnimation(resizeAnimation2);

        ViewAnimation resizeAnimation3 = new ViewAnimation(animationImageView3);
        resizeAnimation3.setDuration(300);
        animationImageView3.startAnimation(resizeAnimation3);

        ViewAnimation resizeAnimation4 = new ViewAnimation(animationImageView4);
        resizeAnimation4.setDuration(300);
        animationImageView4.startAnimation(resizeAnimation4);

        ViewAnimation resizeAnimation5 = new ViewAnimation(animationImageView5);
        resizeAnimation5.setDuration(300);
        animationImageView5.startAnimation(resizeAnimation5);
    }


    // Handler setup, for animation buttons
    private void setupHandler() {
        handler.removeCallbacks(animation);
        handler.postDelayed(animation, 300);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(animation);
    }


    //Class used to animate view when recording audio
    class ViewAnimation extends Animation {
        View view;
        int minHeight = 15;
        int maxHeight = 70;
        int randomHeight;

        public ViewAnimation(View view) {
            this.view = view;
            randomHeight = (int) (Math.random() * maxHeight + minHeight);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            view.getLayoutParams().height = randomHeight;
            view.requestLayout();
        }

        @Override
        public void initialize(int width, int height, int parentWidth, int parentHeight) {
            super.initialize(width, height, parentWidth, parentHeight);
        }

        @Override
        public boolean willChangeBounds() {
            return true;
        }
    }
}

