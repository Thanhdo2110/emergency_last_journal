package com.example.emergencylastjournal.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "contacts")
public class ContactEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public String phone;
    public boolean shareLocation;
    public boolean verified;
    
    // Thêm các trường để theo dõi trạng thái SOS
    public int sosCount = 0;           // Số tin nhắn SOS đã nhận
    public String lastSosMessage;     // Nội dung tin nhắn SOS gần nhất
}
