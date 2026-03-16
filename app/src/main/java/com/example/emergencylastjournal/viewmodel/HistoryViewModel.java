package com.example.emergencylastjournal.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.emergencylastjournal.data.entity.SessionEntity;
import com.example.emergencylastjournal.data.repository.SessionRepository;
import java.util.List;

/**
 * ViewModel for the History screen.
 * Provides access to all recorded sessions.
 */
public class HistoryViewModel extends AndroidViewModel {
    private final SessionRepository repository;
    private final LiveData<List<SessionEntity>> allSessions;

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        repository = new SessionRepository(application);
        allSessions = repository.getAllSessions();
    }

    /**
     * Returns all sessions ordered by start time.
     */
    public LiveData<List<SessionEntity>> getAllSessions() {
        return allSessions;
    }

    /**
     * Updates a session (e.g., to archive it).
     */
    public void updateSession(SessionEntity session) {
        repository.update(session);
    }
}