package com.bignerdranch.android.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.bignerdranch.android.photogallery.model.QueryPreferences;
import com.bignerdranch.android.photogallery.service.PollJobService;
import com.bignerdranch.android.photogallery.service.PollService;

/**
 * Created by michaeltan on 2017/8/19.
 */

public class StartupReceiver extends BroadcastReceiver {
    private static final String TAG = "StartupReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Received broadcast intent: " + intent.getAction());

        boolean isOn = QueryPreferences.isAlarmOn(context);
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
            PollJobService.setServiceAlarm(context, isOn);
        } else {
            PollService.setServiceAlarm(context, isOn);
        }
    }
}
