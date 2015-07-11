package com.anjoola.sharewear;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;

import com.anjoola.sharewear.util.SlidingTabLayout;
import com.anjoola.sharewear.util.ViewPagerAdapter;

/**
 * Displays list of contacts and favorites. Allows user to add and search
 * for contacts. Floating action button allows user to share their location.
 *
 * Tab layout courtesy of
 * http://www.tutorialsbuzz.com/2015/04/Android-Material-Design-Sliding-TabLayout.html.
 */
public class ContactsListActivity extends ShareWearActivity implements
        SearchView.OnQueryTextListener, View.OnClickListener,
        ViewPager.OnPageChangeListener {
    // Floating action button for getting current location.
    android.support.design.widget.FloatingActionButton mFab;

    // For searching.
    Fragment mFragment;
    MenuItem mSearchMenuItem;
    SearchView mSearchView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contacts_list_activity);

        // Set up handler for getting current location floating action button.
        mFab = (android.support.design.widget.FloatingActionButton)
                findViewById(R.id.fab_my_location);
        mFab.setOnClickListener(this);

        // Hide action bar shadow.
        if (getActionBar() != null)
            getActionBar().setElevation(0);

        // Create tabs.
        CharSequence tabTitles[] = {
            getString(R.string.favorites),
            getString(R.string.all_contacts)
        };
        SlidingTabLayout tabs = (SlidingTabLayout) findViewById(R.id.tabs);
        tabs.setDistributeEvenly(true);

        // Page adapter for sliding view.
        ViewPagerAdapter adapter =
                new ViewPagerAdapter(getSupportFragmentManager(), tabTitles);
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(adapter);
        tabs.setViewPager(pager);
        tabs.setOnPageChangeListener(this);

        mFragment = adapter.second;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fab_my_location) {
            Intent intent = new Intent(this, MyLocationActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.contacts_list_menu, menu);

        SearchManager searchManager = (SearchManager)
                getSystemService(Context.SEARCH_SERVICE);
        mSearchMenuItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) mSearchMenuItem.getActionView();

        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setQueryHint(getString(R.string.search_contacts));
        mSearchMenuItem.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                Intent intent = new Intent(this, ContactAddNFCActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_sign_out:
                signOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        // Filter contacts.
        ((ContactsAllFragment) mFragment).mAdapter.getFilter().filter(newText);
        return true;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset,
                               int positionOffsetPixels) { }

    @Override
    public void onPageSelected(int position) {
        // Hide search view in favorites page.
        mSearchMenuItem.setVisible(position != 0);
        if (position == 0)
            mSearchMenuItem.collapseActionView();
    }

    @Override
    public void onPageScrollStateChanged(int state) { }

    /**
     * Hides the search view if it is shown.
     */
    public void hideSearchView() {
        if (mSearchView.isShown()) {
            mSearchMenuItem.collapseActionView();
            mSearchView.setQuery("", false);
        }
    }
}
