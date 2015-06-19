package com.anjoola.sharewear;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MyLocationActivity extends ShareWearActivity implements
        LocationListener, View.OnClickListener {
    // Floating action button for sharing location.
    private android.support.design.widget.FloatingActionButton mFab;

    // Google Map to display current location.
    private GoogleMap mMap;

    // Minimum update time for location updates.
    private final long UPDATE_TIME = 20000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_location_activity);

        // Set up handler for floating action button.
        mFab = (android.support.design.widget.FloatingActionButton)
                findViewById(R.id.fab_share);
        mFab.setOnClickListener(this);

        // Set up handlers for getting location on map.
        SupportMapFragment supportMapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.google_map);
        mMap = supportMapFragment.getMap();
        mMap.setMyLocationEnabled(true);

        // Get most recent location.
        getLocation();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fab_share) {
            // TODO
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_location_menu, menu);
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
    public void onLocationChanged(Location location) {
        // Get updated location.
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        LatLng latLng = new LatLng(latitude, longitude);

        TextView locationTv = (TextView) findViewById(R.id.latlongLocation);
        mMap.addMarker(new MarkerOptions().position(latLng));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        locationTv.setText("Latitude:" + latitude + ", Longitude:" + longitude);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    /**
     * Gets most recent location, or prompts user to turn on GPS if it is
     * turned off.
     */
    private void getLocation() {
        LocationManager locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, true);

        // Get most recent location.
        Location location = locationManager.getLastKnownLocation(provider);

        // Could not get location. Is GPS turned off?
        if (location == null) {
            // TODO tell user GPS is turned off
            Toast.makeText(this, "OOPS", Toast.LENGTH_SHORT).show();
        }
        else {
            onLocationChanged(location);
        }
        locationManager.requestLocationUpdates(provider, UPDATE_TIME, 0, this);
    }
}
