package com.commonslab.commonslab.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.commonslab.commonslab.Activities.ImageDetailsActivity;
import com.commonslab.commonslab.Activities.MainActivity;
import com.commonslab.commonslab.R;
import com.commonslab.commonslab.Utils.RecyclerViewAdapters.POD_Adapter;
import com.commonslab.commonslab.Utils.RecyclerViewClick.RecyclerViewItemClickInterface;
import com.commonslab.commonslab.Utils.RecyclerViewClick.RecyclerViewTouchListener;
import com.commonslab.commonslab.Utils.SharedPreferencesUtils.StorageUtil;

import java.util.ArrayList;

import apiwrapper.commons.wikimedia.org.Commons;
import apiwrapper.commons.wikimedia.org.Enums.CookieStatus;
import apiwrapper.commons.wikimedia.org.Enums.MediaType;
import apiwrapper.commons.wikimedia.org.Interfaces.RSS_FeedCallback;
import apiwrapper.commons.wikimedia.org.Models.FeedItem;

//POD - Picture Of the Day
public class PODFragment extends Fragment {
    private Toolbar toolbar;

    private POD_Adapter adapter;
    private RecyclerView recyclerView;
    private Commons commons;

    private ArrayList<FeedItem> feedItems;
    private StorageUtil storage;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        commons = new Commons(getActivity().getApplicationContext(), CookieStatus.DISABLED);
        storage = new StorageUtil(getActivity());
    }

    public static PODFragment newInstance() {
        return new PODFragment();
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
        loadPictureOfTheDay();
    }

    private void loadPictureOfTheDay() {
        if ((feedItems = storage.retrieveRSS_Feed(MediaType.PICTURE)) == null)//load only if POD is not up to date
            commons.getPictureOfTheDay(new RSS_FeedCallback() {
                @Override
                public void onFeedReceived(ArrayList<FeedItem> arrayList) {
                    storage.storeRSS_Feed(arrayList, MediaType.PICTURE);
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


    private void initRecyclerView() {
        adapter = new POD_Adapter(feedItems, getActivity());
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
                Intent intent = new Intent(getActivity(), ImageDetailsActivity.class);
                intent.putExtra("ImageURL", feedItems.get(position).getMediaLink());
                intent.putExtra("ImageTitle", feedItems.get(position).getTitle());
                startActivity(intent);

//                ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
//                        getActivity(),
//                        view,
//                        getString(R.string.expandTransition));
//                startActivity(intent, optionsCompat.toBundle());
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
                        .remove(PODFragment.this)
                        .commit();
                ((MainActivity) getActivity()).revertStatusBarColor();
            }
        });
        toolbar.setTitle(getResources().getString(R.string.picture_of_the_day));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ((MainActivity) getActivity()).revertStatusBarColor();
    }
}
