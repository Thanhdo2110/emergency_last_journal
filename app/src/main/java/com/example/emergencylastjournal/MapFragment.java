package com.example.emergencylastjournal;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.example.emergencylastjournal.data.entity.GpsLogEntity;
import com.example.emergencylastjournal.data.entity.SessionEntity;
import com.example.emergencylastjournal.service.TrackingForegroundService;
import com.example.emergencylastjournal.viewmodel.MapViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {
    private MapView mapView;
    private GoogleMap googleMap;
    private MapViewModel viewModel;
    private SessionEntity currentActiveSession;
    private TextView tvActiveTimer;
    private boolean isFirstFix = true;
    private Marker userMarker;
    private FusedLocationProviderClient fusedLocationClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        mapView = view.findViewById(R.id.mapView);
        tvActiveTimer = view.findViewById(R.id.tvActiveTimer);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MapViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        view.findViewById(R.id.btnSafeBack).setOnClickListener(v -> showSafeBackConfirmDialog());
        view.findViewById(R.id.btnExtendTimer).setOnClickListener(v -> extendProtectionTime());

        TrackingForegroundService.timeLeftSeconds.observe(getViewLifecycleOwner(), totalSeconds -> {
            if (totalSeconds != null && tvActiveTimer != null) {
                long h = totalSeconds / 3600;
                long m = (totalSeconds % 3600) / 60;
                long s = totalSeconds % 60;
                tvActiveTimer.setText(String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", h, m, s));
            }
        });
    }

    private void extendProtectionTime() {
        Intent extendIntent = new Intent(requireContext(), TrackingForegroundService.class);
        extendIntent.setAction(TrackingForegroundService.ACTION_EXTEND_TIMER);
        requireContext().startForegroundService(extendIntent);
        Toast.makeText(getContext(), "Đã cộng thêm 10 phút bảo vệ", Toast.LENGTH_SHORT).show();
    }

    private void showSafeBackConfirmDialog() {
        if (getContext() == null) return;
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xác nhận An toàn")
                .setMessage("Bạn muốn kết thúc bảo vệ?")
                .setPositiveButton("Xác nhận", (d, which) -> finishCurrentSession())
                .setNegativeButton("Hủy", null)
                .create();
        dialog.show();
        
        // Cường hóa màu chữ để đảm bảo nhìn thấy được trên nền trắng
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#007A8A"));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#757575"));
    }

    private void finishCurrentSession() {
        if (currentActiveSession != null && isAdded()) {
            TrackingForegroundService.timeLeftSeconds.postValue(0L);
            requireContext().stopService(new Intent(requireContext(), TrackingForegroundService.class));
            viewModel.completeSession(currentActiveSession);
            Toast.makeText(getContext(), "Hoàn thành bảo vệ!", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).navigate(R.id.navigation_home);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
            
            // Lấy vị trí ngay lập tức để không bị hiển thị ở Mỹ khi vừa mở map
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null && isFirstFix) {
                    LatLng myPos = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPos, 16f));
                }
            });
        }
        
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        
        viewModel.getActiveSession().observe(getViewLifecycleOwner(), session -> {
            if (session != null) {
                currentActiveSession = session;
                observeGpsLogs(session.id);
            }
        });
    }

    private void observeGpsLogs(int sessionId) {
        viewModel.getGpsLogs(sessionId).observe(getViewLifecycleOwner(), logs -> {
            if (logs != null && !logs.isEmpty() && googleMap != null) {
                drawRoute(logs);
                updateUserMarker(logs.get(logs.size() - 1));
            }
        });
    }

    private void drawRoute(List<GpsLogEntity> logs) {
        List<LatLng> points = new ArrayList<>();
        for (GpsLogEntity log : logs) points.add(new LatLng(log.latitude, log.longitude));

        googleMap.addPolyline(new PolylineOptions()
                .addAll(points)
                .width(15)
                .color(Color.parseColor("#4285F4"))
                .geodesic(true)
                .startCap(new RoundCap())
                .endCap(new RoundCap()));
    }

    private void updateUserMarker(GpsLogEntity latestLog) {
        LatLng currentPos = new LatLng(latestLog.latitude, latestLog.longitude);
        
        if (userMarker == null) {
            userMarker = googleMap.addMarker(new MarkerOptions()
                    .position(currentPos)
                    .title("Vị trí thực tế")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .flat(true)
                    .anchor(0.5f, 0.5f));
        } else {
            userMarker.setPosition(currentPos);
        }

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(currentPos)
                .zoom(17f)
                .tilt(45)
                .build();

        if (isFirstFix) {
            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            isFirstFix = false;
        } else {
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 800, null);
        }
    }

    @Override public void onResume() { super.onResume(); if (mapView != null) mapView.onResume(); }
    @Override public void onPause() { super.onPause(); if (mapView != null) mapView.onPause(); }
    @Override public void onDestroy() { super.onDestroy(); if (mapView != null) mapView.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (mapView != null) mapView.onLowMemory(); }
}
