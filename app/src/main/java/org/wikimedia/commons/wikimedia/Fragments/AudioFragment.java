package org.wikimedia.commons.wikimedia.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.wikimedia.commons.wikimedia.Activities.MainActivity;
import org.wikimedia.commons.wikimedia.R;
import org.wikimedia.commons.wikimedia.Utils.RecyclerViewAdapters.AudioRecyclerViewAdapter;
import org.wikimedia.commons.wikimedia.Utils.RecyclerViewClick.RecyclerViewItemClickInterface;
import org.wikimedia.commons.wikimedia.Utils.RecyclerViewClick.RecyclerViewTouchListener;
import org.wikimedia.commons.wikimedia.Utils.SharedPreferencesUtils.StorageUtil;

import java.lang.reflect.Type;
import java.util.ArrayList;

import apiwrapper.commons.wikimedia.org.Models.Contribution;

public class AudioFragment extends Fragment {

    public static final String Broadcast_PLAY_NEW_AUDIO = "org.wikimedia.commons.wikimedia.PlayNewAudio";
    private static final String AUDIO_LIST = "org.wikimedia.commons.wikimedia.AUDIO_LIST";
    private static final String LAST_VISIBLE_AUDIO = "org.wikimedia.commons.wikimedia.LAST_VISIBLE_AUDIO";

    private AudioRecyclerViewAdapter adapter;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;

    private ArrayList<Contribution> contributionsList;
    private SharedPreferences preferences;


    public AudioFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        contributionsList = new ArrayList<>();
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_audio, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            contributionsList = loadContributionsList();
            initRecyclerView();
            //After the RecyclerView is initialized notify the adapter to display the previous available data
            adapter.notifyDataSetChanged();
            int position = savedInstanceState.getInt(LAST_VISIBLE_AUDIO);
            recyclerView.scrollToPosition(position);
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
        adapter = new AudioRecyclerViewAdapter(contributionsList, getActivity());
        recyclerView = (RecyclerView) getActivity().findViewById(R.id.recyclerviewAudios);
        recyclerView.setAdapter(adapter);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        // Item touch Listener
        recyclerView.addOnItemTouchListener(new RecyclerViewTouchListener(getActivity(), recyclerView, new RecyclerViewItemClickInterface() {
            @Override
            public void onClick(View view, int position) {
                playAudio(position);
            }

            @Override
            public void onLongClick(View view, int position) {
            }
        }));
    }

    private void playAudio(int audioIndex) {
        ((MainActivity) getActivity()).playAudio(audioIndex, contributionsList);
    }

    private void loadContributions() {
        ArrayList<Contribution> storedContributions = new StorageUtil(getActivity()).retrieveUserContributions();
        if (storedContributions != null) {
            if (contributionsList == null || contributionsList.size() == 0) {
                for (int i = 0; i < storedContributions.size(); i++) {
                    if (storedContributions.get(i).getMediatype().equals("AUDIO"))
                        //Insert the images in the +IMAGES+ list
                        contributionsList.add(storedContributions.get(i));
                }
            }
            adapter.notifyDataSetChanged();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(ContributionsLoadedBroadcastReceiverAudio, new IntentFilter(MainActivity.CONTRIBUTIONS_LOADED_BROADCAST));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(ContributionsLoadedBroadcastReceiverAudio);
    }

    BroadcastReceiver ContributionsLoadedBroadcastReceiverAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (contributionsList != null) {
                contributionsList.clear();
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
        //get last visible item of RecyclerView
        int lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition();
        outState.putInt(LAST_VISIBLE_AUDIO, lastVisiblePosition);
    }


    //cannot pass large data through bundle, due to TransactionTooLargeException
    // must cache contributionsList
    private void storeContributionsList(ArrayList<Contribution> contributions) {
        preferences = getActivity().getSharedPreferences(AUDIO_LIST, Context.MODE_PRIVATE);
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
        preferences = getActivity().getSharedPreferences(AUDIO_LIST, Context.MODE_PRIVATE);
        String contributions;
        //Check if contributions are previously stored
        if ((contributions = preferences.getString("Contributions", null)) == null) return null;
        if (contributions.equals("")) return null;//no uploads from this user

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<Contribution>>() {
        }.getType();
        return gson.fromJson(contributions, type);
    }
}
