package com.anjoola.sharewear;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_view_activity);

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

        // Get and set contact details.
        Intent intent = getIntent();
        photoUri = intent.getStringExtra(ShareWearActivity.PHOTO);
        name = intent.getStringExtra(ShareWearActivity.NAME);
        phone = intent.getStringExtra(ShareWearActivity.PHONE);
        email = intent.getStringExtra(ShareWearActivity.EMAIL);
        setContactDetails();
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
        getMenuInflater().inflate(R.menu.contact_view_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_favorite:
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
     * Checks with the server to see if the user is currently sharing their
     * location.
     */
    private void getContactLocation() {
        ShareWearApplication app = (ShareWearApplication) getApplication();
        try {
            JSONObject json = new JSONObject();
            json.put(ServerField.COMMAND, ServerField.LOCATION_GET);
            json.put(ServerField.USER_ID, app.getGcmToken());
            json.put(ServerField.USER_TO, email); // TODO their email??
            ServerConnection.doPost(json, new ContactLocationCallback());
        }
        catch (Exception e) { }
    }

    /**
     * Request a contact's current location. Sends a notification to them.
     */
    private void requestLocation() {
        ShareWearApplication app = (ShareWearApplication) getApplication();
        try {
            JSONObject json = new JSONObject();
            json.put(ServerField.COMMAND, ServerField.LOCATION_REQUEST);
            json.put(ServerField.USER_ID, app.getGcmToken());
            json.put(ServerField.USER_TO, email); // TODO their email??
            ServerConnection.doPost(json, new ContactLocationCallback());
        }
        catch (Exception e) { }
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
    }

    /**
     * Callback for when the server returns with the user's location.
     */
    class ContactLocationCallback implements ServerConnectionCallback {

        public void callback(JSONObject json) {
            try {
                // TODO convert from JSON to LatLng..
                // Not a ShareWear user.
                if (json == null)
                    mTextLocation.setText(R.string.location_not_a_user);

                    // User is not sharing their location.
                else if (true/* TODO */) {
                    mTextLocation.setText(R.string.location_request);
                    location = NO_LOCATION;
                }

                // User is sharing their location.
                else {
                    mTextLocation.setText(R.string.location_navigate);

                    double lat = Double.parseDouble(json.get(ServerField.LATITUDE).toString());
                    double lng = Double.parseDouble(json.get(ServerField.LONGITUDE).toString());
                    location = new LatLng(lat, lng);
                }
            } catch (JSONException e) { }
        }
    }
}
