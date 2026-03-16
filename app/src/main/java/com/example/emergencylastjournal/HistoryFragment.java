package com.example.emergencylastjournal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.emergencylastjournal.ui.history.HistoryAdapter;
import com.example.emergencylastjournal.viewmodel.HistoryViewModel;

public class HistoryFragment extends Fragment {
    private HistoryViewModel viewModel;
    private HistoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        RecyclerView rvHistory = view.findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new HistoryAdapter(session -> {
            // Chuyển sang màn hình chi tiết lộ trình
            Bundle bundle = new Bundle();
            bundle.putInt("SESSION_ID", session.id);
            Navigation.findNavController(view).navigate(R.id.action_history_to_historyDetail, bundle);
        });
        rvHistory.setAdapter(adapter);

        // Kết nối dữ liệu từ Room DB
        viewModel.getAllSessions().observe(getViewLifecycleOwner(), sessions -> {
            if (sessions != null && !sessions.isEmpty()) {
                adapter.submitList(sessions);
                rvHistory.setVisibility(View.VISIBLE);
            }
        });
    }
}