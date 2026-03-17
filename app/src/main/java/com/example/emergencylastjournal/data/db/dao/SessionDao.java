package com.example.emergencylastjournal.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.emergencylastjournal.data.entity.GpsLogEntity;
import com.example.emergencylastjournal.data.entity.SessionEntity;
import java.util.List;

@Dao
public interface SessionDao {
    @Insert
    long insert(SessionEntity session);

    @Update
    void update(SessionEntity session);

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    LiveData<List<SessionEntity>> getAllSessions();

    @Query("SELECT * FROM sessions WHERE outcome IS NULL OR outcome = 'active' ORDER BY startedAt DESC LIMIT 1")
    LiveData<SessionEntity> getActiveSession();

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    SessionEntity getSessionById(int sessionId);

    @Query("UPDATE sessions SET notifiedContacts = :names WHERE id = :sessionId")
    void updateNotifiedContacts(int sessionId, String names);

    @Insert
    void insertGpsLog(GpsLogEntity log);

    @Query("SELECT * FROM gps_logs WHERE sessionId = :sessionId ORDER BY recordedAt ASC")
    LiveData<List<GpsLogEntity>> getLogsForSession(int sessionId);

    @Query("SELECT * FROM gps_logs WHERE sessionId = :sessionId ORDER BY recordedAt ASC")
    List<GpsLogEntity> getLogsForSessionSync(int sessionId);
}
