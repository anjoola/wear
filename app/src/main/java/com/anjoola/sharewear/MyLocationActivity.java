package com.anjoola.sharewear;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
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

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Displays user's current location and location history. Allows user to
 * initiate location sharing with their contacts.
 */
public class MyLocationActivity extends ShareWearActivity implements
        LocationListener, View.OnClickListener {
    // Floating action button for sharing location.
    private android.support.design.widget.FloatingActionButton mFab;

    // Handlers for getting location.
    LocationManager mLocManager;
    String mProvider;

    // Google Map to display current location.
    private GoogleMap mMap;
    private TextView mLocationTitle;
    private TextView mLocationDetails;
    private TextView mLocationLatLng;

    // Previous location.
    private Location mOldLocation;

    // Minimum update time for location updates.
    private final long UPDATE_TIME = 30000;
    private final long SLOW_UPDATE_TIME = 300000;

    // Utility for drawing location history on the map.
    private LocationHistoryUtil mLocationHistoryUtil;

    // Application for global state.
    private ShareWearApplication mApp;

    // Prompts.
    private AlertDialog mShareLocationDialog, mGpsDialog;
    private Toast mShareOnToast, mShareOffToast;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_location_activity);

        // Initialize objects.
        mLocationTitle = (TextView) findViewById(R.id.sharing_status);
        mLocationDetails = (TextView) findViewById(R.id.location_details);
        mLocationLatLng = (TextView) findViewById(R.id.location_latlng);
        mOldLocation = null;
        mApp = (ShareWearApplication) getApplication();
        mShareLocationDialog = null;
        mGpsDialog = null;
        mShareOnToast = null;
        mShareOffToast = null;

        // Set up handler for floating action button.
        mFab = (android.support.design.widget.FloatingActionButton)
                findViewById(R.id.fab_share);
        mFab.setOnClickListener(this);

        // Set up handlers for getting location on map.
        // TODO maybe this throws an error?
        mLocManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        mProvider = mLocManager.getBestProvider(criteria, true);

        // Google Maps fragment.
        SupportMapFragment supportMapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.google_map);
        mMap = supportMapFragment.getMap();
        mMap.setMyLocationEnabled(true);

        mLocationHistoryUtil = new LocationHistoryUtil(mMap);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Get location again each time they open the app.
        getLocation();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fab_share) {
            // Location sharing is on, turn it off.
            if (mApp.isLocationSharingOn())
                turnLocationSharingOff();

            // Location sharing is off, prompt the user to see if they want it
            // turned on.
            else
                promptUserShareLocation();
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
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        LatLng latLng = new LatLng(lat, lng);

        // TODO marker
        //mMap.addMarker(new MarkerOptions().position(latLng));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        //mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

        // Update text fields.
        mLocationDetails.setText(formatLocationDetails(lat, lng));
        mLocationLatLng.setText(String.format("(%1$.3f, %2$.3f)", lat, lng));

        // Draw path between old and new locations.
        if (mOldLocation != null) {
            mLocationHistoryUtil.updatePath(mOldLocation, location);
        }
        mOldLocation = location;
    }

    @Override
    public void onStatusChanged(String mProvider, int status, Bundle extras) {
        // TODO
    }

    @Override
    public void onProviderEnabled(String mProvider) {
        // TODO
    }

    @Override
    public void onProviderDisabled(String mProvider) {
        // TODO warning about GPS disabled
    }

    /**
     * Formats the latitude and longitude into the following:
     *    City Name, Region/State Name
     *
     * @param lat The latitude as a double.
     * @param lng The longitude as a double.
     * @return A string containing the format.
     */
    private String formatLocationDetails(double lat, double lng) {
        StringBuilder string = new StringBuilder();
        Geocoder gcd = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> addresses = gcd.getFromLocation(lat, lng, 1);
            if (addresses.size() > 0) {
                Address addr = addresses.get(0);
                if (addr.getThoroughfare() != null) {
                    string.append(addr.getThoroughfare() + ", ");
                }
                string.append(addr.getLocality());

                if (addr.getAdminArea() != null) {
                    string.append(", " + addr.getAdminArea());
                }
            }
        } catch (IOException e) { }

        return string.toString();
    }

    /**
     * Gets most recent location, or prompts user to turn on GPS if it is
     * turned off.
     */
    private void getLocation() {
        // Get most recent location.
        Location location = mLocManager.getLastKnownLocation(mProvider);

        // Could not get location. Is GPS turned off?
        if (location == null) {
            // TODO tell user GPS is turned off
            Toast.makeText(this, "OOPS", Toast.LENGTH_SHORT).show();
        }
        else {
            onLocationChanged(location);
        }
        mMap.animateCamera(CameraUpdateFactory.zoomTo(14));
    }

    /**
     * Prompt the user about location sharing (do they want to turn it on).
     * If so, turn on location sharing. If not, do nothing.
     */
    private void promptUserShareLocation() {
        // Create the dialog if it hasn't been created yet.
        if (mShareLocationDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            // Set "Share" and "Cancel" buttons.
            builder.setPositiveButton(R.string.share, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    turnLocationSharingOn();
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) { }
            });

            builder.setTitle(R.string.start_sharing_prompt_title);
            builder.setMessage(R.string.start_sharing_prompt);
            mShareLocationDialog = builder.create();
        }
        mShareLocationDialog.show();
    }

    /**
     * Turn location sharing off.
     */
    private void turnLocationSharingOff() {
        // Update UI.
        mFab.setImageResource(R.mipmap.ic_action_share);
        mLocationTitle.setText(R.string.not_sharing_location);
        mMap.clear();

        // Change to lower location-update frequency.
        mApp.setLocationSharingOn(false);
        mLocManager.removeUpdates(this);
        mLocManager.requestLocationUpdates(mProvider, SLOW_UPDATE_TIME, 0, this);

        // Show toast.
        if (mShareOffToast == null) {
            mShareOffToast = Toast.makeText(this, R.string.sharing_off_toast,
                    Toast.LENGTH_SHORT);
        }
        mShareOffToast.show();
    }

    /**
     * Turn location sharing on.
     */
    private void turnLocationSharingOn() {
        // Update UI.
        mFab.setImageResource(R.mipmap.ic_action_location_off);
        mLocationTitle.setText(R.string.sharing_location);

        // Change to higher location-update frequency.
        mApp.setLocationSharingOn(true);
        mLocManager.removeUpdates(this);
        mLocManager.requestLocationUpdates(mProvider, UPDATE_TIME, 0, this);
        getLocation();

        // Show toast.
        if (mShareOnToast == null) {
            mShareOnToast = Toast.makeText(this, R.string.sharing_on_toast,
                    Toast.LENGTH_SHORT);
        }
        mShareOnToast.show();
    }
}
