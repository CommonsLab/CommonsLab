package com.commonslab.commonslab.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import com.commonslab.commonslab.Activities.MainActivity;
import com.commonslab.commonslab.Activities.VideoPlayerActivity;
import com.commonslab.commonslab.R;
import com.commonslab.commonslab.Utils.RecyclerViewAdapters.VideosRecyclerViewAdapter;
import com.commonslab.commonslab.Utils.RecyclerViewClick.RecyclerViewItemClickInterface;
import com.commonslab.commonslab.Utils.RecyclerViewClick.RecyclerViewTouchListener;
import com.commonslab.commonslab.Utils.SharedPreferencesUtils.StorageUtil;

import java.lang.reflect.Type;
import java.util.ArrayList;

import apiwrapper.commons.wikimedia.org.Commons;
import apiwrapper.commons.wikimedia.org.Enums.CookieStatus;
import apiwrapper.commons.wikimedia.org.Interfaces.ThumbnailCallback;
import apiwrapper.commons.wikimedia.org.Models.Contribution;
import apiwrapper.commons.wikimedia.org.Models.Thumbnail;

public class VideoFragment extends Fragment {
    private static final String ALL_VIDEOS = "org.wikimedia.commons.wikimedia.ALL_VIDEOS";
    private static final String DISPLAYED_VIDEOS = "org.wikimedia.commons.wikimedia.DISPLAYED_VIDEOS";
    private static final String LAST_VISIBLE_VIDEO = "org.wikimedia.commons.wikimedia.LAST_VISIBLE_VIDEO";
    private static final String CURRENTLY_DISPLAYED_VIDEOS = "org.wikimedia.commons.wikimedia.CURRENTLY_DISPLAYED_VIDEOS";

    private Commons commons;
    private VideosRecyclerViewAdapter adapter;
    private RecyclerView recyclerView;
    private GridLayoutManager layoutManager;

    //The current number of displayed items
    private int DISPLAY_STATUS = 0;
    private int WIDTH16_9 = 0;
    private int HEIGHT16_9 = 0;
    private int WIDTH4_3 = 0;
    private int HEIGHT4_3 = 0;

    private ArrayList<Contribution> contributionsList;
    private ArrayList<Thumbnail> displayedVideos; //Thumbnail list to display in RecyclerView

    private SharedPreferences preferences;

    public VideoFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        commons = new Commons(getActivity().getApplicationContext(), CookieStatus.DISABLED);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //Get device WINDOW size
        //The thumbnail will be requested according to the device resolution
        // Standard display --> 16:9  &  4:3
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int height = displaymetrics.heightPixels;
        int width = displaymetrics.widthPixels;

        //define the with and height of the requested thumbnails according to the screen display of the device
        WIDTH16_9 = width;
        HEIGHT16_9 = (WIDTH16_9 / 16) * 9;

        WIDTH4_3 = width / 2;
        HEIGHT4_3 = (WIDTH4_3 / 4) * 3;

        contributionsList = new ArrayList<>();
        displayedVideos = new ArrayList<>();

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_video, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            // Restore fragment's state
            contributionsList = loadContributionsList();
            displayedVideos = loadDisplayedVideos();
            DISPLAY_STATUS = savedInstanceState.getInt(CURRENTLY_DISPLAYED_VIDEOS);
            initRecyclerView();
            //After the RecyclerView is initialized notify the adapter to display the previous available data
            adapter.notifyDataSetChanged();
            int position = savedInstanceState.getInt(LAST_VISIBLE_VIDEO);
            recyclerView.scrollToPosition(position);
            updateView();
        }

        if (savedInstanceState == null) {
            initRecyclerView();
            if (new StorageUtil(getActivity()).usersContributionsAvailable() && contributionsList.size() == 0) {
                //must reload data from cache
                loadContributions();
            }
        }
    }


    private void initRecyclerView() {
        adapter = new VideosRecyclerViewAdapter(displayedVideos, getActivity());
        recyclerView = (RecyclerView) getActivity().findViewById(R.id.recyclerviewVideos);
        recyclerView.setAdapter(adapter);

        layoutManager = new GridLayoutManager(getActivity(), 2);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position % 3 == 0)
                    return 2; //fill 2 grid spaces for %3 index images
                else
                    return 1; //set  2 images per row for the rest
            }
        });
        recyclerView.setLayoutManager(layoutManager);


        // Item touch Listener
        recyclerView.addOnItemTouchListener(new RecyclerViewTouchListener(getActivity(), recyclerView, new RecyclerViewItemClickInterface() {
            @Override
            public void onClick(View view, int position) {
                Intent videoIntent = new Intent(getActivity(), VideoPlayerActivity.class);
                videoIntent.putExtra("Contribution", contributionsList.get(position));
                videoIntent.putExtra("ThumbnailURL", displayedVideos.get(position).getThumbnailURL());
                startActivity(videoIntent);
//                ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
//                        getActivity(),
//                        view,
//                        getString(R.string.expandTransition));
//                startActivity(videoIntent, optionsCompat.toBundle());
            }

            @Override
            public void onLongClick(View view, int position) {
            }
        }));

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    //check for scroll down
                    if (layoutManager.findLastCompletelyVisibleItemPosition() > DISPLAY_STATUS - 4)
                        updateView();//load more images
                }
            }
        });
    }

    //Start a view update
    private void updateView() {
        if (contributionsList == null) return;
        int imageWidth = 0;
        int imageHeight = 0;
        for (int i = DISPLAY_STATUS; i < DISPLAY_STATUS + 10 && i < contributionsList.size(); i++) {
            //specify the thumbnail size according to device screen
            if (i % 3 == 0) {
                imageWidth = WIDTH16_9;
                imageHeight = HEIGHT16_9;
            } else {
                imageWidth = WIDTH4_3;
                imageHeight = HEIGHT4_3;
            }

            //load 10 images
            try {
                commons.loadThumbnail(contributionsList.get(i).getTitle(), imageWidth, imageHeight, new ThumbnailCallback() {
                    @Override
                    public void onThumbnailAvailable(Thumbnail thumbnail) {
                        //Width and Height might come slightly changed from the required
                        displayedVideos.add(thumbnail);
                        //when finished loading images trigger a RecyclerView update
                        if (displayedVideos.size() % 10 == 0 || displayedVideos.size() == contributionsList.size())
                            adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError() {
                    }
                });
            } catch (Exception e) {
            }
        }
        DISPLAY_STATUS += 10;
    }

    private void loadContributions() {
        ArrayList<Contribution> storedContributions = new StorageUtil(getActivity()).retrieveUserContributions();
        if (storedContributions != null) {
            if (contributionsList == null || contributionsList.size() == 0) {
                for (int i = 0; i < storedContributions.size(); i++) {
                    if (storedContributions.get(i).getMediatype().equals("VIDEO"))
                        //Insert the images in the +IMAGES+ list
                        if (contributionsList != null)
                            contributionsList.add(storedContributions.get(i));
                }
            }
            updateView(); //Display on the UI
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(ContributionsLoadedBroadcastReceiverVideos, new IntentFilter(MainActivity.CONTRIBUTIONS_LOADED_BROADCAST));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(ContributionsLoadedBroadcastReceiverVideos);
    }

    BroadcastReceiver ContributionsLoadedBroadcastReceiverVideos = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (contributionsList != null) {
                displayedVideos.clear();
                contributionsList.clear();
                DISPLAY_STATUS = 0;
                adapter.notifyDataSetChanged();
                loadContributions();
            } else
                loadContributions();
        }
    };


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        storeContributionsList(contributionsList);
        storeDisplayedVideos(displayedVideos);
        outState.putInt(CURRENTLY_DISPLAYED_VIDEOS, DISPLAY_STATUS);

        //get last visible item of RecyclerView
        int lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition();
        outState.putInt(LAST_VISIBLE_VIDEO, lastVisiblePosition);
    }

    //cannot pass large data through bundle, due to TransactionTooLargeException
    // must cache contributionsList and displayedVideos
    private void storeContributionsList(ArrayList<Contribution> contributions) {
        preferences = getActivity().getSharedPreferences(ALL_VIDEOS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        //clear existing data
        editor.clear();
        editor.apply();

        if (contributions == null || contributions.size() == 0) {
            //No contributions yet
            editor.putString("Contributions", "");
            editor.apply();
        } else {
            Gson gson = new Gson();
            String json = gson.toJson(contributions);
            //Store contributions
            editor.putString("Contributions", json);
            editor.apply();
        }
    }

    private ArrayList<Contribution> loadContributionsList() {
        preferences = getActivity().getSharedPreferences(ALL_VIDEOS, Context.MODE_PRIVATE);
        String contributions;
        //Check if contributions are previously stored
        if ((contributions = preferences.getString("Contributions", null)) == null) return null;
        if (contributions.equals("")) return null;//no uploads from this user

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<Contribution>>() {
        }.getType();
        return gson.fromJson(contributions, type);
    }

    private void storeDisplayedVideos(ArrayList<Thumbnail> thumbnails) {
        preferences = getActivity().getSharedPreferences(DISPLAYED_VIDEOS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        //clear existing data
        editor.clear();
        editor.apply();

        if (thumbnails == null || thumbnails.size() == 0) {
            //No contributions yet
            editor.putString("Thumbnails", "");
            editor.apply();
        } else {
            Gson gson = new Gson();
            String json = gson.toJson(thumbnails);
            //Store contributions
            editor.putString("Thumbnails", json);
            editor.apply();
        }
    }

    private ArrayList<Thumbnail> loadDisplayedVideos() {
        preferences = getActivity().getSharedPreferences(DISPLAYED_VIDEOS, Context.MODE_PRIVATE);
        String thumbnails;
        //Check if contributions are previously stored
        if ((thumbnails = preferences.getString("Thumbnails", null)) == null) return null;
        if (thumbnails.equals("")) return null;

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<Thumbnail>>() {
        }.getType();
        return gson.fromJson(thumbnails, type);
    }
}
