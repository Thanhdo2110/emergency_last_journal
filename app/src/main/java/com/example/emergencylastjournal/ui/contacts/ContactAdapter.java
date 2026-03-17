package com.example.emergencylastjournal.ui.contacts;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.emergencylastjournal.R;
import com.example.emergencylastjournal.data.entity.ContactEntity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

/**
 * Adapter for the trusted contacts list.
 * Supports CRUD operations via callbacks and displays SOS status.
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
            // QUAN TRỌNG: Phải so sánh cả sosCount để Adapter tự động cập nhật UI khi có tin nhắn mới
            return oldItem.name.equals(newItem.name) && 
                   oldItem.phone.equals(newItem.phone) && 
                   oldItem.shareLocation == newItem.shareLocation &&
                   oldItem.sosCount == newItem.sosCount &&
                   (oldItem.lastSosMessage == null ? newItem.lastSosMessage == null : oldItem.lastSosMessage.equals(newItem.lastSosMessage));
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
        private final TextView tvName, tvPhone, tvSosStatusCount, btnViewSosDetail;
        private final MaterialSwitch swLocation;
        private final ImageButton btnEdit, btnDelete;
        private final LinearLayout layoutSosStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvContactName);
            tvPhone = itemView.findViewById(R.id.tvContactPhone);
            swLocation = itemView.findViewById(R.id.switchShareLocation);
            btnEdit = itemView.findViewById(R.id.btnEditContact);
            btnDelete = itemView.findViewById(R.id.btnDeleteContact);
            layoutSosStatus = itemView.findViewById(R.id.layoutSosStatus);
            tvSosStatusCount = itemView.findViewById(R.id.tvSosStatusCount);
            btnViewSosDetail = itemView.findViewById(R.id.btnViewSosDetail);
        }

        public void bind(ContactEntity contact, OnContactActionListener listener) {
            tvName.setText(contact.name);
            tvPhone.setText(contact.phone);
            swLocation.setChecked(contact.shareLocation);

            // LOGIC LINH ĐỘNG: Chỉ hiện thanh thông báo nếu số lượng SOS > 0
            if (contact.sosCount > 0) {
                layoutSosStatus.setVisibility(View.VISIBLE);
                tvSosStatusCount.setText("Đã gửi " + contact.sosCount + " tin nhắn SOS");
                btnViewSosDetail.setOnClickListener(v -> showSosDetailDialog(contact));
            } else {
                layoutSosStatus.setVisibility(View.GONE);
            }

            swLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    listener.onToggleLocation(contact, isChecked);
                }
            });

            btnEdit.setOnClickListener(v -> listener.onEdit(contact));
            btnDelete.setOnClickListener(v -> listener.onDelete(contact));
        }

        private void showSosDetailDialog(ContactEntity contact) {
            new MaterialAlertDialogBuilder(itemView.getContext())
                    .setTitle("Nội dung tin nhắn SOS")
                    .setMessage("Tin nhắn gần nhất được gửi tới " + contact.name + ":\n\n" + 
                                (contact.lastSosMessage != null ? contact.lastSosMessage : "Dữ liệu đang được cập nhật..."))
                    .setPositiveButton("Đóng", null)
                    .show();
        }
    }
}
