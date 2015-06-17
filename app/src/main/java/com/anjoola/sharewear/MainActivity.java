package com.anjoola.sharewear;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;


// TODO rename to LoginActivity
public class MainActivity extends FragmentActivity implements
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

    // Used to connect to Google Play Services and allow Google+ sign in.
    private GoogleApiClient mGoogleApiClient;





    private static final String TAG = "sharewear-debug";

    private static final int RC_SIGN_IN = 0;

    private static final String SAVED_PROGRESS = "sign_in_progress";




    // Used to store the PendingIntent most recently returned by Google Play
    // services until the user clicks 'sign in'.
    private PendingIntent mSignInIntent;

    // Used to store the error code most recently returned by Google Play services
    // until the user clicks 'sign in'.
    private int mSignInError;


    // Buttons and objects.
    private SignInButton mSignInButton;
    private Button mSignOutButton;
    private TextView mStatus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // Buttons and objects.
        mSignInButton = (SignInButton) findViewById(R.id.sign_in_button);
        mSignOutButton = (Button) findViewById(R.id.sign_out_button);
        mStatus = (TextView) findViewById(R.id.sign_in_status);

        // Button listeners.
        mSignInButton.setOnClickListener(this);
        mSignOutButton.setOnClickListener(this);

        // Restore saved instance.
        if (savedInstanceState != null) {
            mSignInProgress = savedInstanceState
                    .getInt(SAVED_PROGRESS, STATE_DEFAULT);
        }

        mGoogleApiClient = buildGoogleApiClient();
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVED_PROGRESS, mSignInProgress);
    }

    @Override
    public void onClick(View v) {
        // Only process button clicks when not transitioning between connection
        // state for the GoogleApiClient.
        if (mGoogleApiClient.isConnecting())
            return;

        switch (v.getId()) {
            case R.id.sign_in_button:
                mStatus.setText(R.string.status_signing_in);
                mSignInProgress = STATE_SIGN_IN;
                mGoogleApiClient.connect();
                break;

            // Allow the user to choose an account to sign in with (clear the
            // default account).
            case R.id.sign_out_button:
                if (mGoogleApiClient.isConnected()) {
                    Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                    mGoogleApiClient.disconnect();
                }
                onSignedOut();
                break;
        }
    }

    @Override
    // User just signed in and successfully connected to Google Play Services.
    public void onConnected(Bundle connectionHint) {
        // TODO Update the user interface to reflect that the user is signed in.
        mSignInButton.setEnabled(false);
        mSignOutButton.setEnabled(true);

        // TODO update
        // Retrieve some profile information to personalize our app for the user.
        Person currentUser = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);

        try {
            mStatus.setText(String.format(
                    getResources().getString(R.string.signed_in_as),
                    currentUser.getDisplayName()));
        } catch (NullPointerException e) {
            // TODO lost internet connection here
        }

        // Sign in process complete.
        mSignInProgress = STATE_DEFAULT;
    }

    @Override
    // Could not connect to Google Play Services.
    public void onConnectionFailed(ConnectionResult result) {
        // Could not get the API requested. Device might not support requested
        // API or may not have the required component installed.
        if (result.getErrorCode() == ConnectionResult.API_UNAVAILABLE) {
            // TODO handle?
            Log.w(TAG, "API Unavailable.");
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

        // User is signed out if no connection go Google Play Services.
        onSignedOut();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        switch (requestCode) {
            case RC_SIGN_IN:
                if (resultCode == RESULT_OK) {
                    // If the error resolution was successful we should continue
                    // processing errors.
                    mSignInProgress = STATE_SIGN_IN;
                } else {
                    // If the error resolution was not successful or the user canceled,
                    // we should stop processing errors.
                    mSignInProgress = STATE_DEFAULT;
                }

                if (!mGoogleApiClient.isConnecting()) {
                    // If Google Play services resolved the issue with a dialog then
                    // onStart is not called so we need to re-attempt connection here.
                    mGoogleApiClient.connect();
                }
                break;
        }
    }

    /**
     * Handler for when the user signs out.
     */
    private void onSignedOut() {
        // TODO
        mSignInButton.setEnabled(true);
        mSignOutButton.setEnabled(false);

        mStatus.setText(R.string.status_signed_out);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // Lost connection. Attempt to re-establish connection.
        mGoogleApiClient.connect();
    }

    private Dialog createErrorDialog() {
        if (GooglePlayServicesUtil.isUserRecoverableError(mSignInError)) {
            return GooglePlayServicesUtil.getErrorDialog(
                    mSignInError,
                    this,
                    RC_SIGN_IN,
                    new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            Log.e(TAG, "Google Play services resolution cancelled");
                            mSignInProgress = STATE_DEFAULT;
                            mStatus.setText(R.string.status_signed_out);
                        }
                    });
        } else {
            return new AlertDialog.Builder(this)
                    .setMessage(R.string.play_services_error)
                    .setPositiveButton(R.string.close,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.e(TAG, "Google Play services error could not be "
                                            + "resolved: " + mSignInError);
                                    mSignInProgress = STATE_DEFAULT;
                                    mStatus.setText(R.string.status_signed_out);
                                }
                            }).create();
        }
    }
}
