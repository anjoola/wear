package com.anjoola.sharewear;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

;

public class MainActivity extends Activity implements View.OnClickListener,
     GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String PATH_ON = "/wear_on";
    private static final String PATH_OFF = "/wear_off";

    // UI elements.
    private ImageView mIcon;
    private TextView mText;

    // Whether or not location sharing is currently on.
    private boolean locationSharingOn;

    // The mobile device this watch is connected to.
    private Node mDevice;
    GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // Connect the Google API client.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mText = (TextView) stub.findViewById(R.id.action);
                mIcon = (ImageView) stub.findViewById(R.id.location_icon);

                View shareAction = stub.findViewById(R.id.action_view);
                shareAction.setOnClickListener(MainActivity.this);
            }
        });

        locationSharingOn = false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.action_view) {
            locationShare();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        // Get the connected device.
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient)
            .setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                @Override
                public void onResult(NodeApi.GetConnectedNodesResult nodes) {
                    for (Node node : nodes.getNodes()) {
                        mDevice = node;
                    }
                }
            });
    }

    @Override
    public void onConnectionSuspended(int i) { }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) { }

    /**
     * Start or stop location sharing, depending on the current state. Also
     * update the UI.
     */
    private void locationShare() {

        // Location services turned off on phone. Cannot share location.
        //LocationServices.FusedLocationApi.getLocationAvailability().isLocationAvailable()

        // Update UI.
        if (!locationSharingOn) {
            mText.setText(R.string.sharing_off);
            mIcon.setImageResource(R.mipmap.action_share_off);
        }
        else {
            mText.setText(R.string.sharing_on);
            mIcon.setImageResource(R.mipmap.action_share_on);

        }
        locationSharingOn = !locationSharingOn;
        sendToDevice(locationSharingOn);

        // Show success confirmation.
        String success = locationSharingOn ?
                getString(R.string.sharing_on_success) :
                getString(R.string.sharing_off_success);
        Intent intent = new Intent(this, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                ConfirmationActivity.OPEN_ON_PHONE_ANIMATION);
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, success);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    /**
     * Send message to mobile device to start or stop location sharing.
     */
    private void sendToDevice(boolean on) {
        if (mDevice == null || mGoogleApiClient == null ||
                !mGoogleApiClient.isConnected())
            return;

        // Send a message to the mobile device to start a location sharing
        // intent.
        String path = on ? PATH_ON : PATH_OFF;
        Wearable.MessageApi.sendMessage(mGoogleApiClient, mDevice.getId(), path, null)
            .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(MessageApi.SendMessageResult result) {
                    // TODO handle failure?
                }
            });
    }
}
