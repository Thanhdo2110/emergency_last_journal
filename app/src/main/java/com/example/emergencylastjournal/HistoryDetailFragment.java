package com.example.emergencylastjournal;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.example.emergencylastjournal.data.db.AppDatabase;
import com.example.emergencylastjournal.data.entity.GpsLogEntity;
import com.example.emergencylastjournal.data.entity.SessionEntity;
import com.google.android.material.card.MaterialCardView;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class HistoryDetailFragment extends Fragment {
    private int sessionId;
    private TextView tvRoute, tvTime, tvStatus, tvNote, tvCoordinates;
    private ImageView ivPhoto;
    private TextView tvLabelPhoto;
    private ImageButton btnBack;
    private MaterialCardView cardCoordinates;
    private Double lastLat = null, lastLng = null;

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
        
        tvRoute = view.findViewById(R.id.tvDetailRoute);
        tvTime = view.findViewById(R.id.tvDetailTime);
        tvStatus = view.findViewById(R.id.tvDetailStatus);
        tvNote = view.findViewById(R.id.tvDetailNote);
        tvCoordinates = view.findViewById(R.id.tvCoordinates);
        ivPhoto = view.findViewById(R.id.ivDetailPhoto);
        tvLabelPhoto = view.findViewById(R.id.tvLabelPhoto);
        btnBack = view.findViewById(R.id.btnBack);
        cardCoordinates = view.findViewById(R.id.cardCoordinates);

        btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        
        cardCoordinates.setOnClickListener(v -> {
            if (lastLat != null && lastLng != null) {
                openGoogleMaps(lastLat, lastLng);
            } else {
                Toast.makeText(getContext(), "Không có dữ liệu tọa độ", Toast.LENGTH_SHORT).show();
            }
        });

        loadSessionDetails();
        loadLastCoordinates();
        return view;
    }

    private void loadSessionDetails() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            SessionEntity session = db.sessionDao().getSessionById(sessionId);
            
            if (getActivity() != null && session != null) {
                getActivity().runOnUiThread(() -> {
                    tvRoute.setText(session.route != null && !session.route.isEmpty() ? session.route : "SOS Khẩn cấp");
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());
                    tvTime.setText("Thời gian: " + sdf.format(new Date(session.startedAt)));
                    
                    String outcomeText = "Hoàn thành";
                    if (session.outcome != null) {
                        switch (session.outcome) {
                            case "safe": outcomeText = "An toàn"; break;
                            case "emergency": outcomeText = "Khẩn cấp (SOS)"; break;
                            case "manual": outcomeText = "Người dùng tự ngắt"; break;
                            case "active": outcomeText = "Đang diễn ra"; break;
                        }
                    }
                    tvStatus.setText("Trạng thái: " + outcomeText);
                    
                    if (session.route != null && !session.route.isEmpty()) {
                        tvNote.setText("Ghi chú: " + session.route);
                    } else {
                        tvNote.setText("Ghi chú: SOS khẩn cấp được kích hoạt trực tiếp.");
                    }

                    // Hiển thị ảnh nếu có (chỉ dành cho Nhật ký có hẹn giờ)
                    if (session.photoPath != null && !session.photoPath.isEmpty()) {
                        File imgFile = new File(session.photoPath);
                        if (imgFile.exists()) {
                            tvLabelPhoto.setVisibility(View.VISIBLE);
                            ivPhoto.setVisibility(View.VISIBLE);
                            Glide.with(this).load(imgFile).into(ivPhoto);
                        } else {
                            tvLabelPhoto.setVisibility(View.GONE);
                            ivPhoto.setVisibility(View.GONE);
                        }
                    } else {
                        tvLabelPhoto.setVisibility(View.GONE);
                        ivPhoto.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    private void loadLastCoordinates() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            GpsLogEntity lastGpsLog = db.gpsLogDao().getLastLogForSessionSync(sessionId);
            SessionEntity session = db.sessionDao().getSessionById(sessionId);

            Double bestLat = null;
            Double bestLng = null;

            if (lastGpsLog != null) {
                bestLat = lastGpsLog.latitude;
                bestLng = lastGpsLog.longitude;
            } else if (session != null && session.latitude != null) {
                bestLat = session.latitude;
                bestLng = session.longitude;
            }

            lastLat = bestLat;
            lastLng = bestLng;
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (lastLat != null && lastLng != null) {
                        tvCoordinates.setText(String.format(Locale.getDefault(), "%.6f, %.6f", lastLat, lastLng));
                    } else {
                        tvCoordinates.setText("Không xác định được tọa độ");
                    }
                });
            }
        });
    }

    private void openGoogleMaps(double lat, double lng) {
        String uri = String.format(Locale.ENGLISH, "google.navigation:q=%f,%f", lat, lng);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            String webUri = String.format(Locale.ENGLISH, "https://www.google.com/maps/search/?api=1&query=%f,%f", lat, lng);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webUri)));
        }
    }
}
