package com.example.emergencylastjournal;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.emergencylastjournal.data.db.AppDatabase;
import com.example.emergencylastjournal.data.entity.UserEntity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {
    private static final String PREF_NAME = "sos_settings";
    private static final String KEY_SOS_MESSAGE = "sos_message";
    private TextInputEditText etSosMessage;
    private AppDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        etSosMessage = view.findViewById(R.id.etSosMessage);
        
        // Tải nội dung tin nhắn SOS
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedMessage = prefs.getString(KEY_SOS_MESSAGE, "SOS! Tôi gặp nguy hiểm. Lộ trình của tôi: [Route]");
        etSosMessage.setText(savedMessage);

        // Click vào "Thông tin cá nhân" card
        view.findViewById(R.id.cardPersonalInfo).setOnClickListener(v -> showPersonalInfoDialog());

        view.findViewById(R.id.btnSaveProfile).setOnClickListener(v -> {
            String newMessage = etSosMessage.getText().toString().trim();
            if (newMessage.isEmpty()) {
                Toast.makeText(getContext(), "Nội dung tin nhắn không được để trống!", Toast.LENGTH_SHORT).show();
            } else {
                prefs.edit().putString(KEY_SOS_MESSAGE, newMessage).apply();
                Toast.makeText(getContext(), "Đã lưu cài đặt SOS!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showPersonalInfoDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_personal_info, null);
        TextInputEditText etName = dialogView.findViewById(R.id.etUserName);
        TextInputEditText etBlood = dialogView.findViewById(R.id.etBloodType);
        TextInputEditText etNotes = dialogView.findViewById(R.id.etMedicalNotes);

        // Lấy dữ liệu cũ từ DB
        Executors.newSingleThreadExecutor().execute(() -> {
            UserEntity user = db.userDao().getUserSync();
            if (user != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    etName.setText(user.name);
                    etBlood.setText(user.bloodType);
                    etNotes.setText(user.emergencyNotes);
                });
            }
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnSaveDialog).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String blood = etBlood.getText().toString().trim();
            String notes = etNotes.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("Vui lòng nhập tên");
                return;
            }

            Executors.newSingleThreadExecutor().execute(() -> {
                UserEntity user = new UserEntity();
                user.id = 1;
                user.name = name;
                user.bloodType = blood;
                user.emergencyNotes = notes;
                db.userDao().insert(user);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Đã cập nhật thông tin cá nhân!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                }
            });
        });

        dialog.show();
    }
}
