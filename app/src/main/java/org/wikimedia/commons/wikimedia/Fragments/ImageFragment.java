package org.wikimedia.commons.wikimedia.Fragments;

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.wikimedia.commons.wikimedia.Activities.ImageDetailsActivity;
import org.wikimedia.commons.wikimedia.Activities.MainActivity;
import org.wikimedia.commons.wikimedia.R;
import org.wikimedia.commons.wikimedia.Utils.RecyclerViewAdapters.ImagesRecyclerViewAdapter;
import org.wikimedia.commons.wikimedia.Utils.RecyclerViewClick.RecyclerViewItemClickInterface;
import org.wikimedia.commons.wikimedia.Utils.RecyclerViewClick.RecyclerViewTouchListener;
import org.wikimedia.commons.wikimedia.Utils.SharedPreferencesUtils.StorageUtil;

import java.lang.reflect.Type;
import java.util.ArrayList;

import apiwrapper.commons.wikimedia.org.Commons;
import apiwrapper.commons.wikimedia.org.Enums.CookieStatus;
import apiwrapper.commons.wikimedia.org.Interfaces.ThumbnailCallback;
import apiwrapper.commons.wikimedia.org.Models.Contribution;
import apiwrapper.commons.wikimedia.org.Models.Thumbnail;


public class ImageFragment extends Fragment {

    private static final String ALL_IMAGES = "org.wikimedia.commons.wikimedia.ALL_IMAGES";
    private static final String DISPLAYED_IMAGES = "org.wikimedia.commons.wikimedia.DISPLAYED_IMAGES";
    private static final String CURRENTLY_DISPLAYED = "org.wikimedia.commons.wikimedia.CURRENTLY_DISPLAYED";
    private static final String LAST_VISIBLE_ITEM = "org.wikimedia.commons.wikimedia.LAST_VISIBLE_ITEM";
    private Commons commons;

    private ImagesRecyclerViewAdapter adapter;
    private RecyclerView recyclerView;
    private GridLayoutManager layoutManager;

    //The current number of displayed items
    private int DISPLAY_STATUS = 0;
    private int WIDTH16_9 = 0;
    private int HEIGHT16_9 = 0;
    private int WIDTH4_3 = 0;
    private int HEIGHT4_3 = 0;

    private ArrayList<Contribution> contributionsList;
    private ArrayList<Thumbnail> displayedImages; //Thumbnail list to display in RecyclerView

    private SharedPreferences preferences;

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
        displayedImages = new ArrayList<>();
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_images, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            // Restore fragment's state
            contributionsList = loadContributionsList();
            displayedImages = loadDisplayedImages();
            DISPLAY_STATUS = savedInstanceState.getInt(CURRENTLY_DISPLAYED);
            initRecyclerView();
            //After the RecyclerView is initialized notify the adapter to display the previous available data
            adapter.notifyDataSetChanged();
            int position = savedInstanceState.getInt(LAST_VISIBLE_ITEM);
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
        adapter = new ImagesRecyclerViewAdapter(displayedImages, getActivity());
        recyclerView = (RecyclerView) getActivity().findViewById(R.id.recyclerview);
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
                Intent intent = new Intent(getActivity(), ImageDetailsActivity.class);
                intent.putExtra("ImageURL", displayedImages.get(position).getThumbnailURL());
                intent.putExtra("ImageTitle", contributionsList.get(position).getTitle());
                startActivity(intent);
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
        int imageWidth;
        int imageHeight;
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
            commons.loadThumbnail(contributionsList.get(i).getTitle(), imageWidth, imageHeight, new ThumbnailCallback() {
                @Override
                public void onThumbnailAvailable(Thumbnail thumbnail) {
                    //Width and Height might come slightly changed from the required
                    displayedImages.add(thumbnail);
                    //when finished loading images trigger a RecyclerView update
                    if (displayedImages.size() % 10 == 0 || displayedImages.size() == contributionsList.size())
                        adapter.notifyDataSetChanged();
                }

                @Override
                public void onError() {
                }
            });
        }
        DISPLAY_STATUS += 10;
    }

    private void loadContributions() {
        ArrayList<Contribution> storedContributions = new StorageUtil(getActivity()).retrieveUserContributions();
        if (storedContributions != null) {
            if (contributionsList == null || contributionsList.size() == 0) {
                for (int i = 0; i < storedContributions.size(); i++) {
                    if (storedContributions.get(i).getMediatype().equals("BITMAP"))
                        //Insert the images in the +IMAGES+ list
                        if (contributionsList != null)
                            contributionsList.add(storedContributions.get(i));
                }
            }
            updateView(); //Display on the UI
        } else {
            Log.w("StorageUtil", "No available contributions");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(ContributionsLoadedBroadcastReceiverImages, new IntentFilter(MainActivity.CONTRIBUTIONS_LOADED_BROADCAST));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(ContributionsLoadedBroadcastReceiverImages);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        storeContributionsList(contributionsList);
        storeDisplayedImages(displayedImages);
        outState.putInt(CURRENTLY_DISPLAYED, DISPLAY_STATUS);

        //get last visible item of RecyclerView
        int lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition();
        outState.putInt(LAST_VISIBLE_ITEM, lastVisiblePosition);
    }

    BroadcastReceiver ContributionsLoadedBroadcastReceiverImages = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (contributionsList != null) {
                displayedImages.clear();
                contributionsList.clear();
                DISPLAY_STATUS = 0;
                adapter.notifyDataSetChanged();
                loadContributions();
            } else
                loadContributions();
        }
    };


    //cannot pass large data through bundle, due to TransactionTooLargeException
    // must cache contributionsList and displayedImages

    private void storeContributionsList(ArrayList<Contribution> contributions) {
        preferences = getActivity().getSharedPreferences(ALL_IMAGES, Context.MODE_PRIVATE);
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
        preferences = getActivity().getSharedPreferences(ALL_IMAGES, Context.MODE_PRIVATE);
        String contributions;
        //Check if contributions are previously stored
        if ((contributions = preferences.getString("Contributions", null)) == null) return null;
        if (contributions.equals("")) return null;//no uploads from this user

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<Contribution>>() {
        }.getType();
        return gson.fromJson(contributions, type);
    }

    private void storeDisplayedImages(ArrayList<Thumbnail> thumbnails) {
        preferences = getActivity().getSharedPreferences(DISPLAYED_IMAGES, Context.MODE_PRIVATE);
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

    private ArrayList<Thumbnail> loadDisplayedImages() {
        preferences = getActivity().getSharedPreferences(DISPLAYED_IMAGES, Context.MODE_PRIVATE);
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
