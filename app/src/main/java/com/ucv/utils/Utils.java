package com.ucv.utils;


import android.util.Log;

import java.util.concurrent.TimeUnit;

public class Utils {

    public static String getTimeString(int millis){
        Log.d(Utils.class.getSimpleName(), "I'm on getTimeString() from Utils");
        if (millis < 0) {
            return "00:00";
        }
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        return String.format("%02d:%02d",
                minutes, computeSeconds(millis, minutes));
    }

    private static long computeSeconds(int millis, long minutes) {
        return TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes);
    }
}
