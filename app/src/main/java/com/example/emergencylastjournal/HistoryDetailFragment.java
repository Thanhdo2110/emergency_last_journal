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
                Toast.makeText(getContext(), getString(R.string.no_location_data), Toast.LENGTH_SHORT).show();
            }
        });

        loadSessionDetails();
        loadLastCoordinates();
        updateStaticTexts(view);
        return view;
    }

    private void updateStaticTexts(View view) {
        ((TextView)view.findViewById(R.id.tvLabelEventLoc)).setText(R.string.event_location_label);
        ((TextView)view.findViewById(R.id.tvTapToViewMap)).setText(R.string.tap_to_view_map);
        ((TextView)view.findViewById(R.id.tvDetailTitleHeader)).setText(R.string.history_detail_title);
        btnBack.setContentDescription(getString(R.string.back));
    }

    private void loadSessionDetails() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            SessionEntity session = db.sessionDao().getSessionById(sessionId);
            
            if (getActivity() != null && session != null) {
                getActivity().runOnUiThread(() -> {
                    tvRoute.setText(session.route != null && !session.route.isEmpty() ? session.route : getString(R.string.emergency_status_msg));
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());
                    tvTime.setText(getString(R.string.time_label, sdf.format(new Date(session.startedAt))));
                    
                    String outcomeText = getString(R.string.filter_success);
                    if (session.outcome != null) {
                        switch (session.outcome) {
                            case "safe": outcomeText = getString(R.string.filter_safe); break;
                            case "emergency": outcomeText = getString(R.string.filter_emergency); break;
                            case "manual": outcomeText = getString(R.string.back_to_safe); break;
                            case "active": outcomeText = getString(R.string.filter_running); break;
                        }
                    }
                    tvStatus.setText(getString(R.string.status_label, outcomeText));
                    
                    if (session.route != null && !session.route.isEmpty()) {
                        tvNote.setText(getString(R.string.journal_note_label, session.route));
                    } else {
                        tvNote.setText(getString(R.string.journal_note_label, getString(R.string.emergency_triggered_msg)));
                    }

                    tvLabelPhoto.setText(R.string.journal_photo_label);

                    // Hiển thị ảnh nếu có
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
                        tvCoordinates.setText(R.string.no_location_data);
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
