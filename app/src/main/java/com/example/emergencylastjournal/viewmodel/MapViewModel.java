package com.example.emergencylastjournal.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.emergencylastjournal.data.db.AppDatabase;
import com.example.emergencylastjournal.data.entity.GpsLogEntity;
import com.example.emergencylastjournal.data.entity.SessionEntity;
import com.example.emergencylastjournal.data.repository.SessionRepository;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapViewModel extends AndroidViewModel {
    private final SessionRepository repository;
    private final AppDatabase db;
    private final ExecutorService executorService;

    public MapViewModel(@NonNull Application application) {
        super(application);
        repository = new SessionRepository(application);
        db = AppDatabase.getInstance(application);
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<SessionEntity> getActiveSession() {
        return repository.getActiveSession();
    }

    public LiveData<List<GpsLogEntity>> getGpsLogs(int sessionId) {
        return db.sessionDao().getLogsForSession(sessionId);
    }

    public void completeSession(SessionEntity session) {
        executorService.execute(() -> {
            // Lấy object mới nhất từ DB để tránh dữ liệu cũ đè lên dữ liệu mới
            SessionEntity currentSession = db.sessionDao().getSessionById(session.id);
            if (currentSession != null) {
                currentSession.outcome = "safe"; // Đổi từ completed thành safe
                currentSession.endedAt = System.currentTimeMillis();
                db.sessionDao().update(currentSession);
            }
        });
    }
}
