package org.wikimedia.commons.wikimedia.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.wikimedia.commons.wikimedia.Activities.MainActivity;
import org.wikimedia.commons.wikimedia.Activities.VideoPlayerActivity;
import org.wikimedia.commons.wikimedia.R;
import org.wikimedia.commons.wikimedia.Utils.RecyclerViewAdapters.MOD_Adapter;
import org.wikimedia.commons.wikimedia.Utils.RecyclerViewClick.RecyclerViewItemClickInterface;
import org.wikimedia.commons.wikimedia.Utils.RecyclerViewClick.RecyclerViewTouchListener;
import org.wikimedia.commons.wikimedia.Utils.SharedPreferencesUtils.StorageUtil;

import java.util.ArrayList;

import apiwrapper.commons.wikimedia.org.Commons;
import apiwrapper.commons.wikimedia.org.Enums.CookieStatus;
import apiwrapper.commons.wikimedia.org.Enums.MediaType;
import apiwrapper.commons.wikimedia.org.Interfaces.RSS_FeedCallback;
import apiwrapper.commons.wikimedia.org.Models.FeedItem;

/**
 * Created by Valdio Veliu on 27/01/2017.
 */

//MOD - Media Of the Day
public class MODFragment extends Fragment {
    private Toolbar toolbar;

    private MOD_Adapter adapter;
    private RecyclerView recyclerView;
    private Commons commons;
    private StorageUtil storage;

    private ArrayList<FeedItem> feedItems;

    public static MODFragment newInstance() {
        return new MODFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        commons = new Commons(getActivity().getApplicationContext(), CookieStatus.DISABLED);
        storage = new StorageUtil(getActivity());
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ((MainActivity) getActivity()).updateStatusBarColor();
        feedItems = new ArrayList<>();

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.feed_layout, container, false);
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setToolbar();
        loadMediaOfTheDay();
    }

    private void initRecyclerView() {
        adapter = new MOD_Adapter(feedItems, getActivity());
        recyclerView = (RecyclerView) getActivity().findViewById(R.id.recyclerviewFeed);
        recyclerView.setAdapter(adapter);

        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 2);
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
                String streamingURL = feedItems.get(position).getStreamingURL();
                if (streamingURL.endsWith(".ogg") || streamingURL.endsWith(".ogv"))
                    Toast.makeText(getActivity(), "Can't play this media. \nNot supported format", Toast.LENGTH_LONG).show();
                else
                    playMedia(view, streamingURL);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

    }

    private void setToolbar() {
        toolbar = (Toolbar) getActivity().findViewById(R.id.feed_toolbar);
        toolbar.setNavigationIcon(R.drawable.backspace_white);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .remove(MODFragment.this)
                        .commit();
                ((MainActivity) getActivity()).revertStatusBarColor();
            }
        });
        toolbar.setTitle("Media of the Day");

    }

    private void loadMediaOfTheDay() {
        if ((feedItems = storage.retrieveRSS_Feed(MediaType.MEDIA)) == null)//load only if MOD is not up to date
            commons.getMediaOfTheDay(new RSS_FeedCallback() {
                @Override
                public void onFeedReceived(ArrayList<FeedItem> arrayList) {
                    storage.storeRSS_Feed(arrayList, MediaType.MEDIA);
                    feedItems = arrayList;
                    initRecyclerView();
                }

                @Override
                public void onError() {
                    Log.wtf("FeedItem", "Error");
                }
            });
        else {
            initRecyclerView();
        }
    }

    private void playMedia(View view, String mediaURL) {
        Intent videoIntent = new Intent(getActivity(), VideoPlayerActivity.class);
        videoIntent.putExtra("LocalVideoURI", mediaURL);

        startActivity(videoIntent);

//        ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
//                getActivity(),
//                view,
//                getString(R.string.expandTransition));
//        startActivity(videoIntent, optionsCompat.toBundle());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ((MainActivity) getActivity()).revertStatusBarColor();
    }
}
