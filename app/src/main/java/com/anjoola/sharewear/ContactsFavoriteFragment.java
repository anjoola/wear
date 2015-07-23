package com.anjoola.sharewear;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.anjoola.sharewear.db.FavoriteContactContract.FavoriteEntry;
import com.anjoola.sharewear.db.FavoriteDbHelper;
import com.anjoola.sharewear.util.ContactDetails;
import com.anjoola.sharewear.util.ContactsFavoriteAdapter;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Fragment for showing favorite contacts.
 */
public class ContactsFavoriteFragment extends Fragment implements
        AdapterView.OnItemClickListener {
    private ShareWearApplication mApp;
    private View mNoFavoritesView;

    // Adapter for mapping contacts to objects in the ListViews.
    private ContactsFavoriteAdapter mAdapter;

    // List of all contacts.
    private ArrayList<ContactDetails> mContactsList;

    // Number of favorites.
    private int numFavorites;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.contacts_favorite_fragment,
                container, false);
        mApp = (ShareWearApplication) getActivity().getApplication();
        mNoFavoritesView = v.findViewById(R.id.no_favorites_view);
        mNoFavoritesView.setVisibility(View.GONE);

        // Set up contacts list view and adapter.
        GridView contactsList = (GridView) v.findViewById(R.id.contacts_list);
        contactsList.setOnItemClickListener(this);

        mContactsList = new ArrayList<ContactDetails>();
        mAdapter = new ContactsFavoriteAdapter(getActivity(), mContactsList);
        contactsList.setAdapter(mAdapter);

        // Load the favorites.
        numFavorites = 0;
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                new ContactsListLoader().execute();
            }
        });

        return v;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
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

        @Override
        protected Cursor doInBackground(Void... params) {
            SQLiteDatabase db = mApp.mDbHelper.getReadableDatabase();
            Cursor cursor = db.query(FavoriteEntry.TABLE_NAME,
                    FavoriteDbHelper.PROJECTION, null, null, null, null,
                    FavoriteEntry.COLUMN_NAME_NAME + " ASC");

            if (!cursor.moveToFirst())
                return null;

            // Loop through each favorite to display it.
            do {
                String name = cursor.getString(cursor.getColumnIndex(
                        FavoriteEntry.COLUMN_NAME_NAME));
                String phone = cursor.getString(cursor.getColumnIndex(
                        FavoriteEntry.COLUMN_NAME_PHONE));
                String email = cursor.getString(cursor.getColumnIndex(
                        FavoriteEntry.COLUMN_NAME_EMAIL));
                String photoUri = cursor.getString(cursor.getColumnIndex(
                        FavoriteEntry.COLUMN_NAME_PHOTO_URI));

                numFavorites++;
                mContactsList.add(new ContactDetails(name, phone, email, photoUri));
            } while (cursor.moveToNext());

            cursor.close();
            return cursor;
        }

        @Override
        protected void onPostExecute(Cursor result) {
            mAdapter.notifyDataSetChanged();
            updateFavoritesDisplay();
        }
    }

    /**
     * Adds a new favorite.
     * @param contact The new favorite contact.
     */
    public void addNewContact(ContactDetails contact) {
        final ContactDetails details = contact;
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                mContactsList.add(details);
                Collections.sort(mContactsList);
                mAdapter.notifyDataSetChanged();
            }
        });
        numFavorites++;
        updateFavoritesDisplay();
    }

    /**
     * Removes a favorited contact with the given name, phone, and email.
     * @param name The contact's name.
     * @param phone The contacts's phone.
     * @param email THe contact's email.
     */
    public void removeContact(String name, String phone, String email) {
        ContactDetails target = null;
        for (ContactDetails contact : mContactsList) {
            if (contact.name.toLowerCase().equals(name.toLowerCase()) &&
                ((contact.phone == null || contact.phone.equals(phone.toLowerCase())) ||
                (contact.email == null || contact.email.equals(email.toLowerCase())))) {
                target = contact;
                break;
            }
        }

        final ContactDetails targetContact = target;
        if (target != null) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    mContactsList.remove(targetContact);
                    mAdapter.notifyDataSetChanged();
                }
            });
        }
        numFavorites--;
        updateFavoritesDisplay();
    }

    /**
     * Update the favorites display depending on how many favorites there are.
     */
    private void updateFavoritesDisplay() {
        if (numFavorites == 0)
            mNoFavoritesView.setVisibility(View.VISIBLE);
        else
            mNoFavoritesView.setVisibility(View.GONE);
    }
}
