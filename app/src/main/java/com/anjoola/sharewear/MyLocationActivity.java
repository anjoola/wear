package com.anjoola.sharewear;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MyLocationActivity extends ShareWearActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_location_activity);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_location_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // TODO other action
            case R.id.action_sign_out:
                signOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}