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
import com.example.emergencylastjournal.ui.history.HistoryAdapter;
import com.example.emergencylastjournal.viewmodel.HistoryViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class HistoryFragment extends Fragment {
    private HistoryViewModel viewModel;
    private HistoryAdapter adapter;
    private RecyclerView rvHistory;
    private EditText etSearch;
    private ChipGroup chipGroupFilter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
        
        rvHistory = view.findViewById(R.id.rvHistory);
        etSearch = view.findViewById(R.id.etSearchHistory);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);

        setupRecyclerView();
        setupFilters();
        setupSearch();
        observeViewModel();
        
        // Cập nhật text cho các Chip từ Resource để hỗ trợ đa ngôn ngữ
        updateChipTexts();
    }

    private void updateChipTexts() {
        if (getView() == null) return;
        ((Chip)getView().findViewById(R.id.chipAll)).setText(R.string.filter_all);
        ((Chip)getView().findViewById(R.id.chipSafe)).setText(R.string.filter_safe);
        ((Chip)getView().findViewById(R.id.chipDanger)).setText(R.string.filter_danger);
        ((Chip)getView().findViewById(R.id.chipEmergency)).setText(R.string.filter_emergency);
        ((Chip)getView().findViewById(R.id.chipRunning)).setText(R.string.filter_running);
    }

    private void setupRecyclerView() {
        adapter = new HistoryAdapter(session -> {
            Bundle bundle = new Bundle();
            bundle.putInt("SESSION_ID", session.id);
            Navigation.findNavController(requireView()).navigate(R.id.action_history_to_historyDetail, bundle);
        });
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvHistory.setAdapter(adapter);
    }

    private void setupFilters() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipAll) viewModel.setFilter("all");
            else if (id == R.id.chipSafe) viewModel.setFilter("safe");
            else if (id == R.id.chipDanger) viewModel.setFilter("danger");
            else if (id == R.id.chipEmergency) viewModel.setFilter("emergency");
            else if (id == R.id.chipRunning) viewModel.setFilter("running");
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setSearchQuery(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void observeViewModel() {
        viewModel.getFilteredSessions().observe(getViewLifecycleOwner(), sessions -> {
            adapter.submitList(sessions);
        });
    }
}
