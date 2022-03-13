package com.atomie.configlogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.widget.Toast;

public class MyBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Toast.makeText(context, action, Toast.LENGTH_SHORT).show();

//        if (isAirplaneModeOn(context.getApplicationContext())) {
//            Toast.makeText(context, "AirPlane mode is on", Toast.LENGTH_SHORT).show();
//        } else {
//            Toast.makeText(context, "AirPlane mode is off", Toast.LENGTH_SHORT).show();
//        }
    }

    private static boolean isAirplaneModeOn(Context context) {
        return Settings.System.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }
}
