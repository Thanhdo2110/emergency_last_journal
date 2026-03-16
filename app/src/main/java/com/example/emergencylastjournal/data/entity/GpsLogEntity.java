package com.example.emergencylastjournal.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "gps_logs")
public class GpsLogEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int sessionId;
    public double latitude;
    public double longitude;
    public long recordedAt;

    public GpsLogEntity(int sessionId, double latitude, double longitude, long recordedAt) {
        this.sessionId = sessionId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.recordedAt = recordedAt;
    }
}