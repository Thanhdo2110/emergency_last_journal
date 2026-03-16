package com.example.emergencylastjournal.viewmodel;

import android.app.Application;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.example.emergencylastjournal.data.entity.SessionEntity;
import com.example.emergencylastjournal.data.repository.SessionRepository;
import com.example.emergencylastjournal.service.TrackingForegroundService;

public class CreateSessionViewModel extends AndroidViewModel {
    private final SessionRepository repository;
    public MutableLiveData<Boolean> sessionStarted = new MutableLiveData<>(false);

    public CreateSessionViewModel(@NonNull Application application) {
        super(application);
        repository = new SessionRepository(application);
    }

    public void startSession(String route, String status, int durationSeconds) {
        SessionEntity session = new SessionEntity();
        session.route = route;
        session.status = status;
        session.timerDuration = durationSeconds;
        session.startedAt = System.currentTimeMillis();
        session.outcome = "active";

        repository.insert(session, sessionId -> {
            Intent serviceIntent = new Intent(getApplication(), TrackingForegroundService.class);
            serviceIntent.putExtra("SESSION_ID", sessionId);
            serviceIntent.putExtra("DURATION_SECONDS", durationSeconds);
            getApplication().startForegroundService(serviceIntent);
            
            sessionStarted.postValue(true);
        });
    }
}