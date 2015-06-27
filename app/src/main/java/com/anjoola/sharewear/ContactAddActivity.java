package com.anjoola.sharewear;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * Activity to add new contact information manually. Can switch to input via
 * NFC if desired.
 */
public class ContactAddActivity extends ShareWearActivity implements
        View.OnClickListener {
    // Button for switching to NFC input.
    Button mSwitchInputButton;

    // EditTexts for inputs.
    EditText mName, mPhone, mEmail;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_add_activity);

        mSwitchInputButton = (Button) findViewById(R.id.nfc_input_button);
        mSwitchInputButton.setOnClickListener(this);

        mName = (EditText) findViewById(R.id.person_name);
        mPhone = (EditText) findViewById(R.id.person_phone);
        mEmail = (EditText) findViewById(R.id.person_email);

        getActionBar().setHideOnContentScrollEnabled(false);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.nfc_input_button) {
            Intent intent = new Intent(this, ContactAddNFCActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.contact_add_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                // TODO
                return true;
            case R.id.action_sign_out:
                signOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
