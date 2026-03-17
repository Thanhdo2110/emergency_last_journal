package com.example.emergencylastjournal.data.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.emergencylastjournal.data.db.dao.ContactDao;
import com.example.emergencylastjournal.data.db.dao.GpsLogDao;
import com.example.emergencylastjournal.data.db.dao.PhotoDao;
import com.example.emergencylastjournal.data.db.dao.SessionDao;
import com.example.emergencylastjournal.data.db.dao.UserDao;
import com.example.emergencylastjournal.data.entity.ContactEntity;
import com.example.emergencylastjournal.data.entity.GpsLogEntity;
import com.example.emergencylastjournal.data.entity.PhotoEntity;
import com.example.emergencylastjournal.data.entity.SessionEntity;
import com.example.emergencylastjournal.data.entity.UserEntity;

/**
 * Main database for the Emergency Journal application.
 * Version 7 adds SOS tracking fields to ContactEntity.
 */
@Database(entities = {
        UserEntity.class, 
        SessionEntity.class, 
        ContactEntity.class, 
        PhotoEntity.class, 
        GpsLogEntity.class
}, version = 7, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract SessionDao sessionDao();
    public abstract ContactDao contactDao();
    public abstract GpsLogDao gpsLogDao();
    public abstract PhotoDao photoDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "emergency_journal_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
