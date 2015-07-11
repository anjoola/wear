package com.anjoola.sharewear.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.anjoola.sharewear.MyLocationActivity;
import com.anjoola.sharewear.R;
import com.anjoola.sharewear.ShareWearApplication;
import com.google.android.gms.gcm.GcmListenerService;

/**
 * Listens for notifications from the cloud server.
 */
public class ShareWearGcmListenerService extends GcmListenerService {
    @Override
    public void onMessageReceived(String from, Bundle data) {
        String contact = data.getString("contact_name");
        sendNotification(contact + " " + getString(R.string.request_message));
    }

    /**
     * Create and show a notification for a location request.
     *
     * @param message Message to send.
     */
    private void sendNotification(String message) {
        // Start the location sharing activity.
        Intent intent = new Intent(this, MyLocationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        // Build notification.
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_action_share)
                .setContentTitle(getString(R.string.request_title))
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(sound)
                .setContentIntent(pendingIntent);

        // Show notification.
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(ShareWearApplication.NOTIFICATION_ID,
                builder.build());
    }
}
