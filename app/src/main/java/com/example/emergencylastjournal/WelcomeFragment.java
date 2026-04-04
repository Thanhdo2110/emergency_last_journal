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
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import java.util.concurrent.Executors;

public class WelcomeFragment extends Fragment {

    private TextInputEditText etName, etBloodType, etDob;
    private MaterialButton btnStart;
    private MaterialSwitch switchLanguage;
    private static final String PREFS_NAME = "sos_settings";
    private static final String KEY_FIRST_RUN = "is_first_run";
    private static final String APP_PREFS = "app_prefs";
    private static final String KEY_LANG = "language";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
        switchLanguage = view.findViewById(R.id.switchLanguage);

        // Thiết lập trạng thái ban đầu cho switch dựa trên ngôn ngữ hiện tại
        SharedPreferences appPrefs = requireContext().getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
        String currentLang = appPrefs.getString(KEY_LANG, "Vietnam");
        if (switchLanguage != null) {
            switchLanguage.setChecked(currentLang.equalsIgnoreCase("English"));
            
            switchLanguage.setOnCheckedChangeListener((buttonView, isChecked) -> {
                String newLang = isChecked ? "English" : "Vietnam";
                saveLanguage(newLang);
            });
        }

        btnStart.setOnClickListener(v -> saveAndStart());
    }

    private void saveLanguage(String lang) {
        SharedPreferences prefs = requireContext().getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANG, lang).apply();
        
        // Recreate activity to apply changes immediately
        if (getActivity() != null) {
            getActivity().recreate();
        }
    }

    private void saveAndStart() {
        final Context appContext = requireContext().getApplicationContext();
        String name = etName.getText().toString().trim();
        String bloodType = etBloodType.getText().toString().trim();
        String dob = etDob.getText().toString().trim();

        btnStart.setEnabled(false);

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
                prefs.edit().putBoolean(KEY_FIRST_RUN, false).commit();

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
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                    .navigate(R.id.navigation_home, null, new NavOptions.Builder()
                            .setPopUpTo(R.id.navigation_welcome, true)
                            .build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
