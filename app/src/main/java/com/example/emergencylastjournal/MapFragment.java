package com.example.emergencylastjournal;

import android.content.Intent;
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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.example.emergencylastjournal.data.entity.GpsLogEntity;
import com.example.emergencylastjournal.data.entity.SessionEntity;
import com.example.emergencylastjournal.service.TrackingForegroundService;
import com.example.emergencylastjournal.viewmodel.MapViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {
    private MapView mapView;
    private GoogleMap googleMap;
    private MapViewModel viewModel;
    private SessionEntity currentActiveSession;
    private TextView tvActiveTimer;

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

        view.findViewById(R.id.btnSafeBack).setOnClickListener(v -> showSafeBackConfirmDialog());
        
        // Xử lý nút +10 Phút
        view.findViewById(R.id.btnExtendTimer).setOnClickListener(v -> extendProtectionTime());

        // Lắng nghe thời gian thực từ Service
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
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận An toàn")
                .setMessage("Bạn muốn kết thúc bảo vệ?")
                .setPositiveButton("Xác nhận", (dialog, which) -> finishCurrentSession())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void finishCurrentSession() {
        if (currentActiveSession != null && isAdded()) {
            // Set thời gian về 0 ngay lập tức trên UI thông qua LiveData
            TrackingForegroundService.timeLeftSeconds.postValue(0L);
            
            // Dừng service tracking
            requireContext().stopService(new Intent(requireContext(), TrackingForegroundService.class));
            
            // Cập nhật database và chuyển hướng
            viewModel.completeSession(currentActiveSession);
            Toast.makeText(getContext(), "Hoàn thành bảo vệ!", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).navigate(R.id.navigation_home);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        
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
            }
        });
    }

    private void drawRoute(List<GpsLogEntity> logs) {
        if (googleMap == null) return;
        googleMap.clear();
        List<LatLng> points = new ArrayList<>();
        for (GpsLogEntity log : logs) points.add(new LatLng(log.latitude, log.longitude));

        googleMap.addPolyline(new PolylineOptions()
                .addAll(points)
                .width(12)
                .color(Color.RED)
                .geodesic(true)
                .startCap(new RoundCap())
                .endCap(new RoundCap()));

        if (!points.isEmpty()) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(points.get(points.size() - 1), 15f));
        }
    }

    @Override public void onResume() { super.onResume(); if (mapView != null) mapView.onResume(); }
    @Override public void onPause() { super.onPause(); if (mapView != null) mapView.onPause(); }
    @Override public void onDestroy() { super.onDestroy(); if (mapView != null) mapView.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (mapView != null) mapView.onLowMemory(); }
}