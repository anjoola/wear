package com.anjoola.sharewear;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentProviderOperation;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.anjoola.sharewear.util.ContactDetails;
import com.anjoola.sharewear.util.ContactsImageProvider;
import com.anjoola.sharewear.util.RoundedImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Activity to add new contact information.
 */
public class ContactAddActivity extends ShareWearActivity implements
        View.OnClickListener {
    private final int CAPTURE_IMAGE_CODE = 100;

    // EditTexts for inputs.
    EditText mName, mPhone, mEmail;

    // Button for adding a photo.
    RoundedImageView mPhoto;
    FrameLayout mPhotoButton;
    File mImageFile;
    Uri mImageFileUri;

    private boolean submitted = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_add_activity);

        mName = (EditText) findViewById(R.id.person_name);
        mPhone = (EditText) findViewById(R.id.person_phone);
        mEmail = (EditText) findViewById(R.id.person_email);

        mPhoto = (RoundedImageView) findViewById(R.id.person_photo);
        mPhotoButton = (FrameLayout) findViewById(R.id.person_photo_button);
        mPhotoButton.setOnClickListener(this);
        mImageFile = null;
        mImageFileUri = null;
        reset();

        if (getActionBar() != null)
            getActionBar().setHideOnContentScrollEnabled(false);
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (submitted) {
            reset();
        }
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
        }
    }

    @Override
    public void onClick(View v) {
        // Start intent to take a picture.
        if (v.getId() == R.id.person_photo_button) {
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
            try {
                // Create compressed bitmap of photo.
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bitmap = BitmapFactory.decodeStream(
                        new FileInputStream(mImageFile), null, options);
                bitmap = ContactsImageProvider.getCompressedBitmap(bitmap,
                        200, this);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();

                ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValueBackReference(Data.RAW_CONTACT_ID, contactIdx)
                        .withValue(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE)
                        .withValue(Data.IS_SUPER_PRIMARY, 1)
                        .withValue(Photo.PHOTO, byteArray)
                        .build());
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        // Add to the "ShareWear" group.
        String group = getString(R.string.app_name);
        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, contactIdx)
                .withValue(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE)
                .withValue(GroupMembership.GROUP_ROW_ID, getGroupId(group))
                .build());

        // Do a batch operation to insert all data.
        try {
            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            ShareWearApplication app = (ShareWearApplication) getApplication();
            app.newContactDetails = new ContactDetails(name, phone, email);

            // Update the contacts list view.
            String photoUri;
            if (mImageFileUri == null) {
                ContactsImageProvider provider =
                        new ContactsImageProvider(getBaseContext());
                photoUri = provider.getDefaultContactUri(name);
            }
            else {
                photoUri = mImageFileUri.toString();
            }

            ((ContactsAllFragment) app.allFragment).addNewContact(
                    new ContactDetails(name, phone, email, photoUri));

            // Show success page.
            Intent intent = new Intent(this, ContactAddDoneActivity.class);
            startActivity(intent);

            submitted = true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the group ID for a particular group name. If it does not exist,
     * then creates a new group.
     *
     * @param groupName The name of the group.
     * @return The ID for the group.
     */
    private String getGroupId(String groupName) {
        String groupId = isGroupId(groupName);

        // Create new one if it doesn't exist.
        if (groupId == null) {
            ArrayList<ContentProviderOperation> ops =
                    new ArrayList<ContentProviderOperation>();
            ops.add(ContentProviderOperation.newInsert(Groups.CONTENT_URI)
                    .withValue(Groups.TITLE, groupName)
                    .withValue(Groups.GROUP_VISIBLE, true)
                    .withValue(Groups.ACCOUNT_NAME, groupName)
                    .withValue(Groups.ACCOUNT_TYPE, groupName)
                    .build());

            try {
                getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            }
            catch (Exception e) { }

            groupId = isGroupId(groupName);
        }

        return groupId;
    }

    /**
     * Checks to see if the group name has a group ID. If so, return it.
     * Otherwise, returns null.
     *
     * @param groupName The name of the group.
     * @return The group ID if it exists, or null.
     */
    private String isGroupId(String groupName) {
        String selection = Groups.DELETED + " = ? AND " +
                Groups.GROUP_VISIBLE + " = ?";
        String[] selectionArgs = { "0", "1" };

        Cursor cursor = getContentResolver().query(Groups.CONTENT_URI, null,
                selection, selectionArgs, null);
        cursor.moveToFirst();

        // Search through the groups.
        String groupId = null;
        for (int i = 0; i < cursor.getCount(); i++) {
            String id = cursor.getString(cursor.getColumnIndex(Groups._ID));
            String title = cursor.getString(cursor.getColumnIndex(Groups.TITLE));

            if (title.equals(groupName)) {
                groupId = id;
                break;
            }
            cursor.moveToNext();
        }
        cursor.close();
        return groupId;
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

    /**
     * Resets the fields.
     */
    private void reset() {
        mName.setText("");
        mPhone.setText("");
        mEmail.setText("");

        // Reset the take photo image.
        mPhoto.setImageResource(R.mipmap.ic_add_picture);
        mImageFile = null;
        mImageFileUri = null;

        submitted = false;
    }
}
