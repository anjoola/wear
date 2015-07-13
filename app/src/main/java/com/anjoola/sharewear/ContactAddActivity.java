package com.anjoola.sharewear;

import android.content.ContentProviderOperation;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.anjoola.sharewear.util.ContactDetails;
import com.anjoola.sharewear.util.RoundedImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Activity to add new contact information manually. Can switch to input via
 * NFC if desired.
 */
public class ContactAddActivity extends ShareWearActivity implements
        View.OnClickListener {
    private final int CAPTURE_IMAGE_CODE = 100;

    // Button for switching to NFC input.
    Button mSwitchInputButton;

    // EditTexts for inputs.
    EditText mName, mPhone, mEmail;

    // Button for adding a photo.
    RoundedImageView mPhoto;
    FrameLayout mPhotoButton;
    File mImageFile;
    Uri mImageFileUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_add_activity);

        mSwitchInputButton = (Button) findViewById(R.id.nfc_input_button);
        mSwitchInputButton.setOnClickListener(this);

        mName = (EditText) findViewById(R.id.person_name);
        mPhone = (EditText) findViewById(R.id.person_phone);
        mEmail = (EditText) findViewById(R.id.person_email);

        mPhoto = (RoundedImageView) findViewById(R.id.person_photo);
        mPhotoButton = (FrameLayout) findViewById(R.id.person_photo_button);
        mPhotoButton.setOnClickListener(this);
        mImageFile = null;
        mImageFileUri = null;

        getActionBar().setHideOnContentScrollEnabled(false);
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        // See if this was started via NFC input. If so, fill in the text fields
        // with the data received via NFC.
        Intent intent = getIntent();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMessages = intent.getParcelableArrayExtra(
                    NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage message = (NdefMessage) rawMessages[0];
            String nfcData = new String(message.getRecords()[0].getPayload());
            ContactDetails info = ContactDetails.decodeNfcData(nfcData);

            // Set the text fields.
            mName.setText(info.name);
            mPhone.setText(info.phone);
            mEmail.setText(info.email);

            // Hide the NFC input button.
            LinearLayout buttonLayout = (LinearLayout) findViewById(R.id.button_layout);
            buttonLayout.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        // Switch to NFC input. Just bring the activity to the front if it has
        // already been started.
        if (v.getId() == R.id.nfc_input_button) {
            Intent intent = new Intent(this, ContactAddNFCActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }
        // Start intent to take a picture.
        else if (v.getId() == R.id.person_photo_button) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            mImageFile = getImageFile();
            mImageFileUri = getImageFileUri(mImageFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageFileUri);
            startActivityForResult(intent, CAPTURE_IMAGE_CODE);
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
                addContact();
                return true;
            case R.id.action_sign_out:
                signOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Result from taking a picture. Set the image to the newly-taken photo.
        if (requestCode == CAPTURE_IMAGE_CODE && resultCode == RESULT_OK &&
                mImageFile != null) {
            mPhoto.setImageURI(mImageFileUri);
        }
    }

    /**
     * Adds a new contact based on the values filled out in the text fields.
     */
    private void addContact() {
        // TODO error message if name not filled out


        // Get filled out fields.
        String name = mName.getText().toString();
        String phone = mPhone.getText().toString();
        String email = mEmail.getText().toString();

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        int contactIdx = ops.size();

        // Insert raw contact into contacts database.
        ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_TYPE, null)
                .withValue(RawContacts.ACCOUNT_NAME, null)
                .build());

        // Display name.
        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, contactIdx)
                .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                .withValue(StructuredName.DISPLAY_NAME, name)
                .build());

        // Phone number.
        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, contactIdx)
                .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                .withValue(Phone.NUMBER, phone)
                .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
                .build());

        // Email.
        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, contactIdx)
                .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                .withValue(Email.DATA, email)
                .withValue(Email.TYPE, Email.TYPE_OTHER)
                .build());

        // Photo.
        if (mImageFile != null) {
            int imgLength = (int) mImageFile.length();
            ByteArrayOutputStream stream = new ByteArrayOutputStream(imgLength);
            byte[] bytes = new byte[imgLength];
            FileInputStream inStream;

            // Convert photo to byte stream.
            try {
                inStream = new FileInputStream(mImageFile);
                inStream.read(bytes);
                stream.write(bytes, 0, imgLength);
                stream.flush();
            }
            catch (Exception e) { }

            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, contactIdx)
                    .withValue(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE)
                    .withValue(Data.IS_SUPER_PRIMARY, 1)
                    .withValue(Photo.PHOTO, stream.toByteArray())
                    .build());
        }

        // Do a batch operation to insert all data.
        try {
            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);

            // Show success page.
            ShareWearApplication app = (ShareWearApplication) getApplication();
            app.newContactDetails = new ContactDetails(name, phone, email);
            Intent intent = new Intent(this, ContactAddDoneActivity.class);
            startActivity(intent);
        }
        // TODO handle exceptions
        catch (RemoteException exp) {}
        catch (OperationApplicationException exp) {}
    }

    /**
     * Create a file URI for saving an image.
     *
     * @param file The file to create a URI from.
     * @return Reference to the URI.
     */
    private Uri getImageFileUri(File file){
        return Uri.fromFile(file);
    }

    /**
     * Create a file for an image.
     *
     * @return Reference to the image file.
     */
    private File getImageFile(){
        // Get the directory for saving images.
        File directory = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), getString(R.string.app_name));

        // Create it if it doesn't exist.
        if (!directory.exists()){
            if (!directory.mkdirs()){
                return null;
            }
        }

        // Create file based on timestamp.
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        File file = new File(directory.getPath() + File.separator +
                "IMG_"+ timestamp + ".jpg");
        return file;
    }
}
