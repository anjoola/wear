package com.anjoola.sharewear;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.SignInButton;


public class MainActivity extends ShareWearActivity implements
        View.OnClickListener {
    private SignInButton mSignInButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mSignInButton = (SignInButton) findViewById(R.id.sign_in_button);

        // Change Google+ button's text.
        for (int i = 0; i < mSignInButton.getChildCount(); i++) {
            View v = mSignInButton.getChildAt(i);
            if (v instanceof TextView) {
                TextView mTextView = (TextView) v;
                mTextView.setText(R.string.sign_in_button_text);
                break;
            }
        }

        // Button listener.
        mSignInButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
        }
    }

    @Override
    public void onSignIn() {
        mSignInButton.setEnabled(false);

        /* TODO
         Person currentUser = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
         currentUser.getDisplayName()));
         */
    }

    @Override
    public void onSignOut() {
        mSignInButton.setEnabled(true);
    }
}
