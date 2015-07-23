package com.anjoola.sharewear;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.anjoola.sharewear.db.FavoriteContactContract;
import com.anjoola.sharewear.db.FavoriteContactContract.FavoriteEntry;
import com.anjoola.sharewear.db.FavoriteDbHelper;
import com.anjoola.sharewear.util.ServerConnection;
import com.anjoola.sharewear.util.ServerConnectionCallback;
import com.anjoola.sharewear.util.ServerField;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Activity to display information about a contact.
 */
public class ContactViewActivity extends ShareWearActivity implements
        View.OnClickListener {
    // Represents no location being shared.
    private final static LatLng NO_LOCATION = new LatLng(-1, -1);

    private final static String GOOGLE_MAPS_URI =
            "http://maps.google.com/maps?daddr=";

    // GridLayouts containing possible actions to take on this contact.
    private GridLayout mCall, mEmail, mGetLocation;
    private TextView mTextName, mTextPhone, mTextEmail, mTextLocation;
    private String name, phone, email;
    private LatLng location;

    // Photo.
    private String photoUri;
    private ImageView mPhoto;

    // Dividers to hide if necessary.
    private View divider1, divider2;

    // Whether or not this contact is a favorite.
    private boolean mIsFavorite;
    private Menu mMenu;

    private ShareWearApplication mApp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_view_activity);
        mApp = (ShareWearApplication) getApplication();

        // Get objects.
        mPhoto = (ImageView) findViewById(R.id.contact_photo);
        mCall = (GridLayout) findViewById(R.id.button_call);
        mEmail = (GridLayout) findViewById(R.id.button_email);
        mGetLocation = (GridLayout) findViewById(R.id.button_navigate);
        mTextName = (TextView) findViewById(R.id.contact_name);
        mTextPhone = (TextView) findViewById(R.id.text_phone);
        mTextEmail = (TextView) findViewById(R.id.text_email);
        mTextLocation = (TextView) findViewById(R.id.text_navigate);
        divider1 = findViewById(R.id.divider1);
        divider2 = findViewById(R.id.divider2);

        mCall.setOnClickListener(this);
        mEmail.setOnClickListener(this);
        mGetLocation.setOnClickListener(this);

        // Get contact details.
        Intent intent = getIntent();
        photoUri = intent.getStringExtra(ShareWearActivity.PHOTO);
        name = intent.getStringExtra(ShareWearActivity.NAME);
        phone = intent.getStringExtra(ShareWearActivity.PHONE);
        email = intent.getStringExtra(ShareWearActivity.EMAIL);
    }

    @Override
    public void onClick(View v) {
        // Dial the number.
        if (v.getId() == R.id.button_call) {
            Uri number = Uri.parse("tel:" + phone);
            Intent callIntent = new Intent(Intent.ACTION_DIAL, number);
            startActivity(callIntent);
        }

        // Send an email.
        else if (v.getId() == R.id.button_email) {
            Uri emailUri = Uri.fromParts("mailto", email, null);
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, emailUri);
            startActivity(emailIntent);
        }

        // Start navigation to the contact, or request their current location.
        else if (v.getId() == R.id.button_navigate) {
            // Not a ShareWear user.
            if (location == null)
                return;

            // If they are currently sharing their location, navigate there.
            else if (location != NO_LOCATION) {
                Uri destination = Uri.parse(GOOGLE_MAPS_URI +
                        location.latitude + "," + location.longitude);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, destination);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            }

            // Otherwise, request their current location.
            else {
                requestLocation();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        getMenuInflater().inflate(R.menu.contact_view_menu, menu);

        // Set contact details after menu has been created.
        setContactDetails();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_favorite:
                if (mIsFavorite)
                    favoriteRemove();
                else
                    favoriteAdd();
                return true;
            case R.id.action_sign_out:
                signOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Adds a contact to favorites.
     */
    private void favoriteAdd() {
        SQLiteDatabase db = mApp.mDbHelper.getWritableDatabase();

        // Create key-value map.
        ContentValues values = new ContentValues();
        values.put(FavoriteEntry.COLUMN_NAME_NAME, name);
        values.put(FavoriteEntry.COLUMN_NAME_PHONE, phone);
        values.put(FavoriteEntry.COLUMN_NAME_EMAIL, email);
        values.put(FavoriteEntry.COLUMN_NAME_PHOTO_URI, photoUri);

        db.insert(FavoriteEntry.TABLE_NAME, "NULL", values);

        // Update UI.
        mIsFavorite = true;
        toggleFavoritesDisplay(true);
        Toast.makeText(this, R.string.added_favorite, Toast.LENGTH_SHORT).show();
    }

    /**
     * Checks to see if this contact is a favorite.
     */
    private boolean favoriteCheck() {
        SQLiteDatabase db = mApp.mDbHelper.getReadableDatabase();
        String[] selectionArgs = {
                name,
                email != null ? email : "NULL",
                phone != null ? phone : "NULL"
        };

        // Search database to see if this user exists.
        Cursor cursor = db.query(FavoriteEntry.TABLE_NAME,
                FavoriteDbHelper.PROJECTION, FavoriteDbHelper.SELECTION,
                selectionArgs, null, null,
                FavoriteContactContract.FavoriteEntry.COLUMN_NAME_NAME + " ASC");

        if (cursor.moveToFirst()) {
            cursor.close();
            return true;
        }

        cursor.close();
        return false;
    }

    /**
     * Removes a contact from favorites.
     */
    private void favoriteRemove() {
        SQLiteDatabase db = mApp.mDbHelper.getWritableDatabase();
        String[] selectionArgs = {
                name,
                email != null ? email : "NULL",
                phone != null ? phone : "NULL"
        };

        // Delete from the database.
        db.delete(FavoriteEntry.TABLE_NAME, FavoriteDbHelper.SELECTION,
                selectionArgs);

        // Update UI.
        mIsFavorite = false;
        toggleFavoritesDisplay(false);
        Toast.makeText(this, R.string.removed_favorite, Toast.LENGTH_SHORT).show();
    }

    /**
     * Checks with the server to see if the user is currently sharing their
     * location.
     */
    private void getContactLocation() {
        try {
            String to = email == null || email.length() == 0 ? phone : email;

            JSONObject json = new JSONObject();
            json.put(ServerField.COMMAND, ServerField.LOCATION_GET);
            json.put(ServerField.USER_ID, mApp.prefGetGcmToken());
            json.put(ServerField.USER_TO, to);
            ServerConnection.doPost(json, new ContactLocationCallback());
        }
        catch (Exception e) { }
    }

    /**
     * Request a contact's current location. Sends a notification to them.
     */
    private void requestLocation() {
        try {
            String to = email == null || email.length() == 0 ? phone : email;

            JSONObject json = new JSONObject();
            json.put(ServerField.COMMAND, ServerField.LOCATION_REQUEST);
            json.put(ServerField.USER_ID, mApp.prefGetGcmToken());
            json.put(ServerField.USER_TO, to);
            ServerConnection.doPost(json, new ContactLocationCallback());
        }
        catch (Exception e) { }

        Toast.makeText(this, R.string.location_request_sent, Toast.LENGTH_SHORT).show();
    }

    /**
     * Set the objects in the layout to contain the specific details for this
     * contact.
     */
    private void setContactDetails() {
        mTextName.setText(name);

        // Phone number.
        if (phone != null && phone.length() > 0)
            mTextPhone.setText(phone);
        else {
            mCall.setVisibility(View.GONE);
            divider1.setVisibility(View.GONE);
        }

        // Email.
        if (email != null && email.length() > 0)
            mTextEmail.setText(email);
        else {
            mEmail.setVisibility(View.GONE);
            divider2.setVisibility(View.GONE);
        }

        // Location.
        getContactLocation();

        // Photo.
        if (photoUri != null) {
            Uri uri = Uri.parse(photoUri);
            mPhoto.setImageURI(uri);
        }

        // Is this contact a favorite? If so, change the favorites icon display.
        mIsFavorite = favoriteCheck();
        toggleFavoritesDisplay(mIsFavorite);
    }

    /**
     * Toggle the favorites display (whether or not the star is on).
     *
     * @param on True if the star is on.
     */
    private void toggleFavoritesDisplay(boolean on) {
        MenuItem favoriteItem = mMenu.getItem(0);

        if (on) {
            favoriteItem.setIcon(getDrawable(R.mipmap.ic_star_filled));
            favoriteItem.setTitle(R.string.remove_favorite);
        }
        else {
            favoriteItem.setIcon(getDrawable(R.mipmap.ic_star_blank));
            favoriteItem.setTitle(R.string.add_favorite);
        }
    }

    /**
     * Callback for when the server returns with the user's location.
     */
    class ContactLocationCallback implements ServerConnectionCallback {

        public void callback(JSONObject json) {
            try {
                // Not a ShareWear user.
                if (json == null) {
                    mTextLocation.setText(R.string.location_not_a_user);
                    return;
                }

                double lat = Double.parseDouble(json.get(ServerField.LATITUDE).toString());
                double lng = Double.parseDouble(json.get(ServerField.LONGITUDE).toString());

                // User is not sharing their location.
                if (lat == -1 && lng == -1) {
                    mTextLocation.setText(R.string.location_request);
                    location = NO_LOCATION;
                }

                // User is sharing their location.
                else {
                    mTextLocation.setText(R.string.location_navigate);
                    location = new LatLng(lat, lng);
                }

            } catch (JSONException e) { }
        }
    }
}
