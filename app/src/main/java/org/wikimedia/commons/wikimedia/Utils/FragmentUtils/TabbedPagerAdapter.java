package org.wikimedia.commons.wikimedia.Utils.FragmentUtils;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;


import org.wikimedia.commons.wikimedia.Fragments.AudioFragment;
import org.wikimedia.commons.wikimedia.Fragments.ImageFragment;
import org.wikimedia.commons.wikimedia.Fragments.VideoFragment;



/**
 * Created by Valdio Veliu on 16-11-16.
 */

public class TabbedPagerAdapter extends FragmentStatePagerAdapter {

    //3 tabs for the application
    public TabbedPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment = null;
        Bundle bundle = new Bundle();

        if (position == 0) {
            fragment = new ImageFragment();
        }
        if (position == 1) {
            fragment = new VideoFragment();
        }
        if (position == 2) {
            fragment = new AudioFragment();
        }
        return fragment;
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {

        CharSequence Tittle = "";
        if (position == 0) {
            Tittle = "Photo";
        }
        if (position == 1) {
            Tittle = "Video";
        }
        if (position == 2) {
            Tittle = "Audio";
        }
        return Tittle;
    }
}
