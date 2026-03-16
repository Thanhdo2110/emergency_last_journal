package com.example.emergencylastjournal;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.emergencylastjournal.data.entity.SessionEntity;
import com.example.emergencylastjournal.data.repository.SessionRepository;
import com.example.emergencylastjournal.service.TrackingForegroundService;
import com.google.android.material.textfield.TextInputEditText;
import java.lang.reflect.Field;

public class CreateJournalFragment extends Fragment {
    private SessionRepository repository;
    private TextInputEditText etRoute;
    private NumberPicker npHour, npMinute;
    private TextView tvTimerDisplay;
    private View rootView; // Lưu trữ view để điều hướng an toàn

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    executeStartProtection();
                } else {
                    Toast.makeText(getContext(), "Ứng dụng cần đủ quyền để bảo vệ bạn!", Toast.LENGTH_LONG).show();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_create_journal, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new SessionRepository(requireActivity().getApplication());
        
        etRoute = view.findViewById(R.id.etRouteDetail);
        tvTimerDisplay = view.findViewById(R.id.tvTimerDisplay);
        npHour = view.findViewById(R.id.npHour);
        npMinute = view.findViewById(R.id.npMinute);

        setupNumberPicker(npHour, 0, 23, 0);
        setupNumberPicker(npMinute, 0, 59, 15);

        NumberPicker.OnValueChangeListener timeChangeListener = (picker, oldVal, newVal) -> updateTimerDisplay();
        npHour.setOnValueChangedListener(timeChangeListener);
        npMinute.setOnValueChangedListener(timeChangeListener);

        view.findViewById(R.id.btnSaveJournal).setOnClickListener(v -> checkPermissionsAndStart());
    }

    private void checkPermissionsAndStart() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION, 
                Manifest.permission.POST_NOTIFICATIONS, 
                Manifest.permission.SEND_SMS
            };
        } else {
            permissions = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION, 
                Manifest.permission.SEND_SMS
            };
        }

        boolean needRequest = false;
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), p) != PackageManager.PERMISSION_GRANTED) {
                needRequest = true;
                break;
            }
        }

        if (needRequest) {
            permissionLauncher.launch(permissions);
        } else {
            executeStartProtection();
        }
    }

    private void executeStartProtection() {
        String route = etRoute.getText().toString();
        if (route.isEmpty()) {
            etRoute.setError("Vui lòng nhập lộ trình");
            return;
        }

        int totalSeconds = (npHour.getValue() * 3600) + (npMinute.getValue() * 60);
        if (totalSeconds == 0) {
            Toast.makeText(getContext(), "Vui lòng chọn thời gian bảo vệ > 0", Toast.LENGTH_SHORT).show();
            return;
        }

        Context appContext = requireActivity().getApplicationContext();
        SessionEntity session = new SessionEntity();
        session.route = route;
        session.status = "active";
        session.timerDuration = totalSeconds;
        session.startedAt = System.currentTimeMillis();

        repository.insert(session, sessionId -> {
            Intent serviceIntent = new Intent(appContext, TrackingForegroundService.class);
            serviceIntent.putExtra("SESSION_ID", sessionId);
            serviceIntent.putExtra("DURATION_SECONDS", totalSeconds);
            appContext.startForegroundService(serviceIntent);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(appContext, "BẢO VỆ ĐÃ ĐƯỢC KÍCH HOẠT!", Toast.LENGTH_SHORT).show();
                    if (isAdded() && rootView != null) {
                        Navigation.findNavController(rootView).navigate(R.id.navigation_map);
                    }
                });
            }
        });
    }

    private void setupNumberPicker(NumberPicker picker, int min, int max, int value) {
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setValue(value);
        try {
            Field selectorWheelPaintField = picker.getClass().getDeclaredField("mSelectorWheelPaint");
            selectorWheelPaintField.setAccessible(true);
            ((Paint) selectorWheelPaintField.get(picker)).setColor(requireContext().getColor(R.color.white));
            int count = picker.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = picker.getChildAt(i);
                if (child instanceof EditText) {
                    ((EditText) child).setTextColor(requireContext().getColor(R.color.white));
                }
            }
            picker.invalidate();
        } catch (Exception ignored) {}
    }

    private void updateTimerDisplay() {
        String time = String.format("%02d:%02d:00", npHour.getValue(), npMinute.getValue());
        tvTimerDisplay.setText(time);
    }
}