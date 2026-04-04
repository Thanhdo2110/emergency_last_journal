package com.example.emergencylastjournal;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.emergencylastjournal.data.entity.SessionEntity;
import com.example.emergencylastjournal.ui.history.HistoryAdapter;
import com.example.emergencylastjournal.viewmodel.HistoryViewModel;
import com.google.android.material.chip.ChipGroup;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HistoryFragment extends Fragment {
    private HistoryViewModel viewModel;
    private HistoryAdapter adapter;
    private List<SessionEntity> fullList = new ArrayList<>();
    private String currentSearchQuery = "";
    private String currentFilter = "all";

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
        EditText etSearch = view.findViewById(R.id.etSearchHistory);
        ChipGroup chipGroup = view.findViewById(R.id.chipGroupFilter);

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new HistoryAdapter(session -> {
            Bundle bundle = new Bundle();
            bundle.putInt("SESSION_ID", session.id);
            Navigation.findNavController(view).navigate(R.id.action_history_to_historyDetail, bundle);
        });
        rvHistory.setAdapter(adapter);

        // Lắng nghe dữ liệu
        viewModel.getAllSessions().observe(getViewLifecycleOwner(), sessions -> {
            if (sessions != null) {
                fullList = sessions;
                applyFilterAndSearch();
                rvHistory.setVisibility(View.VISIBLE);
            }
        });

        // Xử lý tìm kiếm
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                currentSearchQuery = removeAccent(s.toString().toLowerCase().trim());
                applyFilterAndSearch();
            }
        });

        // Xử lý lọc theo Chip
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentFilter = "all";
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chipSafe) currentFilter = "safe";
                else if (id == R.id.chipDanger) currentFilter = "danger";
                else if (id == R.id.chipEmergency) currentFilter = "emergency";
                else if (id == R.id.chipRunning) currentFilter = "active";
                else currentFilter = "all";
            }
            applyFilterAndSearch();
        });
    }

    private void applyFilterAndSearch() {
        if (fullList == null) return;

        List<SessionEntity> filteredList = fullList.stream()
            .filter(session -> {
                // Lọc theo trạng thái kết quả
                boolean matchFilter = true;
                String outcome = session.outcome != null ? session.outcome : "active";
                
                if (!currentFilter.equals("all")) {
                    if (currentFilter.equals("safe")) {
                        matchFilter = "safe".equals(outcome) || "manual".equals(outcome);
                    } else {
                        matchFilter = currentFilter.equals(outcome);
                    }
                }
                
                // Lọc theo từ khóa tìm kiếm (lộ trình) - Đã bỏ dấu
                boolean matchSearch = true;
                if (!currentSearchQuery.isEmpty()) {
                    String routeUnaccented = removeAccent(session.route != null ? session.route.toLowerCase() : "");
                    matchSearch = routeUnaccented.contains(currentSearchQuery);
                }
                
                return matchFilter && matchSearch;
            })
            .collect(Collectors.toList());

        adapter.submitList(filteredList);
    }

    // Hàm loại bỏ dấu tiếng Việt
    private String removeAccent(String s) {
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }
}
