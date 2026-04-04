package com.example.emergencylastjournal.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.example.emergencylastjournal.data.entity.SessionEntity;
import com.example.emergencylastjournal.data.repository.SessionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ViewModel for the History screen.
 * Provides access to all recorded sessions with filtering and searching capabilities.
 */
public class HistoryViewModel extends AndroidViewModel {
    private final SessionRepository repository;
    private final LiveData<List<SessionEntity>> allSessions;
    private final MutableLiveData<String> filter = new MutableLiveData<>("all");
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final LiveData<List<SessionEntity>> filteredSessions;

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        repository = new SessionRepository(application);
        allSessions = repository.getAllSessions();

        // Combine filter and search query to filter the sessions
        LiveData<FilterParams> filterParams = Transformations.switchMap(filter, f -> 
            Transformations.map(searchQuery, s -> new FilterParams(f, s))
        );

        filteredSessions = Transformations.switchMap(filterParams, params -> 
            Transformations.map(allSessions, sessions -> {
                if (sessions == null) return new ArrayList<>();
                return sessions.stream()
                        .filter(s -> applyFilter(s, params.filter))
                        .filter(s -> applySearch(s, params.query))
                        .collect(Collectors.toList());
            })
        );
    }

    private boolean applyFilter(SessionEntity session, String filter) {
        if ("all".equals(filter)) return true;
        if ("running".equals(filter)) return session.endedAt == 0;
        return filter.equalsIgnoreCase(session.status) || filter.equalsIgnoreCase(session.outcome);
    }

    private boolean applySearch(SessionEntity session, String query) {
        if (query == null || query.isEmpty()) return true;
        String lowerQuery = query.toLowerCase();
        return (session.route != null && session.route.toLowerCase().contains(lowerQuery)) ||
               (session.outcome != null && session.outcome.toLowerCase().contains(lowerQuery)) ||
               (session.status != null && session.status.toLowerCase().contains(lowerQuery));
    }

    public LiveData<List<SessionEntity>> getFilteredSessions() {
        return filteredSessions;
    }

    public void setFilter(String filterValue) {
        filter.setValue(filterValue);
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }

    public LiveData<List<SessionEntity>> getAllSessions() {
        return allSessions;
    }

    public void updateSession(SessionEntity session) {
        repository.update(session);
    }

    private static class FilterParams {
        final String filter;
        final String query;
        FilterParams(String filter, String query) {
            this.filter = filter;
            this.query = query;
        }
    }
}
