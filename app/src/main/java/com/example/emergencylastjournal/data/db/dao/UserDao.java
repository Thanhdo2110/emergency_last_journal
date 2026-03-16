package com.example.emergencylastjournal.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.emergencylastjournal.data.entity.UserEntity;

/**
 * Data Access Object for the users table.
 */
@Dao
public interface UserDao {
    @Query("SELECT * FROM users WHERE id = 1 LIMIT 1")
    LiveData<UserEntity> getUser();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserEntity user);

    @Update
    void update(UserEntity user);
}