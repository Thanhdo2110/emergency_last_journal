package com.example.emergencylastjournal.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;

public class LocationHelper {
    public static LocationRequest createLocationRequest() {
        return new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30000) // 30 giây cập nhật 1 lần
                .setMinUpdateDistanceMeters(10)
                .build();
    }

    @SuppressLint("MissingPermission")
    public static void getLastLocation(Context context, OnSuccessListener<Location> listener) {
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(context);
        client.getLastLocation().addOnSuccessListener(listener);
    }
}