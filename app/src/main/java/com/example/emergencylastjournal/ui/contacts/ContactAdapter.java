package com.example.emergencylastjournal.ui.contacts;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.emergencylastjournal.R;
import com.example.emergencylastjournal.data.entity.ContactEntity;
import com.google.android.material.materialswitch.MaterialSwitch;

/**
 * Adapter for the trusted contacts list.
 * Supports CRUD operations via callbacks.
 */
public class ContactAdapter extends ListAdapter<ContactEntity, ContactAdapter.ViewHolder> {

    private final OnContactActionListener listener;

    public interface OnContactActionListener {
        void onToggleLocation(ContactEntity contact, boolean isChecked);
        void onEdit(ContactEntity contact);
        void onDelete(ContactEntity contact);
    }

    public ContactAdapter(OnContactActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<ContactEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<ContactEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull ContactEntity oldItem, @NonNull ContactEntity newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ContactEntity oldItem, @NonNull ContactEntity newItem) {
            return oldItem.name.equals(newItem.name) && 
                   oldItem.phone.equals(newItem.phone) && 
                   oldItem.shareLocation == newItem.shareLocation;
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName, tvPhone;
        private final MaterialSwitch swLocation;
        private final ImageButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvContactName);
            tvPhone = itemView.findViewById(R.id.tvContactPhone);
            swLocation = itemView.findViewById(R.id.switchShareLocation);
            btnEdit = itemView.findViewById(R.id.btnEditContact);
            btnDelete = itemView.findViewById(R.id.btnDeleteContact);
        }

        public void bind(ContactEntity contact, OnContactActionListener listener) {
            tvName.setText(contact.name);
            tvPhone.setText(contact.phone);
            swLocation.setChecked(contact.shareLocation);

            swLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    listener.onToggleLocation(contact, isChecked);
                }
            });

            btnEdit.setOnClickListener(v -> listener.onEdit(contact));
            btnDelete.setOnClickListener(v -> listener.onDelete(contact));
        }
    }
}