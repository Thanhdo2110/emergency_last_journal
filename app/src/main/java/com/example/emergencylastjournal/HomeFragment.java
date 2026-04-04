package com.example.emergencylastjournal;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.emergencylastjournal.data.db.AppDatabase;
import com.example.emergencylastjournal.data.entity.ContactEntity;
import com.example.emergencylastjournal.data.entity.SessionEntity;
import com.example.emergencylastjournal.data.entity.SessionState;
import com.example.emergencylastjournal.service.TrackingForegroundService;
import com.example.emergencylastjournal.util.EmailHelper;
import com.example.emergencylastjournal.util.SmsHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private TextView tvStatus, tvTimerH, tvTimerM, tvTimerS;
    private MaterialCardView statusBadge, timerCard, cardEmergency;
    private View homeRootLayout;
    private FusedLocationProviderClient fusedLocationClient;
    
    // Biến static để theo dõi việc hiển thị thông báo trong phiên chạy app
    private static boolean hasShownContactWarningThisSession = false;

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
        cardEmergency = view.findViewById(R.id.cardEmergency);
        tvTimerH = view.findViewById(R.id.tvTimerH);
        tvTimerM = view.findViewById(R.id.tvTimerM);
        tvTimerS = view.findViewById(R.id.tvTimerS);
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

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
                    Toast.makeText(getContext(), getString(R.string.no_active_session), Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Click vào nút Tình trạng cấp bách
        if (cardEmergency != null) {
            cardEmergency.setOnClickListener(v -> {
                checkAndSendEmergency();
            });
        }

        setupNavigation(view);
        
        // Chỉ kiểm tra và hiện thông báo khi lần đầu vào app (trong phiên chạy này)
        if (!hasShownContactWarningThisSession) {
            checkContactsOnEntry();
        }
    }

    private void checkContactsOnEntry() {
        Context context = getContext();
        if (context == null) return;
        
        AppDatabase db = AppDatabase.getInstance(context);
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ContactEntity> contacts = db.contactDao().getAllContactsSync();
            new Handler(Looper.getMainLooper()).post(() -> {
                if (contacts == null || contacts.isEmpty()) {
                    showNoContactsWarning();
                    hasShownContactWarningThisSession = true; // Đánh dấu đã hiện một lần
                }
            });
        });
    }

    private void showNoContactsWarning() {
        if (!isAdded()) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.need_setup_contacts)
                .setMessage(R.string.no_contacts_msg)
                .setCancelable(false)
                .setPositiveButton(R.string.add_now, (dialog, which) -> {
                    try {
                        NavController navController = Navigation.findNavController(requireView());
                        navController.navigate(R.id.action_home_to_contacts);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .setNegativeButton(R.string.later, (dialog, which) -> {
                    // Người dùng chọn để sau, không làm gì cả, biến flag đã được set để không hiện lại
                })
                .show();
    }

    private void checkAndSendEmergency() {
        Context context = getContext();
        if (context == null) return;

        // BƯỚC 1: KIỂM TRA QUYỀN VỊ TRÍ
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
            return;
        }

        AppDatabase db = AppDatabase.getInstance(context);
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ContactEntity> contacts = db.contactDao().getEmergencyContactsSync();
            
            new Handler(Looper.getMainLooper()).post(() -> {
                if (contacts == null || contacts.isEmpty()) {
                    Toast.makeText(context, getString(R.string.no_contacts_sos_msg), Toast.LENGTH_LONG).show();
                    return;
                }

                // Kiểm tra xem có người thân nào có email chưa
                boolean hasEmail = false;
                for (ContactEntity contact : contacts) {
                    if (contact.email != null && !contact.email.isEmpty()) {
                        hasEmail = true;
                        break;
                    }
                }

                if (!hasEmail) {
                    // Yêu cầu nhập email cho người thân đầu tiên
                    showEmailInputDialog(contacts.get(0));
                } else {
                    performEmergencyActions();
                }
            });
        });
    }

    private void requestLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Đã cấp quyền, tiếp tục gửi SOS
                checkAndSendEmergency();
            } else {
                Toast.makeText(getContext(), "Ứng dụng cần quyền vị trí để gửi SOS chính xác cho người thân.", Toast.LENGTH_LONG).show();
                // Vẫn cho phép gửi SOS nhưng cảnh báo có thể không có vị trí
                performEmergencyActions();
            }
        }
    }

    private void showEmailInputDialog(ContactEntity contact) {
        EditText etEmail = new EditText(getContext());
        etEmail.setHint("Email (e.g., abc@gmail.com)");
        etEmail.setPadding(60, 40, 60, 40);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.setup_email_sos)
                .setMessage(getString(R.string.input_email_msg, contact.name))
                .setView(etEmail)
                .setCancelable(false)
                .setPositiveButton(R.string.send_sos, (dialog, which) -> {
                    String email = etEmail.getText().toString().trim();
                    if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(getContext(), getString(R.string.invalid_email), Toast.LENGTH_SHORT).show();
                        showEmailInputDialog(contact); // Hiện lại dialog nếu sai
                    } else {
                        // Cập nhật email vào DB và gửi SOS
                        updateContactEmailAndSend(contact, email);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateContactEmailAndSend(ContactEntity contact, String email) {
        contact.email = email;
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase.getInstance(requireContext()).contactDao().update(contact);
            new Handler(Looper.getMainLooper()).post(this::performEmergencyActions);
        });
    }

    @SuppressLint("MissingPermission")
    private void performEmergencyActions() {
        Toast.makeText(getContext(), getString(R.string.determining_location), Toast.LENGTH_SHORT).show();
        
        // Lấy vị trí hiện tại trước khi lưu lịch sử
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener(location -> {
                Double lat = (location != null) ? location.getLatitude() : null;
                Double lng = (location != null) ? location.getLongitude() : null;
                
                // Gửi SMS
                SmsHelper.sendEmergencyAlert(requireContext(), -1);
                
                // Gửi Email
                EmailHelper.sendEmergencyEmail(requireContext(), -1);

                // LƯU VÀO LỊCH SỬ với tọa độ
                saveEmergencyToHistory(lat, lng);
            })
            .addOnFailureListener(e -> {
                // Nếu lỗi định vị vẫn gửi SOS nhưng không có tọa độ trong lịch sử
                SmsHelper.sendEmergencyAlert(requireContext(), -1);
                EmailHelper.sendEmergencyEmail(requireContext(), -1);
                saveEmergencyToHistory(null, null);
            });
    }

    private void saveEmergencyToHistory(Double lat, Double lng) {
        Context context = getContext();
        if (context == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            SessionEntity emergencySession = new SessionEntity();
            emergencySession.route = "Kích hoạt SOS khẩn cấp từ màn hình chính";
            emergencySession.status = "danger";
            emergencySession.timerDuration = 0;
            emergencySession.startedAt = System.currentTimeMillis();
            emergencySession.endedAt = System.currentTimeMillis();
            emergencySession.outcome = "emergency";
            emergencySession.latitude = lat;
            emergencySession.longitude = lng;
            
            AppDatabase.getInstance(context).sessionDao().insert(emergencySession);
        });
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
        if (!isAdded() || tvStatus == null || statusBadge == null || homeRootLayout == null) return;

        int statusColor;
        int bgColor;
        String statusText;

        switch (state) {
            case ACTIVE:
                statusText = getString(R.string.status_tracking);
                statusColor = ContextCompat.getColor(requireContext(), R.color.primary);
                bgColor = ContextCompat.getColor(requireContext(), R.color.primary_bg);
                break;
            case WARNING:
                statusText = getString(R.string.warning_status_msg);
                statusColor = ContextCompat.getColor(requireContext(), R.color.history_orange);
                bgColor = ContextCompat.getColor(requireContext(), R.color.history_orange_bg);
                break;
            case URGENT:
            case EMERGENCY:
                statusText = getString(R.string.emergency_status_msg);
                statusColor = ContextCompat.getColor(requireContext(), R.color.alert_red);
                bgColor = ContextCompat.getColor(requireContext(), R.color.alert_red_bg);
                if (getContext() != null) {
                    Toast.makeText(getContext(), getString(R.string.sos_sent_msg), Toast.LENGTH_LONG).show();
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
