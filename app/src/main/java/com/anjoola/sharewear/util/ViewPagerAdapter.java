package com.anjoola.sharewear.util;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.anjoola.sharewear.ContactsAllFragment;
import com.anjoola.sharewear.ContactsFavoriteFragment;
import com.anjoola.sharewear.ShareWearApplication;

/**
 * Used for displaying different views in the sliding tab layout.
 */
public class ViewPagerAdapter extends FragmentStatePagerAdapter {
    private CharSequence mTitles[];
    private int mNumTabs;

    // Fragments to be displayed.
    public Fragment first = null, second = null;

    public ViewPagerAdapter(FragmentManager fm, CharSequence titles[],
                            ShareWearApplication app) {
        super(fm);
        mTitles = titles;
        mNumTabs = mTitles.length;

        first = new ContactsFavoriteFragment();
        second = new ContactsAllFragment();

        // Used to refresh the views later.
        app.favoritesFragment = first;
        app.allFragment = second;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return first;
            case 1:
                return second;
        }
        return null;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTitles[position];
    }

    @Override
    public int getCount() {
        return mNumTabs;
    }
}
