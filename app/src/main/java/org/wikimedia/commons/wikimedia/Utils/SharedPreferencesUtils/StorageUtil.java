package org.wikimedia.commons.wikimedia.Utils.SharedPreferencesUtils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.wikimedia.commons.wikimedia.R;
import org.wikimedia.commons.wikimedia.Utils.Today;

import java.lang.reflect.Type;
import java.util.ArrayList;

import apiwrapper.commons.wikimedia.org.Models.FeedItem;
import apiwrapper.commons.wikimedia.org.Enums.MediaType;
import apiwrapper.commons.wikimedia.org.Models.Contribution;

/**
 * Created by Valdio Veliu on 31/01/2017.
 */

public class StorageUtil {
    private SharedPreferences preferences;
    private Context context;

    public StorageUtil(Context context) {
        this.context = context;
    }


    public void storeUserCredentials(String username) {
        //Store user credentials in SharedPreferences
        preferences = context.getSharedPreferences(context.getString(R.string.User_Credentials), Context.MODE_PRIVATE);
        preferences.edit().putString("username", username).apply();
        preferences.edit().putBoolean("loggedIn", true).apply();
    }

    public boolean validUserSession() {
        preferences = context.getSharedPreferences(context.getString(R.string.User_Credentials), Context.MODE_PRIVATE);
        String username = preferences.getString("username", null);
        boolean loggedIn = preferences.getBoolean("loggedIn", false);
        return (username != null && loggedIn);
    }

    public String loadUserCredentials() {
        preferences = context.getSharedPreferences(context.getString(R.string.User_Credentials), Context.MODE_PRIVATE);
        return preferences.getString("username", null);
    }

    public void clearUserSession() {
        preferences = context.getSharedPreferences(context.getString(R.string.User_Credentials), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }


    public void storeUserContributions(ArrayList<Contribution> contributions) {
        preferences = context.getSharedPreferences(context.getString(R.string.User_Contributions), Context.MODE_PRIVATE);
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

    public void setUserContributionsStatus(boolean status) {
        preferences = context.getSharedPreferences(context.getString(R.string.User_Contributions), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("ContributionsAvailable", status);
        editor.apply();
    }

    public boolean usersContributionsAvailable() {
        preferences = context.getSharedPreferences(context.getString(R.string.User_Contributions), Context.MODE_PRIVATE);
        return preferences.getBoolean("ContributionsAvailable", false);
    }

    public ArrayList<Contribution> retrieveUserContributions() {
        preferences = context.getSharedPreferences(context.getString(R.string.User_Contributions), Context.MODE_PRIVATE);
        String contributions;
        //Check if contributions are previously stored
        if ((contributions = preferences.getString("Contributions", null)) == null) return null;
        if (contributions.equals("")) return null;//no uploads from this user

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<Contribution>>() {
        }.getType();
        return gson.fromJson(contributions, type);
    }


    //Store RSS feed, Picture and Media of the day
    public void storeRSS_Feed(ArrayList<FeedItem> list, MediaType mediaType) {
        if (mediaType == MediaType.PICTURE) {
            preferences = context.getSharedPreferences(context.getString(R.string.Picture_of_the_day), Context.MODE_PRIVATE);
        } else {
            preferences = context.getSharedPreferences(context.getString(R.string.Media_of_the_day), Context.MODE_PRIVATE);
        }
        SharedPreferences.Editor prefsEditor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        //Store date of this RSS feed
        prefsEditor.putString("Date", new Today().date());
        prefsEditor.putString("RSS_Feed", json);
        prefsEditor.apply();
    }

    //Retrieve RSS feed, Picture and Media of the day
    public ArrayList<FeedItem> retrieveRSS_Feed(MediaType mediaType) {
        if (mediaType == MediaType.PICTURE) {
            preferences = context.getSharedPreferences(context.getString(R.string.Picture_of_the_day), Context.MODE_PRIVATE);
        } else {
            preferences = context.getSharedPreferences(context.getString(R.string.Media_of_the_day), Context.MODE_PRIVATE);
        }

        //Check if the feed is valid
        if (!preferences.getString("Date", "").equals(new Today().date())) return null;

        Gson gson = new Gson();
        String json = preferences.getString("RSS_Feed", null);
        Type type = new TypeToken<ArrayList<FeedItem>>() {
        }.getType();
        return gson.fromJson(json, type);
    }

}
