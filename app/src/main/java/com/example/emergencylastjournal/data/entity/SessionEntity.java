package com.example.emergencylastjournal.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sessions")
public class SessionEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int userId;
    public String route;           // Mô tả lộ trình
    public String status;          // "safe" | "tired" | "danger"
    public int timerDuration;      // Giây
    public long startedAt;         // Unix timestamp
    public long endedAt;
    public String outcome;         // "completed" | "emergency" | "manual"
}