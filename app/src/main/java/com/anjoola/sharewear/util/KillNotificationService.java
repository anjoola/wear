package com.anjoola.sharewear.util;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.anjoola.sharewear.MyLocationActivity;

/**
 * Kills the app's notification if the app is also killed.
 */
public class KillNotificationService extends Service {

    public class KillBinder extends Binder {
        public final Service service;

        public KillBinder(Service service) {
            this.service = service;
        }

    }

    private final IBinder mBinder = new KillBinder(this);

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }
    @Override
    public void onCreate() {
        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.cancel(MyLocationActivity.NOTIFICATION_ID);
    }
}
