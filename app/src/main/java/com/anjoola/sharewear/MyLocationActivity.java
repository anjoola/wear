package com.anjoola.sharewear;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v7.app.NotificationCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.anjoola.sharewear.util.KillNotificationService;
import com.anjoola.sharewear.util.LocationHistoryUtil;
import com.anjoola.sharewear.util.ServerConnection;
import com.anjoola.sharewear.util.ServerField;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

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
    private LocationManager mLocManager;
    private String mProvider;

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

    // Used for a persistent notification when location sharing starts.
    private Notification mNotification;
    private NotificationManager mNotificationManager;
    private ServiceConnection mConnection;
    private Intent mKillIntent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_location_activity);

        // Initialize objects, toasts, and dialogs.
        mLocationTitle = (TextView) findViewById(R.id.sharing_status);
        mLocationDetails = (TextView) findViewById(R.id.location_details);
        mLocationLatLng = (TextView) findViewById(R.id.location_latlng);
        mOldLocation = null;
        mApp = (ShareWearApplication) getApplication();
        mShareLocationDialog = null;
        mGpsDialog = null;
        mShareOnToast = null;
        mShareOffToast = null;
        mNotification = null;
        mConnection = null;
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Set up handler for floating action button.
        mFab = (android.support.design.widget.FloatingActionButton)
                findViewById(R.id.fab_share);
        mFab.setOnClickListener(this);

        // Set up handlers for getting location on map.
        mLocManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        mProvider = null;

        // Google Maps fragment.
        SupportMapFragment supportMapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.google_map);
        mMap = supportMapFragment.getMap();
        mMap.setMyLocationEnabled(true);

        mLocationHistoryUtil = new LocationHistoryUtil(mMap);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // Get location each time user focuses on app.
        if (hasFocus)
            checkGPSStatus();
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
    public void onDestroy() {
        super.onDestroy();

        // Turn location sharing off if not at this activity.
        turnLocationSharingOff();

        // TODO unbindService(mConnection);
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
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));

        // Update text fields.
        mLocationDetails.setText(formatLocationDetails(lat, lng));
        mLocationLatLng.setText(String.format("(%1$.3f, %2$.3f)", lat, lng));

        // Draw path between old and new locations.
        if (mOldLocation != null) {
            mLocationHistoryUtil.updatePath(mOldLocation, location);
        }
        mOldLocation = location;

        // Send new location to server.
        sendToServer(lat, lng);
    }

    @Override
    public void onStatusChanged(String mProvider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String mProvider) { }

    @Override
    public void onProviderDisabled(String mProvider) { }

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
     * Check the GPS status and prompt the user to turn it on if it is off.
     * Otherwise, get the current location. Also update the UI if needed
     * depending on location sharing status.
     */
    private void checkGPSStatus() {
        updateUI(mApp.isLocationSharingOn());

        // GPS is turned off.
        if (!mLocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            promptUserGPSEnable();
        }
        else {
            getLocation();
        }
    }

    /**
     * Clears all locations from server.
     */
    private void clearFromServer() {
        try {
            JSONObject json = new JSONObject();
            json.put(ServerField.COMMAND, ServerField.LOCATION_CLEAR);
            json.put(ServerField.USER_ID, mApp.getGcmToken());
            ServerConnection.doPost(json);
        }
        catch (JSONException e) { }
    }

    /**
     * Gets most recent location.
     */
    private void getLocation() {
        if (mProvider == null) {
            Criteria criteria = new Criteria();
            mProvider = mLocManager.getBestProvider(criteria, true);
        }
        Location location = mLocManager.getLastKnownLocation(mProvider);
        if (location != null) {
            onLocationChanged(location);
        }
    }

    /**
     * Prompt user to turn on GPS if they haven't done so already.
     */
    private void promptUserGPSEnable() {
        // Create the dialog if it hasn't been created yet.
        if (mGpsDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            // Set "Go to Settings" and "Cancel" buttons.
            builder.setPositiveButton(R.string.go_to_settings, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Go to GPS settings menu.
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Close location sharing. It cannot be used unless GPS
                    // is turned on.
                    finish();
                }
            });

            builder.setTitle(R.string.gps_prompt_title);
            builder.setMessage(R.string.gps_prompt);
            mGpsDialog = builder.create();
            mGpsDialog.setCancelable(false);
            mGpsDialog.setCanceledOnTouchOutside(false);
        }
        mGpsDialog.show();
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
                public void onClick(DialogInterface dialog, int id) {
                }
            });

            builder.setTitle(R.string.start_sharing_prompt_title);
            builder.setMessage(R.string.start_sharing_prompt);
            mShareLocationDialog = builder.create();
        }
        mShareLocationDialog.show();
    }

    /**
     * Sends location to server.
     * @param lat The latitude.
     * @param lng The longitude.
     */
    private void sendToServer(double lat, double lng) {
        try {
            JSONObject json = new JSONObject();
            json.put(ServerField.COMMAND, ServerField.LOCATION_ADD);
            json.put(ServerField.USER_ID, mApp.getGcmToken());
            json.put(ServerField.LATITUDE, lat);
            json.put(ServerField.LONGITUDE, lng);
            ServerConnection.doPost(json);
        }
        // If we could not send the update, ignore. Try again later with a new
        // location.
        catch (JSONException e) { }
    }

    /**
     * Turn location sharing off.
     */
    public void turnLocationSharingOff() {
        // Already turned off.
        if (!mApp.isLocationSharingOn()) return;

        updateUI(false);

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

        // Hide the notification.
        mNotificationManager.cancel(ShareWearApplication.NOTIFICATION_ID);
        stopService(mKillIntent);
        unbindService(mConnection);

        // Clear locations from server.
        clearFromServer();
    }

    /**
     * Turn location sharing on.
     */
    private void turnLocationSharingOn() {
        updateUI(true);

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

        // Show persistent notification.
        if (mConnection == null) {
            // Create notification.
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(this);
            builder.setSmallIcon(R.mipmap.ic_action_share);
            builder.setContentTitle(getString(R.string.notification_title));
            builder.setContentText(getString(R.string.notification_text));
            builder.setOngoing(true);

            // Add intent so clicking on the notification will go to the app.
            Intent resultIntent = new Intent(this, MyLocationActivity.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addParentStack(MyLocationActivity.class);

            // Make it so the activity starts at the top of the stack.
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent pendingIntent = stackBuilder.getPendingIntent(0,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);

            mNotification = builder.build();

            mConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName className,
                                               IBinder binder) {
                    mNotificationManager.notify(
                            ShareWearApplication.NOTIFICATION_ID, mNotification);
                }

                @Override
                public void onServiceDisconnected(ComponentName className) { }
            };
        }
        mKillIntent = new Intent(this, KillNotificationService.class);
        startService(mKillIntent);
        bindService(mKillIntent, mConnection, BIND_AUTO_CREATE);
    }

    /**
     * Update location sharing UI.
     *
     * @param on Whether or not location sharing is on.
     */
    private void updateUI(boolean on) {
        if (on) {
            mFab.setImageResource(R.mipmap.ic_action_location_off);
            mLocationTitle.setText(R.string.sharing_location);
        }
        else {
            mFab.setImageResource(R.mipmap.ic_action_share);
            mLocationTitle.setText(R.string.not_sharing_location);
            mMap.clear();
        }
    }
}
