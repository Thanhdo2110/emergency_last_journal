package com.example.emergencylastjournal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.emergencylastjournal.data.entity.ContactEntity;
import com.example.emergencylastjournal.ui.contacts.ContactAdapter;
import com.example.emergencylastjournal.viewmodel.ContactsViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ContactsFragment extends Fragment {
    private ContactsViewModel viewModel;
    private ContactAdapter adapter;
    private RecyclerView rvContacts;
    private MaterialButton btnAddContact;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ContactsViewModel.class);
        
        rvContacts = view.findViewById(R.id.rvContacts);
        btnAddContact = view.findViewById(R.id.btnAddContact);
        
        setupRecyclerView();
        observeContacts();

        if (btnAddContact != null) {
            btnAddContact.setOnClickListener(v -> showContactDialog(null));
        }
    }

    private void setupRecyclerView() {
        adapter = new ContactAdapter(new ContactAdapter.OnContactActionListener() {
            @Override
            public void onToggleLocation(ContactEntity contact, boolean isChecked) {
                contact.shareLocation = isChecked;
                viewModel.updateContact(contact);
            }

            @Override
            public void onEdit(ContactEntity contact) {
                showContactDialog(contact);
            }

            @Override
            public void onDelete(ContactEntity contact) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.delete_contact)
                        .setMessage(getString(R.string.delete_confirm_msg, contact.name))
                        .setPositiveButton(R.string.delete, (d, w) -> viewModel.deleteContact(contact))
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });
        rvContacts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvContacts.setAdapter(adapter);
    }

    private void observeContacts() {
        viewModel.getAllContacts().observe(getViewLifecycleOwner(), contacts -> {
            if (contacts != null) {
                adapter.submitList(contacts);
            }
        });
    }

    private void showContactDialog(@Nullable ContactEntity existingContact) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_contact, null);
        EditText etName = dialogView.findViewById(R.id.etContactName);
        EditText etEmail = dialogView.findViewById(R.id.etContactEmail);

        if (existingContact != null) {
            etName.setText(existingContact.name);
            etEmail.setText(existingContact.email);
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(existingContact == null ? R.string.add_contact : R.string.edit_contact)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String email = etEmail.getText().toString().trim();
                    
                    if (!name.isEmpty() && !email.isEmpty()) {
                        if (existingContact == null) {
                            viewModel.addContact(name, "", email);
                        } else {
                            existingContact.name = name;
                            existingContact.email = email;
                            existingContact.phone = ""; 
                            viewModel.updateContact(existingContact);
                        }
                    } else {
                        Toast.makeText(getContext(), R.string.error_fill_all, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
