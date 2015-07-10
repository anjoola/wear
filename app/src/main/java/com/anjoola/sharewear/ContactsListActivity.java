package com.anjoola.sharewear;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;

/**
 * Displays list of contacts and favorites. Allows user to add and search
 * for contacts. Floating action button allows user to share their location.
 */
public class ContactsListActivity extends ShareWearActivity implements
        SearchView.OnQueryTextListener, View.OnClickListener, OnItemClickListener {
    // Floating action button for getting current location.
    android.support.design.widget.FloatingActionButton mFab;

    // Adapter for mapping contacts to objects in the ListViews.
    private SimpleCursorAdapter mFavoritesAdapter, mAdapter;
    private DoubleListAdapter mDoubleAdapter;

    // Asynchronous task to retrieve and load ListView with contacts.
    private ContactsListLoader mLoader;
    private MatrixCursor mMatrixCursor;

    // Number of contacts to load at once.
    public static int LOAD_NUM = 20;

    // For getting images for contacts.
    private ContactsImageProvider mImgProvider;

    // For searching.
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

        mImgProvider = new ContactsImageProvider(getBaseContext());

        // Set up contacts list view. Set adapter and scroll listener for
        // infinite scrolling.
        ListView contactsList = (ListView) findViewById(R.id.contacts_list);
        contactsList.setOnScrollListener(new EndlessScrollListener());
        contactsList.setOnItemClickListener(this);

        mFavoritesAdapter = new SimpleCursorAdapter(this,
                R.layout.contacts_list_view_favorite,
                null,
                new String[] { "photo", "name" },
                new int[] { R.id.photo, R.id.name }, 0);
        mAdapter = new SimpleCursorAdapter(this,
                R.layout.contacts_list_view_layout,
                null,
                new String[] { "photo", "name" },
                new int[] { R.id.photo, R.id.name }, 0);
        mDoubleAdapter = new DoubleListAdapter(this,
                getString(R.string.favorites), mFavoritesAdapter,
                getString(R.string.all_contacts), mAdapter);
        contactsList.setAdapter(mDoubleAdapter);

        // Cursor for loading all details.
        mMatrixCursor = new MatrixCursor(
                new String[] {"_id", "photo", "name", "phone", "email"});
        runOnUiThread(new Runnable() {
            public void run() {
                new ContactsListLoader(0).execute();
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fab_my_location) {
            Intent intent = new Intent(this, MyLocationActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Hide search view if it is shown.
        if (mSearchView.isShown()) {
            mSearchMenuItem.collapseActionView();
            mSearchView.setQuery("", false);
        }

        MatrixCursor entry = (MatrixCursor) parent.getAdapter().getItem(position);

        // Send contact details over to activity.
        Intent intent = new Intent(this, ContactViewActivity.class);
        intent.putExtra(ShareWearActivity.PHOTO, entry.getString(1));
        intent.putExtra(ShareWearActivity.NAME, entry.getString(2));
        intent.putExtra(ShareWearActivity.PHONE, entry.getString(3));
        intent.putExtra(ShareWearActivity.EMAIL, entry.getString(4));
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.contacts_list_menu, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchMenuItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) mSearchMenuItem.getActionView();

        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        mSearchView.setSubmitButtonEnabled(true);
        mSearchView.setOnQueryTextListener(this);
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
        return false;
    }

    /**
     * Asynchronous class to retrieve and load ListView with contacts.
     */
    private class ContactsListLoader extends AsyncTask<Void, Void, Cursor> {
        // URIs for retrieving contacts information.
        private final Uri CONTACTS_URI = ContactsContract.Contacts.CONTENT_URI;
        private final Uri DATA_URI = ContactsContract.Data.CONTENT_URI;

        // Cursor for retrieving all contacts.
        private int offset;

        public ContactsListLoader(int offset) {
            this.offset = offset;
        }

        @Override
        protected Cursor doInBackground(Void... params) {
            Cursor cursor = getContentResolver().query(CONTACTS_URI, null, null,
                    null, ContactsContract.Contacts.DISPLAY_NAME + " ASC ");

            // Move to the specified offset, if it exists.
            if (!cursor.moveToPosition(offset * LOAD_NUM))
                return null;

            // Loop through every contact.
            int numLoaded = 0;
            do {
                long contactId = cursor.getLong(cursor.getColumnIndex("_ID"));

                // Retrieve individual fields for this contact.
                Cursor dCursor = getContentResolver().query(DATA_URI, null,
                        ContactsContract.Data.CONTACT_ID + "=" + contactId,
                        null, null);

                // No data, move on.
                if (!dCursor.moveToFirst()) continue;

                getContactDetails(dCursor, contactId);

            } while (++numLoaded < LOAD_NUM && cursor.moveToNext());

            cursor.close();
            return mMatrixCursor;
        }

        @Override
        protected void onPostExecute(Cursor result) {
            // TODO get favorites
            //mFavoritesAdapter.swapCursor(result);
            mAdapter.swapCursor(result);
            mDoubleAdapter.notifyDataSetChanged();
        }

        /**
         * Get details for the contact at the cursor and add it to the matrix
         * cursor.
         *
         * @param cursor The cursor.
         */
        private void getContactDetails(Cursor cursor, long contactId) {
            // Details to retrieve.
            String photoUri = cursor.getString(cursor.getColumnIndex(
                    Phone.PHOTO_URI));
            String name = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.Contacts.DISPLAY_NAME));
            String phone = null;
            String email = null;

            // Loop since there might be multiple entries.
            do {
                String columnType = cursor.getString(cursor.getColumnIndex("mimetype"));

                // Phone numbers.
                if (columnType.equals(Phone.CONTENT_ITEM_TYPE)) {
                    switch (cursor.getInt(cursor.getColumnIndex("data2"))) {
                        case Phone.TYPE_MOBILE:
                            phone = cursor.getString(cursor.getColumnIndex("data1"));
                            break;
                        case Phone.TYPE_WORK:
                            if (phone == null)
                                phone = cursor.getString(cursor.getColumnIndex("data1"));
                            break;
                        case Phone.TYPE_HOME:
                            if (phone == null)
                                phone = cursor.getString(cursor.getColumnIndex("data1"));
                            break;
                        case Phone.TYPE_OTHER:
                            if (phone == null)
                                phone = cursor.getString(cursor.getColumnIndex("data1"));
                            break;
                    }
                }

                // Email.
                if (columnType.equals(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)) {
                    switch (cursor.getInt(cursor.getColumnIndex("data2"))) {
                        case ContactsContract.CommonDataKinds.Email.TYPE_HOME:
                            email = cursor.getString(cursor.getColumnIndex("data1"));
                            break;
                        case ContactsContract.CommonDataKinds.Email.TYPE_WORK:
                            if (email == null)
                                email = cursor.getString(cursor.getColumnIndex("data1"));
                            break;
                        case ContactsContract.CommonDataKinds.Email.TYPE_OTHER:
                            if (email == null)
                                email = cursor.getString(cursor.getColumnIndex("data1"));
                            break;
                    }
                }
            } while (cursor.moveToNext());

            // If contact has neither a phone nor email, don't show them.
            if (phone == null && email == null) return;

            // Get default photo if contact photo does not exist.
            if (photoUri == null)
                photoUri = mImgProvider.getDefaultContactUri(name);

            // Add contact details to cursor.
            mMatrixCursor.addRow(new Object[]{
                    Long.toString(contactId),
                    photoUri,
                    name,
                    phone,
                    email
            });
        }
    }

    // TODO, doesn't work
    private class EndlessScrollListener implements AbsListView.OnScrollListener {
        // Offset loaded.
        private int offset = 0;

        private int previousTotal = 0;
        private boolean loading = false;


        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {
            if (loading && totalItemCount > previousTotal) {
                loading = false;
            }
            if (!loading && (firstVisibleItem + visibleItemCount - 2 >= previousTotal + LOAD_NUM - 10)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        new ContactsListLoader(++offset).execute();
                        previousTotal += LOAD_NUM;
                        loading = true;
                    }
                });
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) { }
    }
}
