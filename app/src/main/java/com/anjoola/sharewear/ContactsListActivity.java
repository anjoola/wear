package com.anjoola.sharewear;

import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

/**
 * Displays list of contacts and favorites. Allows user to add and search
 * for contacts. Floating action button allows user to share their location.
 */
public class ContactsListActivity extends ShareWearActivity implements
        View.OnClickListener, OnItemClickListener {
    // Floating action button for getting current location.
    android.support.design.widget.FloatingActionButton mFab;

    // ListView to display all contacts.
    private ListView mContactsList;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contacts_list_activity);

        // Set up handler for getting current location floating action button.
        mFab = (android.support.design.widget.FloatingActionButton)
                findViewById(R.id.fab_my_location);
        mFab.setOnClickListener(this);

        mImgProvider = new ContactsImageProvider(getBaseContext());

        // TODO
        // Set up contacts list view. Set adapter and scroll listener for
        // infinite scrolling.
        mContactsList = (ListView) findViewById(R.id.contacts_list);
        mContactsList.setOnScrollListener(new EndlessScrollListener());
        mContactsList.setOnItemClickListener(this);

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

        // TODO
        /*
        mAdapter = new SimpleCursorAdapter(getBaseContext(),
                R.layout.contacts_list_view_layout,
                null,
                new String[] { "photo", "name", "phone", "email" },
                new int[] { R.id.photo, R.id.name, R.id.phone, R.id.email }, 0);*/
        //mContactsList.setAdapter(mAdapter);

        mDoubleAdapter = new DoubleListAdapter(this,
                getString(R.string.favorites), mFavoritesAdapter,
                getString(R.string.all_contacts), mAdapter);
        mContactsList.setAdapter(mDoubleAdapter);

        // TODO
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                Intent intent = new Intent(this, ContactAddNFCActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_search:
                // TODO
                return true;
            case R.id.action_sign_out:
                signOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
            if (!cursor.moveToPosition(offset * LOAD_NUM)) return null;

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

            } while (numLoaded++ <= LOAD_NUM && cursor.moveToNext());

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
            String displayName = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.Data.DISPLAY_NAME));
            String photoUri = cursor.getString(cursor.getColumnIndex(
                    Phone.PHOTO_URI));
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
                        case Phone.TYPE_OTHER:
                            if (phone == null)
                                phone = cursor.getString(cursor.getColumnIndex("data1"));
                            break;
                    }
                }

                // Email.
                if (columnType.equals(Email.CONTENT_ITEM_TYPE)) {
                    switch (cursor.getInt(cursor.getColumnIndex("data2"))) {
                        case Email.TYPE_HOME:
                            email = cursor.getString(cursor.getColumnIndex("data1"));
                            break;
                        case Email.TYPE_WORK:
                            if (email == null)
                                email = cursor.getString(cursor.getColumnIndex("data1"));
                            break;
                        case Email.TYPE_OTHER:
                            if (email == null)
                                email = cursor.getString(cursor.getColumnIndex("data1"));
                            break;
                    }
                }
            } while (cursor.moveToNext());

            // Get default photo if contact photo does not exist.
            if (photoUri == null)
                photoUri = mImgProvider.getDefaultContactUri(displayName);

            // Add contact details to cursor.
            mMatrixCursor.addRow(new Object[]{
                    Long.toString(contactId),
                    photoUri,
                    displayName,
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
        private boolean loading = true;


        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {
            //Log.e("-------", "visible: " + visibleItemCount + "  totla: " + totalItemCount);
            if (loading) {
                if (totalItemCount > previousTotal) {
                    loading = false;
                    previousTotal = totalItemCount;
                    offset++;
                }
            }
            if (!loading && (totalItemCount - visibleItemCount) <= (firstVisibleItem + LOAD_NUM)) {
                return;
//                runOnUiThread(new Runnable() {
//                    public void run() {
//                        new ContactsListLoader(offset).execute();
//                        loading = true;
//                    }
//                });
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) { }
    }
}
