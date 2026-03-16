package com.example.emergencylastjournal.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.emergencylastjournal.data.entity.SessionEntity;
import com.example.emergencylastjournal.data.entity.SessionState;
import com.example.emergencylastjournal.data.repository.SessionRepository;
import com.example.emergencylastjournal.service.TrackingForegroundService;

/**
 * Enhanced ViewModel for Dashboard.
 * Connects directly to the Foreground Service state for zero-latency UI updates.
 */
public class DashboardViewModel extends AndroidViewModel {
    private final SessionRepository sessionRepository;

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        sessionRepository = new SessionRepository(application);
    }

    public LiveData<SessionEntity> getActiveSession() {
        return sessionRepository.getActiveSession();
    }

    public LiveData<SessionState> getCurrentState() {
        return TrackingForegroundService.currentState;
    }

    public LiveData<Long> getTimeLeftSeconds() {
        return TrackingForegroundService.timeLeftSeconds;
    }
}