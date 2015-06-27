package com.anjoola.sharewear;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.text.SimpleDateFormat;
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
    Uri mImageFile;

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
        mPhoto.setOnClickListener(this);
        mImageFile = null;

        getActionBar().setHideOnContentScrollEnabled(false);
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
        else if (v.getId() == R.id.person_photo) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            mImageFile = getImageFileUri();
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageFile);
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
                // TODO
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
            mPhoto.setImageURI(mImageFile);
        }
    }

    /**
     * Create a file URI for saving an image.
     *
     * @return Reference to the URI.
     */
    private Uri getImageFileUri(){
        return Uri.fromFile(getImageFile());
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
