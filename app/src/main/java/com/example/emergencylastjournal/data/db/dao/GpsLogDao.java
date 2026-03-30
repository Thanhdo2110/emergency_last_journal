package com.example.emergencylastjournal.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.emergencylastjournal.data.entity.GpsLogEntity;
import java.util.List;

/**
 * Data Access Object for GPS logs.
 */
@Dao
public interface GpsLogDao {
    @Insert
    void insert(GpsLogEntity log);

    @Query("SELECT * FROM gps_logs WHERE sessionId = :sessionId ORDER BY recordedAt ASC")
    LiveData<List<GpsLogEntity>> getLogsForSession(int sessionId);

    @Query("SELECT * FROM gps_logs WHERE sessionId = :sessionId ORDER BY recordedAt DESC LIMIT 1")
    GpsLogEntity getLastLogForSessionSync(int sessionId);

    @Query("DELETE FROM gps_logs WHERE sessionId = :sessionId")
    void deleteLogsForSession(int sessionId);
}
