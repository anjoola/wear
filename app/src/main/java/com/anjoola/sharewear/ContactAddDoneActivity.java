package com.anjoola.sharewear;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;

import com.anjoola.sharewear.util.ContactDetails;

/**
 * Activity to add new contact information manually. Can switch to input via
 * NFC if desired.
 */
public class ContactAddDoneActivity extends ShareWearActivity {
    // How long to show this activity before transitioning back to the contacts
    // listing (in milliseconds).
    final int TRANSITION_TIME = 4 * 1000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_add_done_activity);

        TextView name = (TextView) findViewById(R.id.person_name);
        TextView phone = (TextView) findViewById(R.id.person_phone);
        TextView email = (TextView) findViewById(R.id.person_email);
        ImageView checkmark = (ImageView) findViewById(R.id.checkmark);

        // Set fields.
        ShareWearApplication app = (ShareWearApplication) getApplication();
        ContactDetails contact = app.newContactDetails;
        name.setText(contact.name);
        phone.setText(contact.phone);
        email.setText(contact.email);

        // Start animation.
        checkmark.setBackgroundResource(R.drawable.checkmark_checked);
        AnimationDrawable animation = (AnimationDrawable) checkmark.getBackground();
        animation.start();

        new Handler().postDelayed(new Runnable() {
            public void run() {
                Intent intent = new Intent(ContactAddDoneActivity.this,
                        ContactsListActivity.class);
                startActivity(intent);
                finish();
            }
        }, TRANSITION_TIME);
    }
}
