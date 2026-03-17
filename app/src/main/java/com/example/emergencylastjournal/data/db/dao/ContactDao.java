package com.example.emergencylastjournal.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.emergencylastjournal.data.entity.ContactEntity;
import java.util.List;

@Dao
public interface ContactDao {
    @Insert
    void insert(ContactEntity contact);

    @Update
    void update(ContactEntity contact);

    @Delete
    void delete(ContactEntity contact);

    @Query("SELECT * FROM contacts")
    LiveData<List<ContactEntity>> getAllContacts();

    @Query("SELECT * FROM contacts")
    List<ContactEntity> getAllContactsSync();

    @Query("SELECT * FROM contacts WHERE shareLocation = 1")
    List<ContactEntity> getEmergencyContactsSync();

    // Lệnh để dọn dẹp toàn bộ trạng thái SOS ảo
    @Query("UPDATE contacts SET sosCount = 0, lastSosMessage = NULL")
    void resetAllSosStatus();
    
    @Query("DELETE FROM contacts WHERE name = 'Người thân mẫu'")
    void deleteMockContact();
}
