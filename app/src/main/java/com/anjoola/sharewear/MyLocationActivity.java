package com.anjoola.sharewear;

import android.graphics.Color;
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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MyLocationActivity extends ShareWearActivity implements
        LocationListener, View.OnClickListener {
    // Floating action button for sharing location.
    private android.support.design.widget.FloatingActionButton mFab;

    // Google Map to display current location.
    private GoogleMap mMap;
    private TextView mLocationDetails;
    private TextView mLocationLatLng;

    // TODO
    private final String GOOGLE_MAPS_JSON_STRING =
            "http://maps.googleapis.com/maps/api/directions/json?origin=$1,$2" +
            "&destination=$3,$4&sensor=false&mode=driving&alternatives=true";

    // Minimum update time for location updates.
    private final long UPDATE_TIME = 20000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_location_activity);

        mLocationDetails = (TextView) findViewById(R.id.location_details);
        mLocationLatLng = (TextView) findViewById(R.id.location_latlng);

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
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        LatLng latLng = new LatLng(lat, lng);

        mMap.addMarker(new MarkerOptions().position(latLng));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

        // Update text fields.
        mLocationDetails.setText(formatLocationDetails(lat, lng));
        mLocationLatLng.setText(String.format("(%1$.3f, %2$.3f)", lat, lng));
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
     * Decodes a string containing the details for drawing a polyline.
     *
     * @param encoded The encoded string for a polyline.
     * @return List of points necessary to draw the line.
     */
    private List<LatLng> decodePoly(String encoded) {
        // TODO comment
        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng( (((double) lat / 1E5)),
                    (((double) lng / 1E5) ));
            poly.add(p);
        }

        return poly;
    }

    /**
     * Draws a path between two points, based on input from the Google API.
     *
     * @param result String returned from the Google Directions API.
     */
    public void drawPath(String result) {
        try {
            // Transform the string into a JSON object.
            final JSONObject json = new JSONObject(result);
            JSONArray routeArray = json.getJSONArray("routes");
            JSONObject routes = routeArray.getJSONObject(0);
            JSONObject polylines = routes.getJSONObject("overview_polyline");
            String encodedString = polylines.getString("points");
            List<LatLng> list = decodePoly(encodedString);

            // Loop through each point to draw.
            for (int z = 0; z < list.size() - 1; z++) {
                LatLng source = list.get(z);
                LatLng dest = list.get(z + 1);
                mMap.addPolyline(new PolylineOptions()
                        .add(source, dest)
                        //.add(new LatLng(source.latitude, source.longitude), new LatLng(dest.latitude, dest.longitude))
                        .width(2)
                        .color(Color.BLUE).geodesic(true));
            }

        } catch (JSONException e) { }
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

    private String getGoogleMapsJsonUrl(double sourceLat, double sourceLng,
                                        double destLat, double destLng) {

        return String.format(GOOGLE_MAPS_JSON_STRING, sourceLat, sourceLng,
                destLat, destLng).toString();
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
