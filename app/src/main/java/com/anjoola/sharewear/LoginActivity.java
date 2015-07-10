package com.anjoola.sharewear;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

/**
 * First activity. Contains code for logging in.
 */
public class LoginActivity extends ShareWearBaseActivity implements
        ConnectionCallbacks, OnConnectionFailedListener,
        View.OnClickListener {

    // Keep track of whether or not the user has signed in yet. Can be one
    // of three values:
    //
    //       STATE_DEFAULT: Default state of application before signing in, or
    //                      after signing out. Will not attempt to resolve sign
    //                      in errors.
    //       STATE_SIGN_IN: User has attempted to sign in. Resolve any errors
    //                      that are preventing sign in.
    //   STATE_IN_PROGRESS: Started an intent to resolve sign in error. Should
    //                      not start further intents until current intent
    //                      is complete.
    private static final int STATE_DEFAULT = 0;
    private static final int STATE_SIGN_IN = 1;
    private static final int STATE_IN_PROGRESS = 2;
    private int mSignInProgress;

    // Stores PendingIntent most recently returned by Google Play Services
    // until user clicks 'Sign In'.
    private PendingIntent mSignInIntent;

    // Stores error code most recently returned by Google Play Services until
    // user clicks 'Sign In'.
    private int mSignInError;

    // Sign in result code.
    private static final int RC_SIGN_IN = 0;

    // Sign in progress state, saved.
    private static final String SAVED_PROGRESS = "sign_in_progress";

    // Application, used for getting the Google API client.
    ShareWearApplication mApp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        // Set font for title.
        TextView title = (TextView) findViewById(R.id.sharewear_title);
        Typeface typeface = Typeface.createFromAsset(getAssets(),
                "fonts/OleoScriptSwashCaps-Regular.ttf");
        title.setTypeface(typeface);

        // Initialize Google API client.
        mApp = (ShareWearApplication) getApplication();
        mApp.googleApiClient = buildGoogleApiClient();

        // Did we come from a sign out? If so, actually sign out.
        Intent intent = getIntent();
        String message = intent.getStringExtra(ShareWearActivity.SIGN_OUT);
        if (message != null) {
            signOut();
        }
        // See if we are already logged in. If so, switch to the main activity.
        else if (prefGetUser() != null) {
            // TODO fix delay here, add loading icon
            toMainActivity(true);
            return;
        }

        // No action bar.
        ActionBar bar = getActionBar();
        if (bar != null) {
            getActionBar().hide();
        }

        // Restore saved instance.
        if (savedInstanceState != null) {
            mSignInProgress = savedInstanceState
                    .getInt(SAVED_PROGRESS, STATE_DEFAULT);
        }

        // Set up sign in button. Change text and add listener.
        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        for (int i = 0; i < signInButton.getChildCount(); i++) {
            View v = signInButton.getChildAt(i);
            if (v instanceof TextView) {
                TextView mTextView = (TextView) v;
                mTextView.setText(R.string.sign_in_button_text);
                break;
            }
        }
        signInButton.setOnClickListener(this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVED_PROGRESS, mSignInProgress);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        switch (requestCode) {
            case RC_SIGN_IN:
                // Error resolution successful and we should continue processing
                // errors.
                if (resultCode == RESULT_OK) {
                    mSignInProgress = STATE_SIGN_IN;
                }
                // Unsuccessful error resolution, or user cancelled. Stop
                // processing errors.
                else {
                    mSignInProgress = STATE_DEFAULT;
                }

                // Google Play Services resolved issue. Re-attempt connection.
                if (!mApp.googleApiClient.isConnecting()) {
                    mApp.googleApiClient.connect();
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.sign_in_button)
            signIn();
    }

    @Override
    // User just signed in and successfully connected to Google Play Services.
    public void onConnected(Bundle connectionHint) {
        mSignInProgress = STATE_DEFAULT;
        Person currentUser = Plus.PeopleApi.getCurrentPerson(mApp.googleApiClient);

        // Save user details for next startup.
        prefSetUser(currentUser.getId());
        String details = ContactDetails.getMyContactDetails(mApp);
        prefSetContactDetails(details);
        toMainActivity(false);
    }

    @Override
    // Could not connect to Google Play Services.
    public void onConnectionFailed(ConnectionResult result) {
        // Could not get the API requested. Device might not support requested
        // API or may not have the required component installed.
        if (result.getErrorCode() == ConnectionResult.API_UNAVAILABLE) {
            System.exit(1);
        }
        // No intent in progress. Store the latest error resolution intent to
        // use when signing in again.
        else if (mSignInProgress != STATE_IN_PROGRESS) {
            mSignInIntent = result.getResolution();
            mSignInError = result.getErrorCode();

            // User already tried signing in, so continue processing errors
            // until user is signed in.
            if (mSignInProgress == STATE_SIGN_IN)
                resolveSignInError();
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // Lost connection. Attempt to re-establish connection.
        mApp.googleApiClient.connect();
    }

    /**
     * Specify where we connect, connection failure callbacks, and Google
     * APIs used. Currently we use the following APIs:
     *   - Google+ sign in
     *
     * @return Reference to the Google API client.
     */
    private GoogleApiClient buildGoogleApiClient() {
        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API, Plus.PlusOptions.builder().build())
                .addScope(Plus.SCOPE_PLUS_LOGIN);

        return builder.build();
    }

    /**
     * Creates an error dialog to resolve sign-in issues.
     *
     * @return Dialog to display to user.
     */
    private Dialog createErrorDialog() {
        if (GooglePlayServicesUtil.isUserRecoverableError(mSignInError)) {
            return GooglePlayServicesUtil.getErrorDialog(
                    mSignInError,
                    this,
                    RC_SIGN_IN,
                    new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            mSignInProgress = STATE_DEFAULT;
                        }
                    });
        }

        return new AlertDialog.Builder(this)
                .setMessage(R.string.play_services_error)
                .setPositiveButton(R.string.close,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int w) {
                                mSignInProgress = STATE_DEFAULT;
                            }
                        }).create();
    }

    /**
     * Starts an intent to resolve current error preventing user from signing
     * in. Could result in a dialog having the user select an account, an
     * activity to have the using consent to the requested permissions, etc.
     */
    private void resolveSignInError() {
        // We have an intent to let the user sign in or resolve the error.
        if (mSignInIntent != null) {
            // Send pending intent stored in most recent onConnectionFailed
            // callback.
            try {
                mSignInProgress = STATE_IN_PROGRESS;
                startIntentSenderForResult(mSignInIntent.getIntentSender(),
                        RC_SIGN_IN, null, 0, 0, 0);
            }
            // Intent cancelled before it was sent.
            catch (SendIntentException e) {
                mSignInProgress = STATE_SIGN_IN;
                mApp.googleApiClient.connect();
            }
        }
        // Google Play Services could not provide an intent for other errors.
        // Show default Google Play Services error dialog.
        else {
            createErrorDialog().show();
        }
    }

    /**
     * Sign in.
     */
    private void signIn() {
        // Only process if not transitioning between connection state for the
        // GoogleApiClient.
        if (mApp.googleApiClient.isConnecting()) {
            return;
        }

        mSignInProgress = STATE_SIGN_IN;
        mApp.googleApiClient.connect();
    }

    /**
     *  Sign out.
     */
    private void signOut() {
        prefSetUser(null);

        if (mApp.googleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mApp.googleApiClient);
            mApp.googleApiClient.disconnect();
        }
    }

    /**
     * Switch to main activity if signed in.
     *
     * @param quickly Whether or not to do this without a transition.
     */
    private void toMainActivity(boolean quickly) {
        Intent intent = new Intent(this, WelcomeActivity.class);
        startActivity(intent);
//        Intent intent = new Intent(this, ContactsListActivity.class);
//
//        if (quickly) {
//            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
//            startActivityForResult(intent, 0);
//            overridePendingTransition(0, 0);
//        }
//        else {
//            startActivity(intent);
//        }
    }
}
