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

/**
 * Activity to display information about a contact.
 */
public class ContactViewActivity extends ShareWearActivity implements
        View.OnClickListener {
    // GridLayouts containing possible actions to take on this contact.
    GridLayout mCall, mEmail, mGetLocation;
    TextView mTextName, mTextPhone, mTextEmail, mTextLocation;
    String name, phone, email;

    // Photo.
    String photoUri;
    ImageView mPhoto;

    // Dividers to hide if necessary.
    View divider1, divider2;

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

        // Get contact details.
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
            // If they are currently sharing their location, navigate there.
            Uri destination = Uri.parse("google.navigation:q=Taronga+Zoo,+Sydney+Australia"); // TODO
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, destination);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);

            // Otherwise, request their current location.
            // TODO
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
        // TODO

        // Photo.
        if (photoUri != null) {
            Uri uri = Uri.parse(photoUri);
            mPhoto.setImageURI(uri);
        }
    }
}
