package com.example.emergencylastjournal.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.emergencylastjournal.data.entity.PhotoEntity;
import java.util.List;

/**
 * Data Access Object for session photos.
 */
@Dao
public interface PhotoDao {
    @Insert
    void insert(PhotoEntity photo);

    @Query("SELECT * FROM photos WHERE sessionId = :sessionId ORDER BY takenAt ASC")
    LiveData<List<PhotoEntity>> getPhotosForSession(int sessionId);

    @Query("DELETE FROM photos WHERE sessionId = :sessionId")
    void deletePhotosForSession(int sessionId);
}