package com.example.emergencylastjournal.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.emergencylastjournal.data.db.AppDatabase;
import com.example.emergencylastjournal.data.entity.ContactEntity;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for managing trusted contacts.
 * Handles background database operations for CRUD functionality.
 */
public class ContactsViewModel extends AndroidViewModel {
    private final AppDatabase db;
    private final ExecutorService executorService;

    public ContactsViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Returns an observable list of all trusted contacts.
     */
    public LiveData<List<ContactEntity>> getAllContacts() {
        return db.contactDao().getAllContacts();
    }

    /**
     * Adds a new contact to the database.
     * 
     * @param name The full name of the contact.
     * @param phone The phone number of the contact.
     * @param email The email address of the contact.
     */
    public void addContact(String name, String phone, String email) {
        executorService.execute(() -> {
            ContactEntity contact = new ContactEntity();
            contact.name = name;
            contact.phone = phone;
            contact.email = email;
            contact.shareLocation = true; // Default to true for emergency safety
            contact.verified = true;
            db.contactDao().insert(contact);
        });
    }

    /**
     * Updates an existing contact's information.
     */
    public void updateContact(ContactEntity contact) {
        executorService.execute(() -> db.contactDao().update(contact));
    }

    /**
     * Removes a contact from the database.
     */
    public void deleteContact(ContactEntity contact) {
        executorService.execute(() -> db.contactDao().delete(contact));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}