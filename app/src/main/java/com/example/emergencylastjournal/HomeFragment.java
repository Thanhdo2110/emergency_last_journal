package com.example.emergencylastjournal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
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
    private MaterialCardView statusBadge, timerCard;
    private View homeRootLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        homeRootLayout = view.findViewById(R.id.homeRootLayout);
        tvStatus = view.findViewById(R.id.tvStatus);
        statusBadge = view.findViewById(R.id.statusBadge);
        timerCard = view.findViewById(R.id.timerCard);
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

        // Click vào ô thời gian sẽ sang màn hình bản đồ/theo dõi
        if (timerCard != null) {
            timerCard.setOnClickListener(v -> {
                if (TrackingForegroundService.currentState.getValue() != SessionState.IDLE) {
                    Navigation.findNavController(view).navigate(R.id.navigation_map);
                } else {
                    Toast.makeText(getContext(), "Hiện không có phiên bảo vệ nào đang chạy!", Toast.LENGTH_SHORT).show();
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
        if (tvStatus == null || statusBadge == null || homeRootLayout == null) return;

        int statusColor;
        int bgColor;
        String statusText;

        switch (state) {
            case ACTIVE:
                statusText = "TRẠNG THÁI: ĐANG THEO DÕI";
                statusColor = ContextCompat.getColor(requireContext(), R.color.primary);
                bgColor = ContextCompat.getColor(requireContext(), R.color.primary_bg);
                break;
            case WARNING:
                statusText = "CẢNH BÁO: CÒN DƯỚI 5 PHÚT!";
                statusColor = ContextCompat.getColor(requireContext(), R.color.history_orange);
                bgColor = ContextCompat.getColor(requireContext(), R.color.history_orange_bg);
                break;
            case URGENT:
            case EMERGENCY:
                statusText = "CẢNH BÁO: ĐÃ GỬI SOS!";
                statusColor = ContextCompat.getColor(requireContext(), R.color.alert_red);
                bgColor = ContextCompat.getColor(requireContext(), R.color.alert_red_bg);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "HỆ THỐNG ĐÃ GỬI TIN NHẮN SOS!", Toast.LENGTH_LONG).show();
                }
                break;
            default:
                statusText = getString(R.string.status_safe);
                statusColor = ContextCompat.getColor(requireContext(), R.color.status_green);
                bgColor = ContextCompat.getColor(requireContext(), R.color.bg_status_green_alpha);
                break;
        }

        tvStatus.setText(statusText);
        statusBadge.setCardBackgroundColor(statusColor);
        homeRootLayout.setBackgroundColor(bgColor);
    }
}
