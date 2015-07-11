package com.anjoola.sharewear;

import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.SimpleCursorAdapter;

import com.anjoola.sharewear.util.ContactsImageProvider;

public class ContactsFavoriteFragment extends Fragment implements
        AdapterView.OnItemClickListener {
    private View mNoFavoritesView;

    // Adapter for mapping contacts to objects in the ListViews.
    private SimpleCursorAdapter mAdapter;

    // Asynchronous task to retrieve and load ListView with contacts.
    private MatrixCursor mMatrixCursor;

    // For getting images for contacts.
    private ContactsImageProvider mImgProvider;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.contacts_favorite_fragment, container, false);

        // TODO hide this if the user does have favorites
        mNoFavoritesView = v.findViewById(R.id.no_favorites_view);
        mImgProvider = new ContactsImageProvider(getActivity().getBaseContext());

        // Set up contacts list view and adapter.
        GridView contactsList = (GridView) v.findViewById(R.id.contacts_list);
        contactsList.setOnItemClickListener(this);

        mAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.contacts_list_view_favorite,
                null,
                new String[] { "photo", "name" },
                new int[] { R.id.photo, R.id.name }, 0);
        contactsList.setAdapter(mAdapter);

        // Cursor for loading all details.
        mMatrixCursor = new MatrixCursor(
                new String[] {"_id", "photo", "name", "phone", "email"});
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                new ContactsListLoader().execute();
            }
        });

        return v;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MatrixCursor entry = (MatrixCursor) parent.getAdapter().getItem(position);

        // Send contact details over to activity.
        Intent intent = new Intent(getActivity(), ContactViewActivity.class);
        intent.putExtra(ShareWearActivity.PHOTO, entry.getString(1));
        intent.putExtra(ShareWearActivity.NAME, entry.getString(2));
        intent.putExtra(ShareWearActivity.PHONE, entry.getString(3));
        intent.putExtra(ShareWearActivity.EMAIL, entry.getString(4));
        startActivity(intent);
    }

    /**
     * Asynchronous class to retrieve and load ListView with contacts.
     */
    private class ContactsListLoader extends AsyncTask<Void, Void, Cursor> {
        // TODO get correct URI that stores the favorites.
        // URIs for retrieving contacts information.
        private final Uri CONTACTS_URI = ContactsContract.Contacts.CONTENT_URI;
        private final Uri DATA_URI = ContactsContract.Data.CONTENT_URI;

        @Override
        protected Cursor doInBackground(Void... params) {
            // TODO
            Cursor cursor = getActivity().getContentResolver().query(CONTACTS_URI, null, null,
                    null, ContactsContract.Contacts.DISPLAY_NAME + " ASC ");

            if (!cursor.moveToFirst())
                return null;

            // Loop through each favorite to display it.
            int numLoaded = 0;
            do {
                long contactId = cursor.getLong(cursor.getColumnIndex("_ID"));

                // Retrieve individual fields for this contact.
                Cursor dCursor = getActivity().getContentResolver().query(DATA_URI, null,
                        ContactsContract.Data.CONTACT_ID + "=" + contactId,
                        null, null);

                // No data, move on.
                if (!dCursor.moveToFirst()) continue;

                getContactDetails(dCursor, contactId);

                // TODO
            } while (++numLoaded < 10 && cursor.moveToNext());

            cursor.close();
            return mMatrixCursor;
        }

        @Override
        protected void onPostExecute(Cursor result) {
            mAdapter.swapCursor(result);
            mAdapter.notifyDataSetChanged();
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
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI));
            String name = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.Contacts.DISPLAY_NAME));
            String phone = null;
            String email = null;

            // Loop since there might be multiple entries.
            do {
                String columnType = cursor.getString(cursor.getColumnIndex("mimetype"));

                // Phone numbers.
                if (columnType.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
                    switch (cursor.getInt(cursor.getColumnIndex("data2"))) {
                        case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                            phone = cursor.getString(cursor.getColumnIndex("data1"));
                            break;
                        case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                            if (phone == null)
                                phone = cursor.getString(cursor.getColumnIndex("data1"));
                            break;
                        case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                            if (phone == null)
                                phone = cursor.getString(cursor.getColumnIndex("data1"));
                            break;
                        case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER:
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
}