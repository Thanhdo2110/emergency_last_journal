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
    public String outcome;         // "safe" | "emergency" | "manual"
    
    // Thêm các trường mới
    public String photoPath;       // Đường dẫn ảnh đính kèm
    public Double latitude;        // Vĩ độ lúc tạo
    public Double longitude;       // Kinh độ lúc tạo

    // Lưu danh sách người thân đã được gửi SOS (định dạng: Tên1, Tên2...)
    public String notifiedContacts;
}