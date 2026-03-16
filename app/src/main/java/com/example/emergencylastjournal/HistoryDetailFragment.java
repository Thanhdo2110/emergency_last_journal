package com.example.emergencylastjournal;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.example.emergencylastjournal.data.db.AppDatabase;
import com.example.emergencylastjournal.data.entity.GpsLogEntity;
import com.example.emergencylastjournal.data.entity.SessionEntity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class HistoryDetailFragment extends Fragment implements OnMapReadyCallback {
    private MapView mapView;
    private GoogleMap googleMap;
    private int sessionId;
    private TextView tvRoute, tvTime, tvStatus, tvNote;
    private ImageView ivPhoto;
    private ImageButton btnBack;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            sessionId = getArguments().getInt("SESSION_ID");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history_detail, container, false);
        mapView = view.findViewById(R.id.historyMapView);
        tvRoute = view.findViewById(R.id.tvDetailRoute);
        tvTime = view.findViewById(R.id.tvDetailTime);
        tvStatus = view.findViewById(R.id.tvDetailStatus);
        tvNote = view.findViewById(R.id.tvDetailNote);
        ivPhoto = view.findViewById(R.id.ivDetailPhoto);
        btnBack = view.findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        loadSessionDetails();
        return view;
    }

    private void loadSessionDetails() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            SessionEntity session = db.sessionDao().getSessionById(sessionId);
            
            if (getActivity() != null && session != null) {
                getActivity().runOnUiThread(() -> {
                    tvRoute.setText(session.route);
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());
                    tvTime.setText("Bắt đầu: " + sdf.format(new Date(session.startedAt)));
                    tvStatus.setText("Trạng thái kết thúc: " + (session.outcome != null ? session.outcome : "Hoàn thành"));
                    
                    // Hiển thị ghi chú nếu có (Sử dụng trường route làm ví dụ hoặc route_note nếu bạn đã thêm)
                    if (session.route != null && !session.route.isEmpty()) {
                        tvNote.setText("Ghi chú: " + session.route);
                    } else {
                        tvNote.setText("Ghi chú: Không có ghi chú");
                    }
                    
                    if (session.photoPath != null) {
                        Glide.with(this).load(session.photoPath).into(ivPhoto);
                    }
                });
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        loadGpsLogs();
    }

    private void loadGpsLogs() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<GpsLogEntity> logs = db.sessionDao().getLogsForSessionSync(sessionId);
            
            if (getActivity() != null && logs != null && !logs.isEmpty()) {
                getActivity().runOnUiThread(() -> drawRoute(logs));
            }
        });
    }

    private void drawRoute(List<GpsLogEntity> logs) {
        List<LatLng> points = new ArrayList<>();
        for (GpsLogEntity log : logs) {
            points.add(new LatLng(log.latitude, log.longitude));
        }

        googleMap.addPolyline(new PolylineOptions()
                .addAll(points)
                .width(12)
                .color(Color.RED)
                .startCap(new RoundCap())
                .endCap(new RoundCap()));

        if (!points.isEmpty()) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), 15f));
        }
    }

    @Override public void onResume() { super.onResume(); mapView.onResume(); }
    @Override public void onPause() { super.onPause(); mapView.onPause(); }
    @Override public void onDestroy() { super.onDestroy(); if (mapView != null) mapView.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
}