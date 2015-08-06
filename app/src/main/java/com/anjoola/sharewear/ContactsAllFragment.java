package com.anjoola.sharewear;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import com.anjoola.sharewear.util.ContactDetails;
import com.anjoola.sharewear.util.ContactsListAdapter;
import com.anjoola.sharewear.util.ContactsListLoader;

public class ContactsAllFragment extends Fragment implements
        AdapterView.OnItemClickListener {
    // Adapter for mapping contacts to objects in the ListViews.
    public ContactsListAdapter mAdapter;

    // "Syncing..." display.
    private View mSyncing;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.contacts_all_fragment, container, false);

        // Set up contacts list view and adapter.
        ListView contactsList = (ListView) v.findViewById(R.id.contacts_list_all);
        contactsList.setOnItemClickListener(this);

        final ShareWearApplication app =
                (ShareWearApplication) getActivity().getApplication();
        mAdapter = new ContactsListAdapter(getActivity(), app.mContactsList);
        contactsList.setAdapter(mAdapter);
        mSyncing = v.findViewById(R.id.syncing_view);

        if (app.mContactListPreloaded && app.mContactsList.size() > 0)
            mSyncing.setVisibility(View.GONE);

        if (!app.mContactListLoaded) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    int idx = app.mContactsList.size() == 0 ? 0 :
                            MainActivity.NUM_CONTACTS_PRELOAD;
                    new ContactsListLoaderAsync(app, getActivity(), idx, -1)
                            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            });
            app.mContactListLoaded = true;
        }

        contactsList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState != 0)
                    mAdapter.isScrolling = true;
                else {
                    mAdapter.isScrolling = false;
                    mAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
            }
        });

        contactsList.setFastScrollEnabled(true);
        contactsList.setAnimationCacheEnabled(false);
        contactsList.setDrawingCacheEnabled(false);

        mAdapter.notifyDataSetChanged();
        return v;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Hide the search view.
        ((ContactsListActivity) getActivity()).hideSearchView();

        ContactDetails entry = (ContactDetails) parent.getAdapter().getItem(position);

        // Send contact details over to activity.
        Intent intent = new Intent(getActivity(), ContactViewActivity.class);
        intent.putExtra(ShareWearActivity.PHOTO, entry.photoUri);
        intent.putExtra(ShareWearActivity.NAME, entry.name);
        intent.putExtra(ShareWearActivity.PHONE, entry.phone);
        intent.putExtra(ShareWearActivity.EMAIL, entry.email);
        startActivity(intent);
    }

    /**
     * Asynchronous class to retrieve and load ListView with contacts.
     */
    public class ContactsListLoaderAsync extends AsyncTask<Void, Integer, Void> {
        private ShareWearApplication app;
        private Activity activity;

        // Cursor for retrieving all contacts.
        private int offset;

        // The number of contacts to load. -1 for all of them.
        private int numLoad;

        public ContactsListLoaderAsync(ShareWearApplication app,
                                       Activity activity, int offset,
                                       int numLoad) {
            this.app = app;
            this.activity = activity;
            this.offset = offset;
            this.numLoad = numLoad;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ContactsListLoader.loadContacts(app, activity, offset, numLoad);
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            mAdapter.notifyDataSetChanged();
            mSyncing.setVisibility(View.GONE);
        }
    }
}
