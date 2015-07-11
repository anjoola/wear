package com.anjoola.sharewear;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;

import com.anjoola.sharewear.util.ContactDetails;

/**
 * Welcome screen. Has the user edit their local profile if it isn't completely
 * set up yet.
 */
public class WelcomeActivity extends ShareWearActivity implements
        View.OnClickListener {
    ShareWearApplication mApp;

    // Button for editing the local profile.
    Button mProfileButton;
    boolean buttonClicked;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_activity);

        mProfileButton = (Button) findViewById(R.id.profile_setup_button);
        mProfileButton.setOnClickListener(this);
        buttonClicked = false;

        mApp = (ShareWearApplication) getApplication();

        // No action bar.
        ActionBar bar = getActionBar();
        if (bar != null) {
            getActionBar().hide();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // See if the profile setup is done. If so, move on to show the contacts
        // listing.
        String details = ContactDetails.getMyContactDetailsStrict(this);
        if (details != null && (buttonClicked || prefGetContactDetails() != null)) {
            prefSetContactDetails(details);
            Intent intent = new Intent(this, ContactsListActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onClick(View v) {
        // Start intent to edit local profile.
        if (v.getId() == R.id.profile_setup_button) {
            buttonClicked = true;
            Intent intent = new Intent(
                    Intent.ACTION_VIEW, ContactsContract.Profile.CONTENT_URI);
            startActivity(intent);
        }
    }
}
