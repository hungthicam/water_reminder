package com.cam.water_reminder.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cam.water_reminder.R;
import com.cam.water_reminder.item.Reminder;

import java.util.List;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder> {

    private List<Reminder> alarmList;
    private OnItemClickListener onItemClickListener;
    private OnDeleteClickListener onDeleteClickListener;

    // Constructor
    public AlarmAdapter(List<Reminder> alarmList, OnItemClickListener onItemClickListener, OnDeleteClickListener onDeleteClickListener) {
        this.alarmList = alarmList;
        this.onItemClickListener = onItemClickListener;
        this.onDeleteClickListener = onDeleteClickListener;
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alarm, parent, false);
        return new AlarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        Reminder reminder = alarmList.get(position);
        holder.titleTextView.setText(reminder.title);
        holder.descriptionTextView.setText(reminder.description);
        String time = String.format("%02d:%02d", reminder.hour, reminder.minute);
        holder.timeTextView.setText(time);

        // Đặt sự kiện khi người dùng click vào item báo thức
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(position);
            }
        });

        // Đặt sự kiện khi người dùng click vào nút xóa báo thức
        holder.deleteButton.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return alarmList.size();
    }

    // ViewHolder của RecyclerView để quản lý các phần tử trong danh sách
    public static class AlarmViewHolder extends RecyclerView.ViewHolder {

        TextView titleTextView;
        TextView descriptionTextView;
        TextView timeTextView;
        ImageButton deleteButton;

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.alarmTitleTextView);
            descriptionTextView = itemView.findViewById(R.id.alarmDescriptionTextView);
            timeTextView = itemView.findViewById(R.id.alarmTimeTextView);
            deleteButton = itemView.findViewById(R.id.deleteAlarmButton);
        }
    }

    // Giao diện cho sự kiện khi click vào một item báo thức
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    // Giao diện cho sự kiện khi click vào nút xóa báo thức
    public interface OnDeleteClickListener {
        void onDeleteClick(int position);
    }
}
