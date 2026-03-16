package com.example.emergencylastjournal.data.repository;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import com.example.emergencylastjournal.data.db.AppDatabase;
import com.example.emergencylastjournal.data.db.dao.SessionDao;
import com.example.emergencylastjournal.data.entity.SessionEntity;
import com.example.emergencylastjournal.data.entity.SessionState;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository class for Session data.
 * Manages database operations and shared preferences for state persistence.
 */
public class SessionRepository {
    private static final String PREF_NAME = "session_prefs";
    private static final String KEY_STATE = "current_state";
    private static final String KEY_SESSION_ID = "active_session_id";

    private final SessionDao sessionDao;
    private final ExecutorService executorService;
    private final SharedPreferences prefs;

    public SessionRepository(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(appContext);
        this.sessionDao = db.sessionDao();
        this.executorService = Executors.newFixedThreadPool(2);
        this.prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // --- Database Operations ---

    public void insert(SessionEntity session, OnSessionInsertedListener listener) {
        executorService.execute(() -> {
            long id = sessionDao.insert(session);
            saveActiveSessionId((int) id);
            if (listener != null) {
                listener.onInserted((int) id);
            }
        });
    }

    public void update(SessionEntity session) {
        executorService.execute(() -> sessionDao.update(session));
    }

    public LiveData<List<SessionEntity>> getAllSessions() {
        return sessionDao.getAllSessions();
    }

    public LiveData<SessionEntity> getActiveSession() {
        return sessionDao.getActiveSession();
    }

    public SessionEntity getSessionByIdSync(int id) {
        return sessionDao.getSessionById(id);
    }

    // --- State Persistence (SharedPreferences) ---

    public void setSessionState(SessionState state) {
        prefs.edit().putString(KEY_STATE, state.name()).apply();
    }

    public SessionState getSessionState() {
        String stateName = prefs.getString(KEY_STATE, SessionState.IDLE.name());
        return SessionState.valueOf(stateName);
    }

    public void saveActiveSessionId(int id) {
        prefs.edit().putInt(KEY_SESSION_ID, id).apply();
    }

    public int getActiveSessionId() {
        return prefs.getInt(KEY_SESSION_ID, -1);
    }

    public interface OnSessionInsertedListener {
        void onInserted(int id);
    }
}