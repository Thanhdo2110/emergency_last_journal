package com.example.emergencylastjournal.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;

public class LocationHelper {
    public static LocationRequest createLocationRequest() {
        return new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000) 
                .setMinUpdateDistanceMeters(2) 
                .build();
    }

    @SuppressLint("MissingPermission")
    public static void getLastLocation(Context context, OnSuccessListener<Location> listener) {
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(context);
        
        CurrentLocationRequest request = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(10000) // Chấp nhận vị trí trong vòng 10 giây
                .build();
                
        client.getCurrentLocation(request, null).addOnSuccessListener(location -> {
            if (location != null) {
                listener.onSuccess(location);
            } else {
                // FALLBACK: Nếu không lấy được vị trí mới (do đang ở trong nhà...), lấy vị trí cuối cùng được lưu
                client.getLastLocation().addOnSuccessListener(listener);
            }
        });
    }
}