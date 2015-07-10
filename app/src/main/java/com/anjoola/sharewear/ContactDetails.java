package com.anjoola.sharewear;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;

import java.io.File;

/**
 * Used to encoding and decoding contact information to be sent via NFC.
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

    /**
     * Get contact details for the current user, to send to a new contact via
     * NFC.
     *
     * @param app Reference to the application.
     * @return An encoded string containing the contact information.
     */
    public static String getMyContactDetails(ShareWearApplication app) {
        // TODO app.googleApiClient is null if already logged in...
        GoogleApiClient client = app.googleApiClient;
        String name = Plus.PeopleApi.getCurrentPerson(client).getDisplayName();
        String phone = "TODO"; // TODO how to get phone number?
        String email = Plus.AccountApi.getAccountName(client);

        return name + "###" + phone + "###" + email;
    }

    public static ContactDetails decodeNfcData(String nfcData) {
        String[] data = nfcData.split("###");
        return new ContactDetails(data[0], data[1], data[2]);
    }
}
