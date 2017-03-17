package org.wikimedia.commons.wikimedia.AudioPlayer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import apiwrapper.commons.wikimedia.org.Models.Contribution;


/**
 * Created by valdio on 06/12/2016.
 */

public class StorageUtilAudioPlayer {

    private final String STORAGE = "org.wikimedia.commons.wikimedia.STORAGE";
    private SharedPreferences preferences;
    private Context context;

    public StorageUtilAudioPlayer(Context context) {
        this.context = context;
    }

    public void storeAudio(ArrayList<Contribution> arrayList) {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(arrayList);
        editor.putString("audioArrayList", json);
        editor.apply();
    }

    public ArrayList<Contribution> loadAudio() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString("audioArrayList", null);
        Type type = new TypeToken<ArrayList<Contribution>>() {
        }.getType();
        return gson.fromJson(json, type);
    }

    public void storeAudioIndex(int index) {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("audioIndex", index);
        editor.apply();
    }

    public int loadAudioIndex() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        return preferences.getInt("audioIndex", -1);//return -1 if no data found
    }

    public void clearCachedAudioPlaylist() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.commit();
    }


    /**
     * Used to set a storage reference that the player is active
     */
    public void setPlayerFlagActive() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("MediaPlayerStatus", true);// is playing
        editor.apply();
    }

    /**
     * Clear the player reference, MediaPlayer is not playing.
     */
    public void clearPlayerFlag() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("MediaPlayerStatus", false);// is not playing
        editor.apply();
    }

    /**
     * Get the flag status from storage
     */
    public boolean getPlayerFlag() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        return preferences.getBoolean("MediaPlayerStatus", false);//return -1 if no data found
    }


    /**
     * Used to perform clear operation when the MainActivity is being closed
     */
    public void clearAllPlayerData() {
        clearPlayerFlag();
        clearCachedAudioPlaylist();
    }
}