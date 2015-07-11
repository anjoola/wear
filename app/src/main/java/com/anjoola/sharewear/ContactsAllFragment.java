package com.anjoola.sharewear;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.anjoola.sharewear.util.ContactDetails;
import com.anjoola.sharewear.util.ContactsImageProvider;
import com.anjoola.sharewear.util.ContactsListAdapter;

import java.util.ArrayList;

public class ContactsAllFragment extends Fragment implements
        AdapterView.OnItemClickListener {
    // Adapter for mapping contacts to objects in the ListViews.
    public ContactsListAdapter mAdapter;

    // List of all contacts.
    private ArrayList<ContactDetails> mContactsList;

    // Number of contacts to load at once.
    public static int LOAD_NUM = 20;

    // For getting images for contacts.
    private ContactsImageProvider mImgProvider;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.contacts_all_fragment, container, false);

        mImgProvider = new ContactsImageProvider(getActivity().getBaseContext());

        // Set up contacts list view and adapter.
        ListView contactsList = (ListView) v.findViewById(R.id.contacts_list_all);
        contactsList.setOnItemClickListener(this);

        mContactsList = new ArrayList<ContactDetails>();
        mAdapter = new ContactsListAdapter(getActivity(), mContactsList);
        contactsList.setAdapter(mAdapter);

        // Load all of the contacts.
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                new ContactsListLoader(0, -1).execute();
            }
        });

        //contactsList.setFastScrollEnabled(true);
        // contactsList.setTextFilterEnabled(true);
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
    private class ContactsListLoader extends AsyncTask<Void, Void, Cursor> {
        // URIs for retrieving contacts information.
        private final Uri CONTACTS_URI = ContactsContract.Contacts.CONTENT_URI;
        private final Uri DATA_URI = ContactsContract.Data.CONTENT_URI;

        // Cursor for retrieving all contacts.
        private int offset;

        // The number of contacts to load. -1 for all of them.
        private int numLoad;

        public ContactsListLoader(int offset, int numLoad) {
            this.offset = offset;
            this.numLoad = numLoad;
        }

        @Override
        protected Cursor doInBackground(Void... params) {
            Cursor cursor = getActivity().getContentResolver().query(
                    CONTACTS_URI, null, null, null,
                    ContactsContract.Contacts.DISPLAY_NAME + " ASC ");

            // Move to the specified offset, if it exists.
            if (!cursor.moveToPosition(offset))
                return null;

            // Loop through every contact.
            int numLoaded = 0;
            do {
                long contactId = cursor.getLong(cursor.getColumnIndex("_ID"));

                // Retrieve individual fields for this contact.
                Cursor dCursor = getActivity().getContentResolver().query(
                        DATA_URI, null, Data.CONTACT_ID + "=" + contactId,
                        null, null);

                // No data, move on.
                if (!dCursor.moveToFirst()) {
                    dCursor.close();
                    continue;
                }

                getContactDetails(dCursor);
                dCursor.close();

            } while ((numLoad == -1 || ++numLoaded < numLoad) && cursor.moveToNext());

            cursor.close();
            return cursor;
        }

        @Override
        protected void onPostExecute(Cursor result) {
            mAdapter.notifyDataSetChanged();
        }

        /**
         * Get details for the contact at the cursor. Add to the list of all
         * contacts.
         *
         * @param cursor The cursor.
         */
        private void getContactDetails(Cursor cursor) {
            // Details to retrieve.
            String photoUri =
                    cursor.getString(cursor.getColumnIndex(Phone.PHOTO_URI));
            String name = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.Contacts.DISPLAY_NAME));
            String phone = null, email = null;

            // Loop since there might be multiple entries.
            do {
                String columnType =
                        cursor.getString(cursor.getColumnIndex("mimetype"));
                int colIndex = cursor.getColumnIndex("data1");

                // Phone numbers.
                if (columnType.equals(Phone.CONTENT_ITEM_TYPE)) {
                    switch (cursor.getInt(cursor.getColumnIndex("data2"))) {
                        case Phone.TYPE_MOBILE:
                            phone = cursor.getString(colIndex);
                            break;
                        case Phone.TYPE_WORK:
                            if (phone == null)
                                phone = cursor.getString(colIndex);
                            break;
                        case Phone.TYPE_HOME:
                            if (phone == null)
                                phone = cursor.getString(colIndex);
                            break;
                        case Phone.TYPE_OTHER:
                            if (phone == null)
                                phone = cursor.getString(colIndex);
                            break;
                    }
                }

                // Email.
                if (columnType.equals(Email.CONTENT_ITEM_TYPE)) {
                    switch (cursor.getInt(cursor.getColumnIndex("data2"))) {
                        case Email.TYPE_HOME:
                            email = cursor.getString(colIndex);
                            break;
                        case Email.TYPE_WORK:
                            if (email == null)
                                email = cursor.getString(colIndex);
                            break;
                        case Email.TYPE_OTHER:
                            if (email == null)
                                email = cursor.getString(colIndex);
                            break;
                    }
                }
            } while (cursor.moveToNext());

            // If contact has neither a phone nor email, don't show them.
            if (phone == null && email == null) return;

            // Get default photo if contact photo does not exist.
            if (photoUri == null)
                photoUri = mImgProvider.getDefaultContactUri(name);

            mContactsList.add(new ContactDetails(name, phone, email, photoUri));
        }
    }
}
