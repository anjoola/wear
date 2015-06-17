package com.anjoola.sharewear;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.SignInButton;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;


public class MainActivity extends ShareWearActivity implements
        View.OnClickListener {

    private static final String TAG = "sharewear-debug";


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
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                mStatus.setText(R.string.status_signing_in);
                signIn();
                break;

            case R.id.sign_out_button:
                signOut();
                break;
        }
    }

    @Override
    public void onSignIn() {
        // TODO
        mSignInButton.setEnabled(false);
        mSignOutButton.setEnabled(true);

        Person currentUser = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);

        try {
            mStatus.setText(String.format(
                    getResources().getString(R.string.signed_in_as),
                    currentUser.getDisplayName()));
        } catch (NullPointerException e) {
            // TODO lost internet connection here
        }

    }

    @Override
    public void onSignOut() {
        // TODO
        mSignInButton.setEnabled(true);
        mSignOutButton.setEnabled(false);

        mStatus.setText(R.string.status_signed_out);
    }
}
