package com.anjoola.sharewear;

import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.anjoola.sharewear.util.ContactLoadCallback;
import com.anjoola.sharewear.util.ContactsListLoader;

/**
 * Main activity. Loading screen.
 */
public class LoginActivity extends ShareWearActivity {
    // Number of contacts to preload before showing main app.
    public final static int NUM_CONTACTS_PRELOAD = 100;

    private ShareWearApplication mApp;

    private ProgressBar mProgress;
    private double progressAmount = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        // Set font for title.
        TextView title = (TextView) findViewById(R.id.sharewear_title);
        Typeface typeface = Typeface.createFromAsset(getAssets(),
                "fonts/OleoScriptSwashCaps-Regular.ttf");
        title.setTypeface(typeface);

        // No action bar.
        ActionBar bar = getActionBar();
        if (bar != null) {
            getActionBar().hide();
        }

        mApp = (ShareWearApplication) getApplication();
        mProgress = (ProgressBar) findViewById(R.id.progress_bar);

        // Possibly load contacts in the background.
        new Thread(new Runnable() {
            public void run() {
                toNextActivity();
            }
        }).start();
    }

    /**
     * Go to the next activity (either the welcome screen or the contacts
     * listing).
     */
    private void toNextActivity() {
        Intent intent;

        // User has already set up their profile. Preload contacts and move to
        // the contacts listing.
        if (mApp.prefGetContactDetails() != null) {
            ContactsListLoader.loadContacts(mApp, this, 0, NUM_CONTACTS_PRELOAD,
                    new ProgressIncrement());

            intent = new Intent(this, ContactsListActivity.class);
            startActivityForResult(intent, 0);
        }

        // User has not set up their profile yet.
        else {
            intent = new Intent(this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivityForResult(intent, 0);
            overridePendingTransition(0, 0);
        }
    }

    /**
     * Callback each time another contact is loaded. Add to the progress bar.
     */
    class ProgressIncrement implements ContactLoadCallback {
        public void callback() {
            progressAmount += 100.0 / NUM_CONTACTS_PRELOAD;
            mProgress.setProgress((int) Math.floor(progressAmount));
        }
    }
}
