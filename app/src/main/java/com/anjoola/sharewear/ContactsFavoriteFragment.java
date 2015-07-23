package com.anjoola.sharewear;

import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
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
import android.widget.SimpleCursorAdapter;

import com.anjoola.sharewear.db.FavoriteContactContract.FavoriteEntry;
import com.anjoola.sharewear.db.FavoriteDbHelper;
import com.anjoola.sharewear.util.ContactsImageProvider;

public class ContactsFavoriteFragment extends Fragment implements
        AdapterView.OnItemClickListener {
    private ShareWearApplication mApp;
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
        mApp = (ShareWearApplication) getActivity().getApplication();
        mImgProvider = new ContactsImageProvider(getActivity().getBaseContext());
        mNoFavoritesView = v.findViewById(R.id.no_favorites_view);

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
                new String[] { "_id", "photo", "name", "phone", "email"});
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

        private int favoritesFound = 0;

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
                long id = cursor.getLong(cursor.getColumnIndex(FavoriteEntry._ID));
                String name = cursor.getString(cursor.getColumnIndex(
                        FavoriteEntry.COLUMN_NAME_NAME));
                String phone = cursor.getString(cursor.getColumnIndex(
                        FavoriteEntry.COLUMN_NAME_PHONE));
                String email = cursor.getString(cursor.getColumnIndex(
                        FavoriteEntry.COLUMN_NAME_EMAIL));
                String photoUri = cursor.getString(cursor.getColumnIndex(
                        FavoriteEntry.COLUMN_NAME_PHOTO_URI));

//                String photoUri = mImgProvider.getDefaultContactUri(name); // TODO default

                // Get default photo if contact photo does not exist.
//                if (photoUri == null)
//                    photoUri = mImgProvider.getDefaultContactUri(name);

                // Add contact details to cursor.
                mMatrixCursor.addRow(new Object[] {
                        Long.toString(id),
                        photoUri,
                        name,
                        phone,
                        email
                });
                favoritesFound++;
            } while (cursor.moveToNext());

            cursor.close();
            return mMatrixCursor;
        }

        @Override
        protected void onPostExecute(Cursor result) {
            mAdapter.swapCursor(result);
            mAdapter.notifyDataSetChanged();

            // Favorites found. Hide the "no favorites" view.
            if (favoritesFound > 0) {
                mNoFavoritesView.setVisibility(View.GONE);
            }
        }
    }
}
