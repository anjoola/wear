package com.anjoola.sharewear.util;

import android.content.Intent;

import com.anjoola.sharewear.MyLocationActivity;
import com.anjoola.sharewear.ShareWearActivity;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Handles actions from the Android Wear app.
 *
 * Courtesy of
 * https://gist.github.com/gabrielemariotti/117b05aad4db251f7534.
 */
public class SharingListenerService extends WearableListenerService {
    private static final String PATH_ON = "/wear_on";
    private static final String PATH_OFF = "/wear_off";

    @Override
    public void onMessageReceived(MessageEvent event) {
        // Received a message from the watch. Start intent for location sharing.
        if (event.getPath().equals(PATH_ON) || event.getPath().equals(PATH_OFF)) {
            Intent intent = new Intent(this, MyLocationActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            String extra = event.getPath().equals(PATH_ON) ?
                    ShareWearActivity.START_SHARING : ShareWearActivity.STOP_SHARING;
            intent.putExtra(extra, true);
            startActivity(intent);
        }
    }
}
