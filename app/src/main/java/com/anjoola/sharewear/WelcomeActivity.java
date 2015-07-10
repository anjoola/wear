package com.anjoola.sharewear;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;

/**
 * First activity. Contains code for logging in.
 */
public class WelcomeActivity extends ShareWearBaseActivity implements
        View.OnClickListener {

    // Button for editing the local profile.
    Button mProfileButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_activity);

        mProfileButton = (Button) findViewById(R.id.profile_setup_button);
        mProfileButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        // Start intent to edit local profile.
        if (v.getId() == R.id.profile_setup_button) {
            Intent intent = new Intent (
                    Intent.ACTION_VIEW, ContactsContract.Profile.CONTENT_URI);
            startActivity (intent);
//            user_profile.cid = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_IS_USER_PROFILE));
//
//            signIn();
//
//
//            Intent i = new Intent(Intent.ACTION_EDIT);
//            // i.setType(ContactsContract.RawContacts.CONTENT_ITEM_TYPE);
//            Uri contactUri =
//                    ContentUris.withAppendedId(ContactsContract.Profile.CONTENT_URI, Long.parseLong(up.cid));
//            // i.setData(Uri.parse(ContactsContract.Contacts.CONTENT_LOOKUP_URI + "/" + up.cid));
//            i.setDataAndType(contactUri,
//                    ContactsContract.RawContacts.CONTENT_ITEM_TYPE);
//            startActivity(i);
        }
    }
}
