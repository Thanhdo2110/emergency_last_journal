package com.example.emergencylastjournal.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Represents a photo evidence taken during a session.
 */
@Entity(tableName = "photos",
        foreignKeys = @ForeignKey(entity = SessionEntity.class,
                parentColumns = "id",
                childColumns = "sessionId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("sessionId")})
public class PhotoEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int sessionId;
    public String photoUri;
    public double latitude;
    public double longitude;
    public long takenAt;
}