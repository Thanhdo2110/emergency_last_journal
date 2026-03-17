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
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;

public class ProfileFragment extends Fragment {
    private static final String PREF_NAME = "sos_settings";
    private static final String KEY_SOS_MESSAGE = "sos_message";
    private TextInputEditText etSosMessage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        etSosMessage = view.findViewById(R.id.etSosMessage);
        
        // Tải nội dung tin nhắn đã lưu
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedMessage = prefs.getString(KEY_SOS_MESSAGE, "SOS! Tôi gặp nguy hiểm. Lộ trình của tôi: [Route]");
        etSosMessage.setText(savedMessage);

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
}
