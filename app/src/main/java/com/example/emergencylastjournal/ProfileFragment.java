package com.example.emergencylastjournal;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
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
    private AppDatabase db;
    private TextView tvCurrentLanguage;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        tvCurrentLanguage = view.findViewById(R.id.tvCurrentLanguage);
        
        updateLanguageDisplay();

        view.findViewById(R.id.cardAboutApp).setOnClickListener(v -> showAboutAppDialog());
        view.findViewById(R.id.cardPersonalInfo).setOnClickListener(v -> showPersonalInfoDialog());
        view.findViewById(R.id.cardLanguage).setOnClickListener(v -> showLanguageSelectionDialog());
        view.findViewById(R.id.cardSurvivalHandbook).setOnClickListener(v -> showSurvivalHandbookDialog());
    }

    private void updateLanguageDisplay() {
        String lang = prefs.getString("language", "Vietnam");
        if (tvCurrentLanguage != null) {
            tvCurrentLanguage.setText(getString(R.string.selected_language, lang));
        }
    }

    private void showLanguageSelectionDialog() {
        String[] languages = {"Vietnam", "English"};
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.language_dialog_title))
            .setItems(languages, (dialog, which) -> {
                saveLanguage(languages[which]);
            })
            .show();
    }

    private void saveLanguage(String lang) {
        // Lưu vào SharedPreferences để MainActivity đọc nhanh khi khởi động (Fix crash)
        prefs.edit().putString("language", lang).apply();

        // Đồng bộ vào Database
        Executors.newSingleThreadExecutor().execute(() -> {
            UserEntity user = db.userDao().getUserSync();
            if (user == null) {
                user = new UserEntity();
                user.id = 1;
            }
            user.language = lang;
            db.userDao().insert(user);
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    restartApp();
                });
            }
        });
    }

    private void restartApp() {
        if (getActivity() != null) {
            Intent intent = getActivity().getIntent();
            getActivity().finish();
            startActivity(intent);
        }
    }

    private void showAboutAppDialog() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.about_app_title))
            .setMessage(getString(R.string.about_app_message))
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }

    private void showSurvivalHandbookDialog() {
        String[] topics = getResources().getStringArray(R.array.survival_topics);
        String[] contents = getResources().getStringArray(R.array.survival_contents);

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.survival_handbook_title))
            .setItems(topics, (dialog, which) -> {
                showSurvivalDetail(topics[which], contents[which]);
            })
            .setPositiveButton(android.R.string.cancel, null)
            .show();
    }

    private void showSurvivalDetail(String title, String content) {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(content)
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }

    private void showPersonalInfoDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_personal_info, null);
        TextInputEditText etName = dialogView.findViewById(R.id.etUserName);
        TextInputEditText etBlood = dialogView.findViewById(R.id.etBloodType);
        TextInputEditText etDob = dialogView.findViewById(R.id.etUserDob);
        TextInputEditText etNotes = dialogView.findViewById(R.id.etMedicalNotes);
        AutoCompleteTextView actvLanguage = dialogView.findViewById(R.id.actvLanguage);

        String[] languages = {"Vietnam", "English"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, languages);
        actvLanguage.setAdapter(adapter);

        Executors.newSingleThreadExecutor().execute(() -> {
            UserEntity user = db.userDao().getUserSync();
            if (user != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    etName.setText(user.name);
                    etBlood.setText(user.bloodType);
                    etDob.setText(user.dateOfBirth);
                    etNotes.setText(user.emergencyNotes);
                    String currentLang = prefs.getString("language", "Vietnam");
                    actvLanguage.setText(currentLang, false);
                });
            }
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnSaveDialog).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String blood = etBlood.getText().toString().trim();
            String dob = etDob.getText().toString().trim();
            String notes = etNotes.getText().toString().trim();
            String lang = actvLanguage.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError(getString(R.string.error_required));
                return;
            }

            saveLanguage(lang); // Lưu ngôn ngữ và restart app

            Executors.newSingleThreadExecutor().execute(() -> {
                UserEntity user = new UserEntity();
                user.id = 1;
                user.name = name;
                user.bloodType = blood;
                user.dateOfBirth = dob;
                user.emergencyNotes = notes;
                user.language = lang;
                db.userDao().insert(user);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        dialog.dismiss();
                    });
                }
            });
        });

        dialog.show();
    }
}
