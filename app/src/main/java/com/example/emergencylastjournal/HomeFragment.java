package com.example.emergencylastjournal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.emergencylastjournal.data.entity.SessionState;
import com.example.emergencylastjournal.service.TrackingForegroundService;
import com.google.android.material.card.MaterialCardView;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private TextView tvStatus, tvTimerH, tvTimerM, tvTimerS;
    private MaterialCardView statusBadge;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        tvStatus = view.findViewById(R.id.tvStatus);
        statusBadge = view.findViewById(R.id.statusBadge);
        tvTimerH = view.findViewById(R.id.tvTimerH);
        tvTimerM = view.findViewById(R.id.tvTimerM);
        tvTimerS = view.findViewById(R.id.tvTimerS);

        // Lắng nghe trạng thái bảo vệ để cập nhật UI
        if (TrackingForegroundService.currentState != null) {
            TrackingForegroundService.currentState.observe(getViewLifecycleOwner(), state -> {
                if (state != null) updateUIByState(state);
            });
        }

        // Lắng nghe bộ đếm giờ thực
        if (TrackingForegroundService.timeLeftSeconds != null) {
            TrackingForegroundService.timeLeftSeconds.observe(getViewLifecycleOwner(), totalSeconds -> {
                if (totalSeconds != null && totalSeconds > 0) {
                    long h = totalSeconds / 3600;
                    long m = (totalSeconds % 3600) / 60;
                    long s = totalSeconds % 60;
                    
                    if (tvTimerH != null) tvTimerH.setText(String.format(Locale.getDefault(), "%02d", h));
                    if (tvTimerM != null) tvTimerM.setText(String.format(Locale.getDefault(), "%02d", m));
                    if (tvTimerS != null) tvTimerS.setText(String.format(Locale.getDefault(), "%02d", s));
                } else {
                    resetTimerDisplay();
                }
            });
        }

        setupNavigation(view);
    }

    private void resetTimerDisplay() {
        if (tvTimerH != null) tvTimerH.setText("00");
        if (tvTimerM != null) tvTimerM.setText("00");
        if (tvTimerS != null) tvTimerS.setText("00");
    }

    private void setupNavigation(View view) {
        // Kết nối các nút bấm với Action điều hướng trong NavGraph
        View btnStartNow = view.findViewById(R.id.btnStartNow);
        View btnViewHistory = view.findViewById(R.id.btnViewHistory);
        View btnManageContacts = view.findViewById(R.id.btnManageContacts);

        if (btnStartNow != null) {
            btnStartNow.setOnClickListener(v -> 
                Navigation.findNavController(view).navigate(R.id.action_home_to_createSession));
        }
        
        if (btnViewHistory != null) {
            btnViewHistory.setOnClickListener(v -> 
                Navigation.findNavController(view).navigate(R.id.action_home_to_history));
        }
            
        if (btnManageContacts != null) {
            btnManageContacts.setOnClickListener(v -> 
                Navigation.findNavController(view).navigate(R.id.action_home_to_contacts));
        }
    }

    private void updateUIByState(SessionState state) {
        if (tvStatus == null || statusBadge == null) return;

        switch (state) {
            case ACTIVE:
                tvStatus.setText("TRẠNG THÁI: ĐANG THEO DÕI");
                statusBadge.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary));
                break;
            case WARNING:
                tvStatus.setText("CẢNH BÁO: CÒN DƯỚI 5 PHÚT!");
                statusBadge.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.history_orange));
                break;
            case URGENT:
                tvStatus.setText("CẤP BÁCH: ĐANG GỬI THÔNG TIN!");
                statusBadge.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.alert_red));
                break;
            default:
                tvStatus.setText(getString(R.string.status_safe));
                statusBadge.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.status_green));
                break;
        }
    }
}