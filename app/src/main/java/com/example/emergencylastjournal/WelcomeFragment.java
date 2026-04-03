package com.example.emergencylastjournal;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import com.example.emergencylastjournal.data.db.AppDatabase;
import com.example.emergencylastjournal.data.entity.UserEntity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.concurrent.Executors;

public class WelcomeFragment extends Fragment {

    private TextInputEditText etName, etBloodType, etDob;
    private MaterialButton btnStart;
    private static final String PREFS_NAME = "sos_settings"; // Đổi về cùng bộ Prefs để đồng bộ
    private static final String KEY_FIRST_RUN = "is_first_run";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Kiểm tra ngay tại đây, nếu không phải lần đầu thì ẩn view đi
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_FIRST_RUN, true)) {
            return new View(requireContext()); 
        }
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_FIRST_RUN, true)) {
            navigateToHome();
            return;
        }

        etName = view.findViewById(R.id.etName);
        etBloodType = view.findViewById(R.id.etBloodType);
        etDob = view.findViewById(R.id.etDob);
        btnStart = view.findViewById(R.id.btnStart);

        btnStart.setOnClickListener(v -> saveAndStart());
    }

    private void saveAndStart() {
        final Context appContext = requireContext().getApplicationContext();
        String name = etName.getText().toString().trim();
        String bloodType = etBloodType.getText().toString().trim();
        String dob = etDob.getText().toString().trim();

        btnStart.setEnabled(false); // Tránh nhấn nhiều lần

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(appContext);
                UserEntity user = db.userDao().getUserSync();
                if (user == null) {
                    user = new UserEntity();
                    user.id = 1;
                }
                
                user.name = name.isEmpty() ? "Người dùng" : name;
                user.bloodType = bloodType;
                user.dateOfBirth = dob;
                
                db.userDao().insert(user);

                SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit().putBoolean(KEY_FIRST_RUN, false).commit(); // Dùng commit để lưu chắc chắn

                if (isAdded()) {
                    requireActivity().runOnUiThread(this::navigateToHome);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void navigateToHome() {
        if (!isAdded()) return;
        
        try {
            // Sử dụng requireView() để tìm NavController từ chính Fragment này
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                    .navigate(R.id.navigation_home, null, new NavOptions.Builder()
                            .setPopUpTo(R.id.navigation_welcome, true)
                            .build());
        } catch (Exception e) {
            // Fallback nếu navigation gặp lỗi
            e.printStackTrace();
        }
    }
}
