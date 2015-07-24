package com.anjoola.sharewear;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;

import com.anjoola.sharewear.util.ContactDetails;
import com.anjoola.sharewear.util.RegistrationIntentService;

/**
 * Welcome screen. Has the user edit their local profile if it isn't completely
 * set up yet.
 */
public class WelcomeActivity extends ShareWearActivity implements
        View.OnClickListener {
    // Reference to the ShareWear application.
    private ShareWearApplication mApp;

    // Whether or not the button for editing the local profile has been clicked.
    private boolean buttonClicked;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_activity);

        mApp = (ShareWearApplication) getApplication();
        Button profileButton = (Button) findViewById(R.id.profile_setup_button);
        profileButton.setOnClickListener(this);
        buttonClicked = false;

        // No action bar.
        ActionBar bar = getActionBar();
        if (bar != null) {
            getActionBar().hide();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        checkProfileComplete();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkProfileComplete();
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

    /**
     * Check to see if the user has finished filling out their profile. If so,
     * move on to the contacts listing.
     */
    private void checkProfileComplete() {
        String details = ContactDetails.getMyContactDetailsStrict(this);
        if (details != null &&
            (buttonClicked || mApp.prefGetContactDetails() != null)) {
            mApp.prefSetContactDetails(details);
            ((ShareWearApplication) getApplication()).myDetails = details;

            // Register this user with the cloud server.
            Intent service = new Intent(this, RegistrationIntentService.class);
            startService(service);

            // Show contacts listing.
            Intent intent = new Intent(this, ContactsListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK |
                    Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
}
