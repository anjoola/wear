package com.anjoola.sharewear;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import java.io.File;
import java.io.FileOutputStream;

public class ContactsListActivity extends ShareWearActivity {
    // ListView to store all contacts.
    private ListView mContactsList;

    // Adapter for mapping columns from cursor to ListView.
    private SimpleCursorAdapter mAdapter;

    // Asynchronous task to retrieve and load ListView with contacts.
    private ContactsListLoader mLoader;
    private MatrixCursor mMatrixCursor;

    // Number of contacts to load at once.
    public static int LOAD_NUM = 20;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contacts_list_activity);

        // Set up contacts list view. Set adapter and scroll listener for
        // infinite scrolling.
        mContactsList = (ListView) findViewById(R.id.contacts_list);
        mContactsList.setOnScrollListener(new EndlessScrollListener());
        mAdapter = new SimpleCursorAdapter(getBaseContext(),
                R.layout.contacts_list_view_layout,
                null,
                new String[] { "photo", "name", "phone", "email" },
                new int[] { R.id.photo, R.id.name, R.id.phone, R.id.email }, 0);
        mContactsList.setAdapter(mAdapter);

        mMatrixCursor = new MatrixCursor(
                new String[] {"_id", "photo", "name", "phone", "email"});

        // Set up asynchronous task and start it.
        mLoader = new ContactsListLoader(0);
        mLoader.execute();
    }

    /** An AsyncTask class to retrieve and load listview with contacts */
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

            int numLoaded = 0;
            do {
                long contactId = cursor.getLong(cursor.getColumnIndex("_ID"));

                // Retrieve individual items for this contact.
                Cursor dCursor = getContentResolver().query(DATA_URI, null,
                        ContactsContract.Data.CONTACT_ID + "=" + contactId,
                        null, null);

                // No data, move on.
                if (!dCursor.moveToFirst()) continue;

                // Details to retrieve.
                String displayName = dCursor.getString(dCursor.getColumnIndex(
                        ContactsContract.Data.DISPLAY_NAME));
                String photoPath = "";
                String phone = null;
                String email = null;

                // Loop since there might be multiple entries.
                do {
                    String columnType = dCursor.getString(dCursor.getColumnIndex("mimetype"));

                    // Photo.
                    if (columnType.equals(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)){
                        byte[] photoByte = dCursor.getBlob(dCursor.getColumnIndex("data15"));

                        if (photoByte == null) continue;

                        Bitmap bitmap = BitmapFactory.decodeByteArray(photoByte, 0, photoByte.length);

                        // Create temporary file to store contact image.
                        File cacheDirectory = getBaseContext().getCacheDir();
                        File tmpFile = new File(cacheDirectory.getPath() + "/wpta_"+contactId+".png");
                        try {
                            FileOutputStream fOutStream = new FileOutputStream(tmpFile);

                            // Writing the bitmap to the temporary file as png file
                            bitmap.compress(Bitmap.CompressFormat.PNG,100, fOutStream);

                            fOutStream.flush();
                            fOutStream.close();

                        } catch (Exception e) { }
                        photoPath = tmpFile.getPath();
                    }

                    // Phone numbers.
                    if (columnType.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
                        switch (dCursor.getInt(dCursor.getColumnIndex("data2"))) {
                            case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                                phone = dCursor.getString(dCursor.getColumnIndex("data1"));
                                break;
                            case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                                // Only show work phone if no mobile phone.
                                if (phone == null)
                                    phone = dCursor.getString(dCursor.getColumnIndex("data1"));
                                break;
                        }
                    }

                    // Email.
                    if (columnType.equals(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)) {
                        switch (dCursor.getInt(dCursor.getColumnIndex("data2"))) {
                            case ContactsContract.CommonDataKinds.Email.TYPE_HOME:
                                email = dCursor.getString(dCursor.getColumnIndex("data1"));
                                break;
                            case ContactsContract.CommonDataKinds.Email.TYPE_WORK:
                                // Only show work email if no home email.
                                if (email == null)
                                    email = dCursor.getString(dCursor.getColumnIndex("data1"));
                                break;
                        }
                    }
                } while (dCursor.moveToNext());

                // Add contact details to cursor.
                mMatrixCursor.addRow(new Object[] {
                        Long.toString(contactId),
                        photoPath,
                        displayName,
                        phone,
                        email
                });

            } while (numLoaded++ <= LOAD_NUM && cursor.moveToNext());

            return mMatrixCursor;
        }

        @Override
        protected void onPostExecute(Cursor result) {
            // Setting the cursor containing contacts to listview
            mAdapter.swapCursor(result);
        }
    }

    private class EndlessScrollListener implements AbsListView.OnScrollListener {
        // Offset loaded.
        private int offset = 0;

        private int previousTotal = 0;
        private boolean loading = true;


        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {
            Log.e("-------", "visible: " + visibleItemCount + "  totla: " + totalItemCount);
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
