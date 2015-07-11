package com.anjoola.sharewear;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Used for displaying different views in the sliding tab layout.
 */
public class ViewPagerAdapter extends FragmentStatePagerAdapter {
    CharSequence mTitles[];
    int mNumTabs;

    public ViewPagerAdapter(FragmentManager fm, CharSequence titles[]) {
        super(fm);
        mTitles = titles;
        mNumTabs = mTitles.length;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new ContactsFavoriteFragment();
            case 1:
                return new ContactsAllFragment();
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
