package com.anjoola.sharewear;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import java.io.File;

/**
 * Used to encoding and decoding contact information.
 */
public class ContactDetails {
    public String name;
    public String phone;
    public String email;
    public File photo;

    public ContactDetails(String name, String phone, String email) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.photo = null;
    }

    public ContactDetails(String name, String phone, String email, File photo) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.photo = photo;
    }

    @Override
    public String toString() {
        return name + "###" + phone + "###" + email;
    }

    /**
     * Decode contact details received via NFC.
     *
     * @param nfcData The data received.
     * @return The contact details associated with the NFC data.
     */
    public static ContactDetails decodeNfcData(String nfcData) {
        String[] data = nfcData.split("###");
        return new ContactDetails(data[0], data[1], data[2]);
    }

    /**
     * Get contact details for the current user.
     *
     * @param context Reference to the current context.
     * @return An encoded string containing the contact information.
     */
    public static ContactDetails getMyContactDetails(Context context) {
        // Query for this user's profile.
        final ContentResolver content = context.getContentResolver();
        final Cursor cursor = content.query(
                Uri.withAppendedPath(
                        ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY),
                ProfileQuery.PROJECTION,

                ContactsContract.Contacts.Data.MIMETYPE + "=? OR "
                        + ContactsContract.Contacts.Data.MIMETYPE + "=? OR "
                        + ContactsContract.Contacts.Data.MIMETYPE + "=? OR "
                        + ContactsContract.Contacts.Data.MIMETYPE + "=?",
                new String[]{
                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                        ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                },

                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC"
        );

        String mime_type, name = null, phone = null, email = null;
        while (cursor.moveToNext()) {
            mime_type = cursor.getString(ProfileQuery.MIME_TYPE);
            if (mime_type.equals(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE))
                email = cursor.getString(ProfileQuery.EMAIL);
            else if (mime_type.equals(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE))
                name = cursor.getString(ProfileQuery.GIVEN_NAME) + " " + cursor.getString(ProfileQuery.FAMILY_NAME);
            else if (mime_type.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE))
                phone = cursor.getString(ProfileQuery.PHONE_NUMBER);
            // TODO contact photo
            // else if (mime_type.equals(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE))
            //photo???
            // user_profile.addPossiblePhoto(Uri.parse(cursor.getString(ProfileQuery.PHOTO)));
        }
        cursor.close();

        return new ContactDetails(name, phone, email);
    }

    /**
     * Returns null if any field is missing.
     */
    public static String getMyContactDetailsStrict(Context context) {
        ContactDetails details = getMyContactDetails(context);
        if (details.name == null || details.phone == null || details.email == null)
            return null;
        return details.toString();
    }

    /**
     * Used for getting user profile information.
     */
    private interface ProfileQuery {
        // Columns to extract from the profile query results.
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Photo.PHOTO_URI,
                ContactsContract.Contacts.Data.MIMETYPE
        };

        int EMAIL = 0;
        int FAMILY_NAME = 1;
        int GIVEN_NAME = 2;
        int PHONE_NUMBER = 3;
        int PHOTO = 4;
        int MIME_TYPE = 5;
    }
}
