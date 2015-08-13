package com.anjoola.sharewear;

import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.anjoola.sharewear.util.ContactLoadCallback;
import com.anjoola.sharewear.util.ContactsListLoader;
import com.anjoola.sharewear.util.RegistrationIntentService;

/**
 * Main activity. Loading screen.
 */
public class MainActivity extends ShareWearActivity {
    // Number of contacts to preload before showing main app.
    public final static int NUM_CONTACTS_PRELOAD = 50;

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
    }

    @Override
    public void onStart() {
        super.onStart();

        // Possibly load contacts in the background.
        if (!mApp.mContactListPreloaded) {
            new Thread(new Runnable() {
                public void run() {
                    toNextActivity();
                }
            }).start();
        }
        mApp.mContactListPreloaded = true;
    }

    /**
     * Go to the next activity (either the welcome screen or the contacts
     * listing).
     */
    private void toNextActivity() {
        // User has already set up their profile. Preload contacts and move to
        // the contacts listing.
        if (mApp.prefGetContactDetails() != null) {
            // Registration token was not sent to the server yet.
            if (mApp.prefGetGcmToken() == null) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        new RegistrationIntentService.RegisterAsync(
                                MainActivity.this, getApplication())
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                });
            }

            ContactsListLoader.loadContacts(this, 0, NUM_CONTACTS_PRELOAD,
                    new ProgressIncrement());

            Intent intent = new Intent(this, ContactsListActivity.class);
            startActivityForResult(intent, 0);
        }

        // User has not set up their profile yet.
        else {
            Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
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
