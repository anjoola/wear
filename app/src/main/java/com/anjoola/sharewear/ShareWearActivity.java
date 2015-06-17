package com.anjoola.sharewear;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.plus.Plus;

/**
 * Base activity. Contains the code needed for the action bar and signing
 * in and out.
 */
public class ShareWearActivity extends Activity implements
        ConnectionCallbacks, OnConnectionFailedListener {

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

    // Used to connect to Google Play Services and allow Google+ sign in.
    protected GoogleApiClient mGoogleApiClient;

    // Reference to the action bar menu.
    private Menu mActionBarMenu;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Restore saved instance.
        if (savedInstanceState != null) {
            mSignInProgress = savedInstanceState
                    .getInt(SAVED_PROGRESS, STATE_DEFAULT);
        }

        // Initialize Google API client.
        mGoogleApiClient = buildGoogleApiClient();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVED_PROGRESS, mSignInProgress);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Add items to action bar if present.
        getMenuInflater().inflate(R.menu.menu_actions, menu);
        mActionBarMenu = menu;

        checkSignInOption();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Sign In or Sign Out option selected.
            case R.id.signin_signout:
                if (mGoogleApiClient.isConnected()) {
                    signOut();
                }
                else {
                    signIn();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
                if (!mGoogleApiClient.isConnecting()) {
                    mGoogleApiClient.connect();
                }
                break;
        }
    }

    @Override
    // User just signed in and successfully connected to Google Play Services.
    public void onConnected(Bundle connectionHint) {
        mSignInProgress = STATE_DEFAULT;
        checkSignInOption();
        onSignIn();
    }

    @Override
    // Could not connect to Google Play Services.
    public void onConnectionFailed(ConnectionResult result) {
        // Could not get the API requested. Device might not support requested
        // API or may not have the required component installed.
        if (result.getErrorCode() == ConnectionResult.API_UNAVAILABLE) {
            // TODO handle?
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

        // User is signed out if no connection to Google Play Services.
        onSignOut();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // Lost connection. Attempt to re-establish connection.
        mGoogleApiClient.connect();
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
     * Toggle the action bar item between "Sign In" and "Sign Out" depending
     * on the user's signed-in status.
     */
    private void checkSignInOption() {
        MenuItem item = mActionBarMenu.findItem(R.id.signin_signout);
        if (mGoogleApiClient.isConnected())
            item.setTitle(R.string.sign_out);
        else
            item.setTitle(R.string.sign_in);
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
                mGoogleApiClient.connect();
            }
        }
        // Google Play Services could not provide an intent for other errors.
        // Show default Google Play Services error dialog.
        else {
            createErrorDialog().show();
        }
    }

    /**
     * Attempt to sign in via Google+.
     */
    protected void signIn() {
        // Only process if not transitioning between connection state for the
        // GoogleApiClient.
        if (mGoogleApiClient.isConnecting()) {
            return;
        }

        mSignInProgress = STATE_SIGN_IN;
        mGoogleApiClient.connect();
    }

    /**
     * Callback after successful sign-in.
     */
    protected void onSignIn() { }

    /**
     * Attempt to sign out.
     */
    protected void signOut() {
        // Only process if not transitioning between connection state for the
        // GoogleApiClient.
        if (mGoogleApiClient.isConnecting()) {
            return;
        }

        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            mGoogleApiClient.disconnect();
        }

        checkSignInOption();
        onSignOut();
    }

    /**
     * Callback after successful sign-out.
     */
    protected void onSignOut() { }
}
