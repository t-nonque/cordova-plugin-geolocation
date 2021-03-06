package com.castr.cordova.plugin;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import java.util.List;

/**
 * Created by freddy on 24/06/15.
 */
public class BuiltInGeolocation {

    public interface BuiltInLocationCallback {
        void onSuccess(Location location);
        void onError(String message);
    }

    private static final String TAG = "CdvGeolocationPlugin";

    /**
     * Built-in location manager.
     */
    private LocationManager mLocationManager;

    /**
     * Listener for new locations updates.
     */
    private LocationListener mLocationListener;

    /**
     * Basic constructor.
     * @param context A context for the {@link LocationManager}.
     */
    public BuiltInGeolocation(Context context) {
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    // -------------------------------------------------------------------------------------------
    // Built-in location service
    // -------------------------------------------------------------------------------------------
    public void getLocation(final BuiltInLocationCallback callback) {
        // Check first last known positions
        Log.v(TAG, "Checking last know positions.");
        Location lastBestLoc = getLastBestLocation();
        if (lastBestLoc != null) {
            Log.i(TAG, "Last location found.");
            callback.onSuccess(lastBestLoc);
            return;
        }

        // Use an update otherwise
        Log.v(TAG, "Requesting update.");
        Looper locLooper = Looper.getMainLooper();

        LocationListener locListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                callback.onSuccess(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Log.i(TAG, "Requesting new position from network.");
            mLocationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locListener, locLooper);
        }
        else if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.i(TAG, "Requesting new position from GPS.");
            mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locListener, locLooper);
        }
        else if (mLocationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
            callback.onSuccess(mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER));
        }
        else {
            Log.i(TAG, "No provider enabled.");
            callback.onError("No provider enabled.");
        }

    }

    // -------------------------------------------------------------------------------------------
    // Built-in location service
    // -------------------------------------------------------------------------------------------
    private Location getLastBestLocation() {
         int minDistance = 200;
         long minTime = System.currentTimeMillis() - (2 * 60 * 1000);

        return getLastBestLocation(minDistance, minTime);
    }

    /**
     * {@see http://android-developers.blogspot.fr/2011/06/deep-dive-into-location.html}
     * @param minDistance Minimum distance before we require a location update.
     * @param minTime Minimum time required between location updates.
     * @return The most accurate and / or timely previously detected location.
     */
    private Location getLastBestLocation(int minDistance, long minTime) {
        Location bestResult = null;
        float bestAccuracy = Float.MAX_VALUE;
        long bestTime = Long.MIN_VALUE;

        // Iterate through all the providers on the system, keeping
        // note of the most accurate result within the acceptable time limit.
        // If no result is found within maxTime, return the newest Location.
        List<String> matchingProviders = mLocationManager.getAllProviders();
        for (String provider: matchingProviders) {
            Location location = mLocationManager.getLastKnownLocation(provider);
            if (location != null) {
                float accuracy = location.getAccuracy();
                long time = location.getTime();

                if ((time > minTime && accuracy < bestAccuracy)) {
                    bestResult = location;
                    bestAccuracy = accuracy;
                    bestTime = time;
                }
                else if (time < minTime && bestAccuracy == Float.MAX_VALUE && time > bestTime) {
                    bestResult = location;
                    bestTime = time;
                }
            }
        }

        // If the best result is beyond the allowed time limit, or the accuracy of the
        // best result is wider than the acceptable maximum distance, request a single update.
        // This check simply implements the same conditions we set when requesting regular
        // location updates every [minTime] and [minDistance].
        if (mLocationListener != null && (bestTime < minTime || bestAccuracy > minDistance)) {

        }

        return bestResult;
    }
}
