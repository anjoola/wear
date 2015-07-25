package com.anjoola.sharewear.util;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;

import com.anjoola.sharewear.ShareWearApplication;

import java.util.ArrayList;

/**
 * Loads contacts.
 */
public class ContactsListLoader {
    // URIs for retrieving contacts information.
    private final static Uri CONTACTS_URI = Contacts.CONTENT_URI;
    private final static Uri DATA_URI = Data.CONTENT_URI;

    /**
     * Loads contacts from the phone's contact book.
     *
     * @param app The application.
     * @param activity The current activity.
     * @param start Index to start at.
     * @param number Number of contacts to load, or -1 for the rest of them.
     * @param callback Callback after each contact is loaded.
     *
     * @return true if there are no more entries after the last one returned,
     *         false otherwise.
     */
    public static boolean loadContacts(ShareWearApplication app, Activity activity,
                                    int start, int number,
                                    ContactLoadCallback callback) {
        ArrayList<ContactDetails> contactsList = new ArrayList<ContactDetails>();
        Cursor cursor = activity.getContentResolver().query(
                CONTACTS_URI, null, null, null, Contacts.DISPLAY_NAME + " ASC ");

        // Move to the specified start index, if it exists.
        if (!cursor.moveToPosition(start))
            return false;

        // Loop through every contact.
        int numLoaded = 0;
        do {
            long contactId = cursor.getLong(cursor.getColumnIndex("_ID"));

            // Retrieve individual fields for this contact.
            Cursor dCursor = activity.getContentResolver().query(
                    DATA_URI, null, Data.CONTACT_ID + "=" + contactId,
                    null, null);

            // No data, move on.
            if (!dCursor.moveToFirst()) {
                dCursor.close();
                continue;
            }

            getContactDetails(contactsList, activity, dCursor);
            dCursor.close();

            // Callback after contact is loaded.
            if (callback != null)
                callback.callback();

        } while ((++numLoaded < number || number == -1) && cursor.moveToNext());

        app.mContactsList.addAll(contactsList);
        cursor.close();
        return numLoaded <= number;
    }

    /**
     * Loads contacts from the phone's contact book.
     *
     * @param app The application.
     * @param activity The current activity.
     * @param start Index to start at.
     * @param number Number of contacts to load, or -1 for the rest of them.
     *
     * @return true if there are no more entries after the last one returned,
     *         false otherwise.
     */
    public static boolean loadContacts(ShareWearApplication app, Activity activity,
                                    int start, int number) {
        return loadContacts(app, activity, start, number, null);
    }

    /**
     * Get details for the contact at the cursor. Add to the list of all
     * contacts.
     *
     * @param list The temporary list of contacts.
     * @param activity The current activity.
     * @param cursor The cursor.
     */
    private static void getContactDetails(ArrayList<ContactDetails> list,
                                          Activity activity, Cursor cursor) {
        // Details to retrieve.
        String photoUri =
                cursor.getString(cursor.getColumnIndex(Phone.PHOTO_URI));
        String name = cursor.getString(cursor.getColumnIndex(
                Contacts.DISPLAY_NAME));
        String phone = null, email = null;

        // Loop since there might be multiple entries.
        do {
            String columnType =
                    cursor.getString(cursor.getColumnIndex("mimetype"));
            int colIndex = cursor.getColumnIndex("data1");

            // Phone numbers.
            if (columnType.equals(Phone.CONTENT_ITEM_TYPE)) {
                switch (cursor.getInt(cursor.getColumnIndex("data2"))) {
                    case Phone.TYPE_MOBILE:
                        phone = cursor.getString(colIndex);
                        break;
                    case Phone.TYPE_WORK:
                        if (phone == null)
                            phone = cursor.getString(colIndex);
                        break;
                    case Phone.TYPE_HOME:
                        if (phone == null)
                            phone = cursor.getString(colIndex);
                        break;
                    case Phone.TYPE_OTHER:
                        if (phone == null)
                            phone = cursor.getString(colIndex);
                        break;
                }
            }

            // Email.
            if (columnType.equals(Email.CONTENT_ITEM_TYPE)) {
                switch (cursor.getInt(cursor.getColumnIndex("data2"))) {
                    case Email.TYPE_HOME:
                        email = cursor.getString(colIndex);
                        break;
                    case Email.TYPE_WORK:
                        if (email == null)
                            email = cursor.getString(colIndex);
                        break;
                    case Email.TYPE_OTHER:
                        if (email == null)
                            email = cursor.getString(colIndex);
                        break;
                }
            }
        } while (cursor.moveToNext());

        // If contact has neither a phone nor email, don't show them.
        if (phone == null && email == null) return;

        // Get default photo if contact photo does not exist.
        if (photoUri == null) {
            ContactsImageProvider provider =
                    new ContactsImageProvider(activity.getBaseContext());
            photoUri = provider.getDefaultContactUri(name);
        }

        list.add(new ContactDetails(name, phone, email, photoUri));
    }
}
