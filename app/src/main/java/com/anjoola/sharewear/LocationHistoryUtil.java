package com.anjoola.sharewear;

import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

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

// TODO
public class LocationHistoryUtil {
    // JSON string used for requests to the Google Maps API.
    private String GOOGLE_MAPS_JSON_STRING =
            "http://maps.googleapis.com/maps/api/directions/json?origin=%f,%f" +
            "&destination=%f,%f&sensor=false&mode=walking&alternatives=true";

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
        // TODO if within some threshold, don't draw path
        String url = String.format(GOOGLE_MAPS_JSON_STRING,
                oldLoc.getLatitude(), oldLoc.getLongitude(),
                newLoc.getLatitude(), newLoc.getLongitude());
        DrawPathAsync task = new DrawPathAsync(url);
        task.execute();
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
                Log.e("------", "STUPID");
                LatLng source = list.get(z);
                LatLng dest = list.get(z + 1);
                mMap.addPolyline(new PolylineOptions()
                        .add(new LatLng(source.latitude, source.longitude), new LatLng(dest.latitude, dest.longitude))
                        .width(2)
                        .color(Color.BLUE).geodesic(true));
            }

        } catch (JSONException e) { }
    }

    // TODO
    private class DrawPathAsync extends AsyncTask<Void, Void, String> {
        String url;

        public DrawPathAsync(String url){
            this.url = url;
        }

        @Override
        protected String doInBackground(Void... params) {
            return getJsonFromUrl(url);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                drawPath(result);
            }
        }

        /**
         * TODO
         * @param url
         * @return
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
