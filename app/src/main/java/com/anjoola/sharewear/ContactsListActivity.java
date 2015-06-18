package com.anjoola.sharewear;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class ContactsListActivity extends ShareWearActivity {
    // ListView to store all contacts.
    ListView mContactsList;

    // Adapter for mapping columns from cursor to ListView.
    SimpleCursorAdapter mAdapter;

    // Asynchronous task to retrieve and load ListView with contacts.
    ContactsListLoader mLoader;


    MatrixCursor mMatrixCursor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contacts_list_activity);

        // Set up contacts list view. Set adapter and scroll listener for
        // infinite scrolling.
        mContactsList = (ListView) findViewById(R.id.contacts_list);
        mContactsList.setOnScrollListener(new EndlessScrollListener());
        mAdapter = new SimpleCursorAdapter(getBaseContext(),
                R.layout.contacts_list_view_layout,
                null,
                new String[] { "name","photo","details"}, // TODO
                new int[] { R.id.name,R.id.photo,R.id.details}, 0);
        mContactsList.setAdapter(mAdapter);

        // TODO
        // The contacts from the contacts content provider is stored in this cursor
        mMatrixCursor = new MatrixCursor(new String[] { "_id","name","photo","details"} );

        // Set up asynchronous task and start it.
        mLoader = new ContactsListLoader();
        mLoader.execute();
    }

    /** An AsyncTask class to retrieve and load listview with contacts */
    private class ContactsListLoader extends AsyncTask<Void, Void, Cursor> {

        // URIs for retrieving contacts information.
        private final Uri CONTACTS_URI = ContactsContract.Contacts.CONTENT_URI;
        private final Uri DATA_URI = ContactsContract.Data.CONTENT_URI;

        // Cursor for retrieving all contacts.
        private Cursor cursor;

        public ContactsListLoader() {
            cursor = getContentResolver().query(CONTACTS_URI, null, null,
                    null, ContactsContract.Contacts.DISPLAY_NAME + " ASC ");
        }

        @Override
        protected Cursor doInBackground(Void... params) {
            int i = 0;
            if(cursor.moveToFirst()){
                do{
                    i++;
                    if ( i > 10) break;
                    long contactId = cursor.getLong(cursor.getColumnIndex("_ID"));

                    // Querying the table ContactsContract.Data to retrieve individual items like
                    // home phone, mobile phone, work email etc corresponding to each contact
                    Cursor dataCursor = getContentResolver().query(DATA_URI, null,
                            ContactsContract.Data.CONTACT_ID + "=" + contactId,
                            null, null);

                    String displayName="";
                    String nickName="";
                    String homePhone="";
                    String mobilePhone="";
                    String workPhone="";
                    String photoPath="" ;// R.drawable.blank;
                    byte[] photoByte=null;
                    String homeEmail="";
                    String workEmail="";
                    String companyName="";
                    String title="";

                    if(dataCursor.moveToFirst()){
                        // Getting Display Name
                        displayName = dataCursor.getString(dataCursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME ));
                        do{

                            // Getting NickName
                            if(dataCursor.getString(dataCursor.getColumnIndex("mimetype")).equals(ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE))
                                nickName = dataCursor.getString(dataCursor.getColumnIndex("data1"));

                            // Getting Phone numbers
                            if(dataCursor.getString(dataCursor.getColumnIndex("mimetype")).equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)){
                                switch(dataCursor.getInt(dataCursor.getColumnIndex("data2"))){
                                    case ContactsContract.CommonDataKinds.Phone.TYPE_HOME :
                                        homePhone = dataCursor.getString(dataCursor.getColumnIndex("data1"));
                                        break;
                                    case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE :
                                        mobilePhone = dataCursor.getString(dataCursor.getColumnIndex("data1"));
                                        break;
                                    case ContactsContract.CommonDataKinds.Phone.TYPE_WORK :
                                        workPhone = dataCursor.getString(dataCursor.getColumnIndex("data1"));
                                        break;
                                }
                            }

                            // Getting EMails
                            if(dataCursor.getString(dataCursor.getColumnIndex("mimetype")).equals(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE ) ) {
                                switch(dataCursor.getInt(dataCursor.getColumnIndex("data2"))){
                                    case ContactsContract.CommonDataKinds.Email.TYPE_HOME :
                                        homeEmail = dataCursor.getString(dataCursor.getColumnIndex("data1"));
                                        break;
                                    case ContactsContract.CommonDataKinds.Email.TYPE_WORK :
                                        workEmail = dataCursor.getString(dataCursor.getColumnIndex("data1"));
                                        break;
                                }
                            }

                            // Getting Organization details
                            if(dataCursor.getString(dataCursor.getColumnIndex("mimetype")).equals(ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)){
                                companyName = dataCursor.getString(dataCursor.getColumnIndex("data1"));
                                title = dataCursor.getString(dataCursor.getColumnIndex("data4"));
                            }

                        }while(dataCursor.moveToNext());
                        String details = "";

                        // Concatenating various information to single string
                        if(mobilePhone != null && !mobilePhone.equals("") )
                            details += "MobilePhone : " + mobilePhone + "\n";
                        if(workPhone != null && !workPhone.equals("") )
                            details += "WorkPhone : " + workPhone + "\n";
                        if(nickName != null && !nickName.equals("") )
                            details += "NickName : " + nickName + "\n";
                        if(homeEmail != null && !homeEmail.equals("") )
                            details += "HomeEmail : " + homeEmail + "\n";
                        if(workEmail != null && !workEmail.equals("") )
                            details += "WorkEmail : " + workEmail + "\n";
                        if(companyName != null && !companyName.equals("") )
                            details += "CompanyName : " + companyName + "\n";
                        if(title != null && !title.equals("") )
                            details += "Title : " + title + "\n";

                        // Adding id, display name, path to photo and other details to cursor
                        mMatrixCursor.addRow(new Object[]{ Long.toString(contactId),displayName,photoPath,details});
                    }
                }while(cursor.moveToNext());
            }
            return mMatrixCursor;
        }

        @Override
        protected void onPostExecute(Cursor result) {
            // Setting the cursor containing contacts to listview
            mAdapter.swapCursor(result);
        }
    }

    private class EndlessScrollListener implements AbsListView.OnScrollListener {

        private int visibleThreshold = 5;
        private int currentPage = 0;
        private int previousTotal = 0;
        private boolean loading = true;

        public EndlessScrollListener() {
        }
        public EndlessScrollListener(int visibleThreshold) {
            this.visibleThreshold = visibleThreshold;
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {
            if (loading) {
                if (totalItemCount > previousTotal) {
                    loading = false;
                    previousTotal = totalItemCount;
                    currentPage++;
                }
            }
            if (!loading && (totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {
                // I load the next page of gigs using a background task,
                // but you can call any function here.
                mLoader.execute();
                loading = true;
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) { }
    }
}
