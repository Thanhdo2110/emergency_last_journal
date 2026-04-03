package com.example.emergencylastjournal.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Represents a single user profile in the database.
 */
@Entity(tableName = "users")
public class UserEntity {
    @PrimaryKey
    public int id = 1; // Singleton user for this app

    public String name;
    public String bloodType;
    public String dateOfBirth; // New field
    public String emergencyNotes; // Medical conditions, etc.
}