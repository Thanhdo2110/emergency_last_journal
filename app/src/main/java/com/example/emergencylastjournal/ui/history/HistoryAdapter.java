package com.example.emergencylastjournal.ui.history;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.emergencylastjournal.R;
import com.example.emergencylastjournal.data.entity.SessionEntity;
import com.google.android.material.chip.Chip;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Adapter for the RecyclerView in HistoryFragment.
 * Uses ListAdapter with DiffUtil for efficient list updates.
 */
public class HistoryAdapter extends ListAdapter<SessionEntity, HistoryAdapter.ViewHolder> {

    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(SessionEntity session);
    }

    public HistoryAdapter(OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<SessionEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<SessionEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull SessionEntity oldItem, @NonNull SessionEntity newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull SessionEntity oldItem, @NonNull SessionEntity newItem) {
            return (oldItem.route != null && oldItem.route.equals(newItem.route)) &&
                    (oldItem.status != null && oldItem.status.equals(newItem.status)) &&
                    (oldItem.outcome != null && oldItem.outcome.equals(newItem.outcome));
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDate, tvRoute, tvViewDetail;
        private final Chip chipStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvHistoryDate);
            tvRoute = itemView.findViewById(R.id.tvHistoryRoute);
            chipStatus = itemView.findViewById(R.id.chipHistoryStatus);
            tvViewDetail = itemView.findViewById(R.id.tvViewDetail);
        }

        public void bind(SessionEntity session, OnItemClickListener listener) {
            Context context = itemView.getContext();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(session.startedAt)));
            
            String routeText = (session.route != null && !session.route.isEmpty()) ? session.route : context.getString(R.string.no_route_description);
            tvRoute.setText(routeText);
            
            if (tvViewDetail != null) {
                tvViewDetail.setText(R.string.view_history);
            }

            // Logic hiển thị trạng thái theo yêu cầu mới
            String outcome = session.outcome != null ? session.outcome : "active";
            
            switch (outcome) {
                case "safe":
                case "manual":
                    chipStatus.setText(context.getString(R.string.filter_safe).toUpperCase());
                    chipStatus.setChipBackgroundColorResource(R.color.bg_status_green_alpha);
                    chipStatus.setTextColor(context.getColor(R.color.status_green));
                    chipStatus.setChipStrokeColorResource(R.color.status_green);
                    break;
                case "danger":
                case "emergency":
                case "sos_manual":
                    String statusText = "emergency".equals(outcome) || "sos_manual".equals(outcome) ? 
                            context.getString(R.string.filter_emergency) : context.getString(R.string.filter_danger);
                    
                    chipStatus.setText(statusText.toUpperCase());
                    chipStatus.setChipBackgroundColorResource(R.color.bg_alert_red_alpha);
                    chipStatus.setTextColor(context.getColor(R.color.alert_red));
                    chipStatus.setChipStrokeColorResource(R.color.alert_red);
                    break;
                case "active":
                    chipStatus.setText(context.getString(R.string.filter_running).toUpperCase());
                    chipStatus.setChipBackgroundColorResource(R.color.primary_bg);
                    chipStatus.setTextColor(context.getColor(R.color.primary));
                    chipStatus.setChipStrokeColorResource(R.color.primary);
                    break;
                default:
                    chipStatus.setText(context.getString(R.string.filter_safe).toUpperCase());
                    chipStatus.setChipBackgroundColorResource(R.color.bg_status_green_alpha);
                    chipStatus.setTextColor(context.getColor(R.color.status_green));
                    chipStatus.setChipStrokeColorResource(R.color.status_green);
                    break;
            }

            itemView.setOnClickListener(v -> listener.onItemClick(session));
        }
    }
}
