package com.anjoola.sharewear;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.anjoola.sharewear.util.ContactDetails;
import com.anjoola.sharewear.util.RegistrationIntentService;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

/**
 * Welcome screen. Has the user input the data they want to share.
 */
public class WelcomeActivity extends ShareWearActivity implements
        View.OnClickListener {
    // Reference to the ShareWear application.
    private ShareWearApplication mApp;

    // EditTexts for inputs.
    EditText mName, mPhone, mEmail;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_activity);

        mApp = (ShareWearApplication) getApplication();
        Button profileButton = (Button) findViewById(R.id.profile_setup_button);
        profileButton.setOnClickListener(this);

        mName = (EditText) findViewById(R.id.person_name);
        mPhone = (EditText) findViewById(R.id.person_phone);
        mEmail = (EditText) findViewById(R.id.person_email);

        // No action bar.
        ActionBar bar = getActionBar();
        if (bar != null) {
            getActionBar().hide();
        }
    }

    @Override
    public void onClick(View v) {
        // Check if profile information is complete.
        if (v.getId() == R.id.profile_setup_button) {
            checkProfileComplete();
        }
    }

    /**
     * Check to see if the user has finished filling out their profile. If so,
     * move on to the contacts listing.
     */
    private void checkProfileComplete() {
        // Get filled out fields.
        String name = mName.getText().toString();
        String phone = mPhone.getText().toString();
        String email = mEmail.getText().toString();

        // Make sure a name and one of phone or email is filled out. If not,
        // show an error dialog.
        if (name.length() == 0 || (phone.length() == 0 && email.length() == 0)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setPositiveButton(R.string.okay, null);

            builder.setMessage(name.length() == 0 ? R.string.error_name_blank :
                    R.string.error_detail_blank);

            Dialog dialog = builder.create();
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
            return;
        }

        // Otherwise, save this information.
        ContactDetails me = new ContactDetails(name, phone, email);
        String details = me.toString();
        mApp.prefSetContactDetails(details);
        ((ShareWearApplication) getApplication()).myDetails = details;

        // Register this with the cloud server.
        runOnUiThread(new Runnable() {
            public void run() {
                new RegisterAsync().execute();
            }
        });

        // Show contacts listing.
        Intent intent = new Intent(this, ContactsListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Gets the GCM token and sends it to the server in the background.
     */
    private class RegisterAsync extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            InstanceID instanceID = InstanceID.getInstance(WelcomeActivity.this);

            try {
                String token = instanceID.getToken(getString(R.string.SENDER_ID),
                        GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                RegistrationIntentService
                        .sendRegistrationToServer(getApplication(), token);
            } catch (IOException e) { }

            return null;
        }
    }
}
