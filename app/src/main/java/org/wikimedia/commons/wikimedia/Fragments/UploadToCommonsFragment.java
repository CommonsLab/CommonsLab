package org.wikimedia.commons.wikimedia.Fragments;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import org.wikimedia.commons.wikimedia.Activities.ImageDetailsActivity;
import org.wikimedia.commons.wikimedia.Activities.MainActivity;
import org.wikimedia.commons.wikimedia.Activities.VideoPlayerActivity;
import org.wikimedia.commons.wikimedia.R;
import org.wikimedia.commons.wikimedia.Utils.SharedPreferencesUtils.StorageUtil;

import java.io.File;
import java.io.IOException;

import apiwrapper.commons.wikimedia.org.Commons;
import apiwrapper.commons.wikimedia.org.Enums.ContributionType;
import apiwrapper.commons.wikimedia.org.Enums.CookieStatus;
import apiwrapper.commons.wikimedia.org.Interfaces.UploadCallback;
import apiwrapper.commons.wikimedia.org.Models.Licenses;
import apiwrapper.commons.wikimedia.org.Models.User;
import apiwrapper.commons.wikimedia.org.Utils.UriToAbsolutePath;


public class UploadToCommonsFragment extends Fragment {


    private FFmpeg ffmpeg;
    private ProgressDialog progressDialog;

    enum Media {
        IMAGE, VIDEO, AUDIO
    }

    private Commons commons;
    private String contributionPath;
    private boolean loadAbsolutePath;
    Media mediaType;
    private MediaPlayer mediaPlayer;

    private ImageView coverImage;
    private Button cancelButton;
    private Button uploadButton;

    private AutoCompleteTextView uploadTitleTextView;
    private AutoCompleteTextView uploadDescriptionTextView;
    private AutoCompleteTextView uploadCommentTextView;
    private Spinner upload_license;
    ContributionType contributionType = ContributionType.IMAGE;

    public UploadToCommonsFragment() {
        // Required empty public constructor
    }

    private void setFonts() {
        Typeface type = Typeface.createFromAsset(getActivity().getAssets(), "fonts/open_sans/OpenSans-Regular.ttf");
        uploadTitleTextView.setTypeface(type);
        uploadDescriptionTextView.setTypeface(type);
        uploadCommentTextView.setTypeface(type);

    }

    public static UploadToCommonsFragment newInstance(String contributionPath, boolean loadAbsolutePath, ContributionType contributionType) {
        UploadToCommonsFragment fragment = new UploadToCommonsFragment();
        Bundle args = new Bundle();
        args.putString("contributionPath", contributionPath);
        args.putBoolean("loadAbsolutePath", loadAbsolutePath);
        args.putString("contributionType", contributionType.toString());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contributionPath = getArguments().getString("contributionPath");
        loadAbsolutePath = getArguments().getBoolean("loadAbsolutePath");
        contributionType = ContributionType.valueOf(getArguments().getString("contributionType"));
        if (contributionType == ContributionType.AUDIO)
            mediaType = Media.AUDIO;
        else if (contributionType == ContributionType.VIDEO)
            mediaType = Media.VIDEO;
        else
            mediaType = Media.IMAGE;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_upload_to_commons, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupView();
        ((MainActivity) getActivity()).updateStatusBarColor();

        if (mediaType == Media.VIDEO) {
            coverImage.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.ic_play_arrow_white_24dp));
            coverImage.setBackgroundColor(getActivity().getResources().getColor(R.color.black_opacity));
        } else if (mediaType == Media.IMAGE) {
            coverImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(getActivity())
                    .load(contributionPath)
                    .into(coverImage);
        }

        if (contributionType != ContributionType.IMAGE) {
            //video or audio needs encoding
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setTitle(null);
            loadFFMpegBinary();
        }
    }

    private void setupView() {
        uploadTitleTextView = (AutoCompleteTextView) getActivity().findViewById(R.id.upload_title);
        uploadDescriptionTextView = (AutoCompleteTextView) getActivity().findViewById(R.id.upload_description);
        uploadCommentTextView = (AutoCompleteTextView) getActivity().findViewById(R.id.upload_comment);

        upload_license = (Spinner) getActivity().findViewById(R.id.upload_license);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.Licenses, android.R.layout.simple_spinner_item);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        upload_license.setAdapter(adapter);

        coverImage = (ImageView) getActivity().findViewById(R.id.uploadImageView);
        coverImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaType == Media.AUDIO) {
                    if (mediaPlayer == null) {
                        try {
                            playAudio(contributionPath);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                    } else {
                        //start playing from the start
                        try {
                            playAudio(contributionPath);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (mediaType == Media.VIDEO) {
                    playVideo();
                } else {
                    //Image
                    Intent intent = new Intent(getActivity(), ImageDetailsActivity.class);
                    intent.putExtra("ImageURL", contributionPath);
                    startActivity(intent);
                }
            }
        });


        cancelButton = (Button) getActivity().findViewById(R.id.upload_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exitFragment();
            }
        });

        uploadButton = (Button) getActivity().findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (contributionType == ContributionType.IMAGE) {
                    //Upload image
                    uploadMedia();
                } else if (contributionType == ContributionType.AUDIO) {
                    uploadMedia();
                } else {
                    // encoding needed
                    uploadEncodedMedia();
                }
            }
        });

        setFonts();
    }


    private void showUnsupportedExceptionDialog() {
        new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_alert)
                .setTitle("Device not supported ")
                .setMessage("This Android device does not support FFmpeg media encoding")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        exitFragment();
                    }
                })
                .create().show();
    }

    private void loadFFMpegBinary() {
        ffmpeg = FFmpeg.getInstance(getActivity().getApplicationContext());
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    showUnsupportedExceptionDialog();
                }
            });
        } catch (FFmpegNotSupportedException e) {
            showUnsupportedExceptionDialog();
        }
    }

    private void execFFmpegBinary(String mediaPath, ContributionType contributionType) {
        try {
//            if (ffmpeg.isFFmpegCommandRunning())
//                ffmpeg.killRunningProcesses();
            // to execute "ffmpeg -version" command you just need to pass "-version"
            String cmd;
            if (contributionType == ContributionType.VIDEO)
                //  cmd = "-y -i " + mediaPath + " /storage/emulated/0/DCIM/Camera/output.webm";
//                cmd = "-y -i " + mediaPath + " /storage/emulated/0/DCIM/Camera/output.webm";
                cmd = "-version";
            else
                cmd = "-y -i " + mediaPath + " output.ogg";
            String[] command = cmd.split(" ");
            if (command.length != 0)
                ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                    @Override
                    public void onStart() {
                        progressDialog.setMessage("Processing...");
                        progressDialog.show();
                    }

                    @Override
                    public void onProgress(String message) {
                        progressDialog.setMessage("Processing\n" + message);
                    }

                    @Override
                    public void onFailure(String message) {
                        showToastMessage("Failed to encode media");
                    }

                    @Override
                    public void onSuccess(String message) {
                        Log.d("FFmpeg output", message);
                    }

                    @Override
                    public void onFinish() {
                        progressDialog.dismiss();
                    }
                });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
        }
    }

    private void exitFragment() {
        ((MainActivity) getActivity()).revertStatusBarColor();
        //Close this fragment
        getActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .remove(UploadToCommonsFragment.this)
                .commit();
    }

    public void playAudio(String audioFilePath) throws IOException {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setDataSource(audioFilePath);
        mediaPlayer.prepare();
        mediaPlayer.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        ((MainActivity) getActivity()).revertStatusBarColor();
    }

    private void playVideo() {
        Intent videoIntent = new Intent(getActivity(), VideoPlayerActivity.class);
        videoIntent.putExtra("LocalVideoURI", contributionPath);
        startActivity(videoIntent);

//        ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
//                getActivity(),
//                coverImage,
//                getString(R.string.expandTransition));
//        startActivity(videoIntent, optionsCompat.toBundle());
    }


    private void uploadMedia() {
        String title = uploadTitleTextView.getText().toString();
        String description = uploadDescriptionTextView.getText().toString();
        String comment = uploadCommentTextView.getText().toString();
        String license = extractLicenseString();
        StorageUtil storage = new StorageUtil(getActivity());
        String username = storage.loadUserCredentials();//get username from cache

        if (username == null) {
            showToastMessage("User session deleted \nPlease re-login to upload media");
        } else if (title == null || title.equals("")) {
            showToastMessage("Set the media title");
            //Other fields are optional
        } else {
            commons = new Commons(getActivity().getApplicationContext(), CookieStatus.ENABLED);
            File file;

            if (loadAbsolutePath)
                file = new File(UriToAbsolutePath.getPath(getActivity(), Uri.parse(contributionPath)));
            else
                file = new File(contributionPath);

            if (mediaType == Media.VIDEO)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    file = new File(UriToAbsolutePath.getPath(getActivity(), Uri.parse(contributionPath)));
                } else {
                    file = ((MainActivity) getActivity()).getVideoFile();// get file from activity ..
                }

            //show a loading screen
            FrameLayout loadingScreen = (FrameLayout) getActivity().findViewById(R.id.progressBarHolder);
            loadingScreen.setVisibility(View.VISIBLE);

            User user = new User();
            user.setUsername(username);// all the upload needs the username
            commons.uploadContribution(
                    file,
                    user,
                    title,
                    comment,
                    description,
                    contributionType,
                    license,
                    R.drawable.upload_icon,
                    new UploadCallback() {
                        @Override
                        public void onMediaUploadedSuccessfully() {
                            showToastMessage("Thank you for sharing in Commons");
                            ((MainActivity) getActivity()).triggerContributionLoadRequest();
                            exitFragment();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            showToastMessage(errorMessage);
                            exitFragment();
                        }
                    });
        }
    }

    private void uploadEncodedMedia() {
        String title = uploadTitleTextView.getText().toString();
        String description = uploadDescriptionTextView.getText().toString();
        String comment = uploadCommentTextView.getText().toString();
        String license = extractLicenseString();
        StorageUtil storage = new StorageUtil(getActivity());
        String username = storage.loadUserCredentials();//get username from cache

        if (username == null) {
            showToastMessage("User session deleted \nPlease re-login to upload media");
        } else if (title == null || title.equals("")) {
            showToastMessage("Set the media title");
            //Other fields are optional
        } else {
            commons = new Commons(getActivity().getApplicationContext(), CookieStatus.ENABLED);
            File file;

            if (loadAbsolutePath)
                file = new File(UriToAbsolutePath.getPath(getActivity(), Uri.parse(contributionPath)));
            else
                file = new File(contributionPath);

//            if (mediaType == Media.VIDEO)
//                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
//                    file = new File(UriToAbsolutePath.getPath(getActivity(), Uri.parse(contributionPath)));
//                } else {
//                    file = ((MainActivity) getActivity()).getVideoFile();// get file from activity ..
//                }
            Log.wtf("File", file.getAbsolutePath());
            Log.wtf("File", file.getPath());
            Log.wtf("File", file.getName());

            //show a loading screen
            FrameLayout loadingScreen = (FrameLayout) getActivity().findViewById(R.id.progressBarHolder);
            loadingScreen.setVisibility(View.VISIBLE);

            execFFmpegBinary(file.getAbsolutePath(), contributionType);

//            User user = new User();
//            user.setUsername(username);// all the upload needs the username
//            commons.uploadContribution(
//                    file,
//                    user,
//                    title,
//                    comment,
//                    description,
//                    contributionType,
//                    license,
//                    R.drawable.upload_icon,
//                    new UploadCallback() {
//                        @Override
//                        public void onMediaUploadedSuccessfully() {
//                            showToastMessage("Thank you for sharing in Commons");
//                            ((MainActivity) getActivity()).triggerContributionLoadRequest();
//                            exitFragment();
//                        }
//
//                        @Override
//                        public void onFailure(String errorMessage) {
//                            showToastMessage(errorMessage);
//                            exitFragment();
//                        }
//                    });
        }
    }


    private String extractLicenseString() {
        String license;
        int selectedLicense = upload_license.getSelectedItemPosition();
        switch (selectedLicense) {
            case 0:
                license = Licenses.CreativeCommonsZero;
                break;
            case 1:
                license = Licenses.CreativeCommonsAttributionShare30;
                break;
            case 2:
                license = Licenses.CreativeCommonsAttributionShareAlike30;
                break;
            default:
                license = Licenses.CreativeCommonsAttributionShareAlike30;
                break;
        }
        return license;
    }

    private void showToastMessage(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
    }
}
