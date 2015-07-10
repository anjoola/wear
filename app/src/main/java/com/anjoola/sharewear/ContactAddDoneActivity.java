package com.anjoola.sharewear;

import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Activity to add new contact information manually. Can switch to input via
 * NFC if desired.
 */
public class ContactAddDoneActivity extends ShareWearActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_add_done_activity);

        TextView name = (TextView) findViewById(R.id.person_name);
        TextView phone = (TextView) findViewById(R.id.person_phone);
        TextView email = (TextView) findViewById(R.id.person_email);

        RoundedImageView photo = (RoundedImageView) findViewById(R.id.contact_photo);
        ImageView checkmark = (ImageView) findViewById(R.id.checkmark);

        // Set fields.
        ShareWearApplication app = (ShareWearApplication) getApplication();
        ContactDetails contact = app.newContactDetails;
        name.setText(contact.name);
        phone.setText(contact.phone);
        email.setText(contact.email);
        if (contact.photo != null) {
            // TODO doesn't work if there is a contact photo?
            //Uri imageUri = Uri.fromFile(contact.photo);
            //photo.setImageURI(imageUri);
            checkmark.setBackgroundResource(R.drawable.checkmark);
        }
        else {
            checkmark.setBackgroundResource(R.drawable.checkmark_checked);
        }

        // Start animation.
        AnimationDrawable animation = (AnimationDrawable) checkmark.getBackground();
        animation.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.contact_add_nfc_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sign_out:
                signOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
