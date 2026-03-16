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
        // Cấu hình để lấy vị trí mượt và chính xác nhất như Google Maps
        return new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000) 
                .setMinUpdateDistanceMeters(2) 
                .build();
    }

    @SuppressLint("MissingPermission")
    public static void getLastLocation(Context context, OnSuccessListener<Location> listener) {
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(context);
        
        // Thay vì lấy vị trí cũ (có thể ở Mỹ), ta yêu cầu lấy vị trí THỰC TẾ HIỆN TẠI
        CurrentLocationRequest request = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(5000) // Chỉ chấp nhận vị trí mới trong vòng 5 giây
                .build();
                
        client.getCurrentLocation(request, null).addOnSuccessListener(listener);
    }
}