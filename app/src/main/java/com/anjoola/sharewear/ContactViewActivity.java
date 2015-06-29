package com.anjoola.sharewear;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridLayout;

/**
 * Activity to display information about a contact.
 */
public class ContactViewActivity extends ShareWearActivity implements
        View.OnClickListener {
    // GridLayouts containing possible actions to take on this contact.
    GridLayout mCall, mEmail, mGetLocation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_view_activity);

        // Get objects.
        mCall = (GridLayout) findViewById(R.id.button_call);
        mEmail = (GridLayout) findViewById(R.id.button_email);
        mGetLocation = (GridLayout) findViewById(R.id.button_navigate);
        mCall.setOnClickListener(this);
        mEmail.setOnClickListener(this);
        mGetLocation.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        // Dial the number.
        if (v.getId() == R.id.button_call) {
            Uri number = Uri.parse("tel:" + "123456"); // TODO
            Intent callIntent = new Intent(Intent.ACTION_DIAL, number);
            startActivity(callIntent);
        }
        // Send an email.
        else if (v.getId() == R.id.button_email) {
            Uri email = Uri.fromParts("mailto", "abc@gmail.com", null); // TODO
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, email);
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
}
