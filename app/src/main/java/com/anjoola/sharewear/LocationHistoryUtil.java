package com.anjoola.sharewear;

import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for displaying location history on a map.
 */
public class LocationHistoryUtil {
    // Color of line to draw on map.
    private int LINE_COLOR = Color.BLUE;

    // Width of line to draw on map.
    private int LINE_WIDTH = 20;

    // Threshold distance (in km) for drawing a line.
    private double DRAWING_THRESHOLD = 0.1;

    // Corresponding Google Map to draw paths on.
    private GoogleMap mMap;

    public LocationHistoryUtil(GoogleMap map) {
        mMap = map;
    }

    /**
     * Update the map by drawing a path from the old location to the new
     * location. Doesn't draw a path if the distance moved is not past a
     * certain threshold.
     *
     * @param oldLoc Old location.
     * @param newLoc New location.
     */
    public void updatePath(Location oldLoc, Location newLoc) {
        // Don't draw path if distance isn't great enough.
        if (!isWithinThreshold(oldLoc, newLoc)) {
            return;
        }

        // JSON string used for requests to the Google Maps API.
        final String GOOGLE_MAPS_JSON_STRING =
                "http://maps.googleapis.com/maps/api/directions/json" +
                "?origin=%f,%f&destination=%f,%f&sensor=false&mode=walking" +
                "&alternatives=true";
        String url = String.format(GOOGLE_MAPS_JSON_STRING,
                oldLoc.getLatitude(), oldLoc.getLongitude(),
                newLoc.getLatitude(), newLoc.getLongitude());
        DrawPathAsync task = new DrawPathAsync(url, oldLoc, newLoc);
        task.execute();
    }

    /**
     * Checks to see if the distance between two locations is big enough for
     * a line to be drawn between them. Uses the Haversine formula (which tends
     * to overestimate trans-polar distances and underestimates trans-equatorial
     * distances.
     *
     * @param oldLoc The old location.
     * @param newLoc The new location.
     * @return Whether or not the distance is great enough.
     */
    private boolean isWithinThreshold(Location oldLoc, Location newLoc) {
        final double RADIUS_OF_EARTH = 6367.445;

        double dLng = newLoc.getLongitude() - oldLoc.getLongitude();
        double dLat = newLoc.getLatitude() - oldLoc.getLatitude();

        double sinLat = Math.sin(dLat / 2);
        sinLat *= sinLat;
        double sinLng = Math.sin(dLng / 2);
        sinLng *= sinLng;
        double a = sinLat + Math.cos(oldLoc.getLatitude()) *
                Math.cos(newLoc.getLatitude()) * sinLng;
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = RADIUS_OF_EARTH * c;

        return distance >= DRAWING_THRESHOLD;
    }

    /**
     * Decodes a string containing the details for drawing a polyline.
     *
     * @param encoded The encoded string for a polyline.
     * @return List of points necessary to draw the line.
     */
    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        double lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            // Decode latitude.
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dLat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dLat;

            // Decode longitude.
            shift = 0; result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dLng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dLng;

            LatLng p = new LatLng(lat / 1E5, lng / 1E5);
            poly.add(p);
        }

        return poly;
    }

    /**
     * Draws a path between two points, based on input from the Google API.
     *
     * @param result String returned from the Google Directions API.
     * @param oldLoc Old location.
     * @param newLoc New location.
     */
    public void drawPath(String result, Location oldLoc, Location newLoc) {
        boolean drewPath = false;
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
                       // .add(new LatLng(source.latitude, source.longitude), new LatLng(dest.latitude, dest.longitude))
                        .width(LINE_WIDTH)
                        .color(LINE_COLOR).geodesic(true));
                drewPath = true;
            }
        } catch (JSONException e) { }

        // If Google Maps could not draw a path, we draw our own.
        if (!drewPath) {
            LatLng source = new LatLng(oldLoc.getLatitude(), oldLoc.getLongitude());
            LatLng dest = new LatLng(newLoc.getLatitude(), newLoc.getLongitude());
            mMap.addPolyline(new PolylineOptions().add(source, dest)
                    .width(LINE_WIDTH).color(LINE_COLOR).geodesic(true));
        }
    }

    /**
     * Asynchronously draw paths on the map.
     */
    private class DrawPathAsync extends AsyncTask<Void, Void, String> {
        Location oldLoc, newLoc;
        String url;

        public DrawPathAsync(String url, Location oldLoc, Location newLoc){
            this.url = url;
            this.oldLoc = oldLoc;
            this.newLoc = newLoc;
        }

        @Override
        protected String doInBackground(Void... params) {
            return getJsonFromUrl(url);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                drawPath(result, oldLoc, newLoc);
            }
        }

        /**
         * Get JSON results from the Google Maps API.
         *
         * @param url URL to query.
         * @return String containing the JSON results.
         */
        private String getJsonFromUrl(String url) {
            InputStream inputStream = null;
            StringBuilder json = new StringBuilder();

            // Make HTTP request to the Google API.
            try {
                DefaultHttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost(url);

                HttpResponse httpResponse = httpClient.execute(httpPost);
                HttpEntity httpEntity = httpResponse.getEntity();
                inputStream = httpEntity.getContent();
            } catch (Exception e) { }

            // Read results and create JSON string.
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, "iso-8859-1"), 8);
                String line = null;
                while ((line = reader.readLine()) != null) {
                    json.append(line + "\n");
                }
                inputStream.close();
            } catch (Exception e) { }

            return json.toString();
        }
    }
}
