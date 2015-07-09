package com.anjoola.sharewear;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

/**
 * Activity to exchange contact information over NFC. Can switch to manual
 * input if NFC is not turned on or not desired.
 */
public class ContactAddNFCActivity extends ShareWearActivity implements
        View.OnClickListener, NfcAdapter.CreateNdefMessageCallback {
    // Animation for NFC.
    AnimationDrawable animation;

    // NFC manager.
    NfcManager mNfcManager;
    NfcAdapter mAdapter;

    // Prompt for turning on NFC.
    private AlertDialog mNfcDialog;

    // Button for switching to manual input.
    Button mSwitchButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_add_nfc_activity);

        // Initialize objects.
        mNfcDialog = null;
        mSwitchButton = (Button) findViewById(R.id.manual_input_button);
        mSwitchButton.setOnClickListener(this);

        // Get NFC manager and adapter.
        mNfcManager = (NfcManager) this.getApplicationContext()
                .getSystemService(Context.NFC_SERVICE);
        mAdapter = mNfcManager.getDefaultAdapter();

        // Start animation.
        ImageView animationView = (ImageView) findViewById(R.id.nfc_image);
        animationView.setBackgroundResource(R.drawable.nfc_animation);
        animation = (AnimationDrawable) animationView.getBackground();

        // Used for sending NFC data.
        NfcAdapter mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        mNfcAdapter.setNdefPushMessageCallback(this, this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // Check NFC status each time.
        checkNFCStatus();

        if (hasFocus)
            animation.start();
        else
            animation.stop();
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

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.manual_input_button) {
            toManualInputActivity();
        }
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        // Send contact information via NFC.
        String message = prefGetContactDetails();
        //String message = ContactDetails
        //        .getMyContactDetails((ShareWearApplication) getApplication());
        Log.e("----", message);
                NdefRecord ndefRecord = NdefRecord.createMime("text/plain",
                        message.getBytes());
        NdefMessage ndefMessage = new NdefMessage(ndefRecord);
        return ndefMessage;
    }

    /**
     * Checks if NFC is turned on. If not, prompt the user to turn it on.
     */
    private void checkNFCStatus() {
        // NFC is turned off.
        if (mAdapter != null && !mAdapter.isEnabled()) {
            promptUserNFCEnable();
        }
    }

    /**
     * Prompt user to turn on NFC if they haven't done so already.
     */
    private void promptUserNFCEnable() {
        // Create the dialog if it hasn't been created yet.
        if (mNfcDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            // Set "Go to Settings" and "Cancel" buttons.
            builder.setPositiveButton(R.string.go_to_settings, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Go to NFC settings menu.
                    startActivity(new Intent(android.provider.Settings.ACTION_NFC_SETTINGS));
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Switch to manual input.
                    toManualInputActivity();
                }
            });

            builder.setTitle(R.string.nfc_prompt_title);
            builder.setMessage(R.string.nfc_prompt);
            mNfcDialog = builder.create();
            mNfcDialog.setCancelable(false);
            mNfcDialog.setCanceledOnTouchOutside(false);
        }
        mNfcDialog.show();
    }

    /**
     * Switch to the manual input activity.
     */
    private void toManualInputActivity() {
        Intent intent = new Intent(this, ContactAddActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }
}
