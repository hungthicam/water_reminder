package com.cam.water_reminder.Fragment;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cam.water_reminder.model.AlarmReceiver;
import com.cam.water_reminder.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.cam.water_reminder.item.Reminder;
import com.cam.water_reminder.adapter.AlarmAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class SetDrinkingTimeFragment extends Fragment {

    private RecyclerView recyclerView;
    private AlarmAdapter adapter;
    private List<Reminder> alarmHistory;
    private TimePicker timePicker;
    private Button setAlarmButton;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_UID = "uid";
    private static final String KEY_PERMISSION_GRANTED = "isPermissionGranted";

    private Handler handler = new Handler();
    private Runnable refreshRunnable;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_set_drinking_time, container, false);

        recyclerView = view.findViewById(R.id.alarmHistoryRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        alarmHistory = new ArrayList<>();
        adapter = new AlarmAdapter(alarmHistory, position -> {
            Reminder clickedAlarm = alarmHistory.get(position);
            openEditAlarmDialog(clickedAlarm, position);
        }, this::onDeleteClick);
        recyclerView.setAdapter(adapter);

        timePicker = view.findViewById(R.id.timePicker);
        setAlarmButton = view.findViewById(R.id.setAlarmButton);

        sharedPreferences = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        boolean isPermissionGranted = sharedPreferences.getBoolean(KEY_PERMISSION_GRANTED, false);

        setAlarmButton.setOnClickListener(v -> {
            if (isPermissionGranted || checkExactAlarmPermission()) {
                setAlarmProcess();
            } else {
                requestExactAlarmPermission();
            }
        });

        // Gọi hàm loadAlarmHistory() để tải dữ liệu lần đầu tiên
        loadAlarmHistory();

        // Đặt Runnable để tự động làm mới mỗi 1 giây
        refreshRunnable = new Runnable() {
            @Override
            public void run() {

                loadAlarmHistory();
                handler.postDelayed(this, 500);
            }
        };
        handler.post(refreshRunnable);
        deleteOldAlarms();
        resetAlarms();

        return view;
    }



    private void resetAlarms() {
        String uid = getUserUid();
        if (uid != null) {
            DatabaseReference database = FirebaseDatabase.getInstance().getReference();
            DatabaseReference remindersRef = database.child("users").child(uid).child("alarmHistory");

            remindersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Reminder reminder = snapshot.getValue(Reminder.class);
                        if (reminder != null) {
                            setAlarm(reminder.hour, reminder.minute, reminder.timestamp);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("FirebaseError", "Error reinitializing alarm: " + databaseError.getMessage());
                }
            });
        }
    }

    private boolean checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true;
    }

    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
    }

    private String getUserUid() {
        return sharedPreferences.getString(KEY_UID, null);
    }

    private void setAlarmProcess() {
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();
        String alarmTime = String.format("%02d:%02d", hour, minute);

        Calendar now = Calendar.getInstance();
        Calendar selectedTime = Calendar.getInstance();
        selectedTime.set(Calendar.HOUR_OF_DAY, hour);
        selectedTime.set(Calendar.MINUTE, minute);
        selectedTime.set(Calendar.SECOND, 0);

        if (selectedTime.before(now)) {
            Log.d("Alarm", "The selected time has passed, the alarm cannot be set.");
            return;
        }

        for (Reminder reminder : alarmHistory) {
            if (reminder.hour == hour && reminder.minute == minute) {
                Log.d("Alarm", "Alarm already exists for " + alarmTime);
                return;
            }
        }

        String title = "New reminder";
        String description = "Reminder at " + alarmTime;
        String timestamp = String.valueOf(System.currentTimeMillis());

        String uid = getUserUid();
        if (uid != null) {
            DatabaseReference database = FirebaseDatabase.getInstance().getReference();
            DatabaseReference newReminderRef = database.child("users").child(uid).child("alarmHistory").child(alarmTime);
            String newReminderId = newReminderRef.getKey();

            Reminder newReminder = new Reminder(newReminderId, title, description, timestamp, hour, minute);

            newReminderRef.setValue(newReminder)
                    .addOnSuccessListener(aVoid -> {
                        alarmHistory.add(newReminder);
                        adapter.notifyItemInserted(alarmHistory.size() - 1);
                        setAlarm(hour, minute, timestamp);
                    })
                    .addOnFailureListener(e -> Log.e("FirebaseError", "Error saving alarm: " + e.getMessage()));

        } else {
            Log.e("UIDError", "Invalid user UID.");
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private void setAlarm(int hour, int minute, String timestamp) {
        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getContext(), AlarmReceiver.class);

        int requestCode = (hour * 10000 + minute * 100 + timestamp.hashCode()) & 0xfffffff;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
        } else {
            Snackbar.make(recyclerView, "Failed to initialize AlarmManager.", Snackbar.LENGTH_LONG).show();
        }
    }
    private void onDeleteClick(int position) {
        // Check for a valid location before operating
        if (position < 0 || position >= alarmHistory.size()) {
            Toast.makeText(getContext(), "Invalid index.", Toast.LENGTH_SHORT).show();
            return;
        }

        Reminder reminderToDelete = alarmHistory.get(position);

        if (reminderToDelete.id == null) {
            Toast.makeText(getContext(), "Invalid alarm ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = getUserUid();
        if (uid != null) {
            DatabaseReference database = FirebaseDatabase.getInstance().getReference();
            DatabaseReference reminderRef = database.child("users").child(uid).child("alarmHistory").child(reminderToDelete.id);

            // Cancel the alarm and remove from Firebase
            cancelAlarm(reminderToDelete.hour, reminderToDelete.minute, reminderToDelete.timestamp);

            reminderRef.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        // Check and delete from alarmHistory if it still exists
                        if (position < alarmHistory.size()) {
                            alarmHistory.remove(position);
                            adapter.notifyItemRemoved(position);
                        }
                    })
                    .addOnFailureListener(e -> Log.e("FirebaseError", "Error clearing alarm: " + e.getMessage()));

        } else {
            Log.e("UIDError", "Invalid user UID.");
        }
    }


    private void loadAlarmHistory() {
        String uid = getUserUid();
        if (uid != null) {
            DatabaseReference database = FirebaseDatabase.getInstance().getReference();
            DatabaseReference remindersRef = database.child("users").child(uid).child("alarmHistory");

            remindersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    alarmHistory.clear();  // Xóa dữ liệu cũ trước khi tải mới

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Reminder reminder = snapshot.getValue(Reminder.class);
                        if (reminder != null) {
                            alarmHistory.add(reminder);
                        }
                    }

                    // Sắp xếp lại danh sách báo thức
                    Collections.sort(alarmHistory, new Comparator<Reminder>() {
                        @Override
                        public int compare(Reminder o1, Reminder o2) {
                            if (o1.hour != o2.hour) {
                                return Integer.compare(o1.hour, o2.hour);
                            } else {
                                return Integer.compare(o1.minute, o2.minute);
                            }
                        }
                    });

                    adapter.notifyDataSetChanged();  // Cập nhật lại RecyclerView
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("FirebaseError", "Error loading alarm data: " + databaseError.getMessage());
                }
            });
        }
    }



    // Hàm lưu báo thức vào reminder_history và xóa khỏi alarmHistory
    private void saveReminderToHistoryAndDelete(Reminder reminder, DatabaseReference reminderRef) {
        String uid = getUserUid();
        if (uid == null) {
            Log.e("UIDError", "Invalid user UID.");
            return;
        }

        // Save to reminder_history before deleting
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference()
                .child("reminder_history").child(uid).push();

        historyRef.setValue(reminder)
                .addOnSuccessListener(aVoid1 -> {
                    Log.d("FirebaseSuccess", "The alarm has been saved to reminder_history.");
                    // Delete from alarmHistory after successful saving
                    reminderRef.removeValue()
                            .addOnSuccessListener(aVoid -> {
                                cancelAlarm(reminder.hour, reminder.minute, reminder.timestamp);
                                Log.d("FirebaseSuccess", "The alarm has been removed from alarmHistory.");
                            })
                            .addOnFailureListener(e -> Log.e("FirebaseError", "Error removing alarm from alarmHistory: " + e.getMessage()));
                })
                .addOnFailureListener(e -> Log.e("FirebaseError", "Error saving alarm to reminder_history: " + e.getMessage()));
    }

    private void deleteOldAlarms() {
        String uid = getUserUid();
        if (uid != null) {
            DatabaseReference database = FirebaseDatabase.getInstance().getReference();
            DatabaseReference remindersRef = database.child("users").child(uid).child("alarmHistory");

            remindersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Reminder reminder = snapshot.getValue(Reminder.class);

                        if (reminder != null) {
                            Calendar reminderTime = Calendar.getInstance();
                            reminderTime.set(Calendar.HOUR_OF_DAY, reminder.hour);
                            reminderTime.set(Calendar.MINUTE, reminder.minute);

                            if (reminderTime.before(Calendar.getInstance())) {
                                DatabaseReference reminderRef = snapshot.getRef();
                                saveReminderToHistoryAndDelete(reminder, reminderRef);
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("FirebaseError", "Error retrieving alarm data: " + databaseError.getMessage());
                }
            });
        }
    }

    private void openEditAlarmDialog(Reminder reminder, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Chỉnh sửa báo thức");

        // Sử dụng TimePicker để chọn lại giờ và phút
        TimePicker timePicker = new TimePicker(getContext());
        timePicker.setHour(reminder.hour);
        timePicker.setMinute(reminder.minute);
        builder.setView(timePicker);

        // Nút "Lưu" để xác nhận chỉnh sửa
        builder.setPositiveButton("Save", (dialog, which) -> {
            int newHour = timePicker.getHour();
            int newMinute = timePicker.getMinute();

            if (newHour != reminder.hour || newMinute != reminder.minute) {
                String uid = getUserUid();
                if (uid != null) {
                    DatabaseReference database = FirebaseDatabase.getInstance().getReference();
                    DatabaseReference reminderRef = database.child("users").child(uid).child("alarmHistory").child(reminder.id);

                    // Hủy báo thức cũ
                    cancelAlarm(reminder.hour, reminder.minute, reminder.timestamp);

                    // Create new timestamp
                    String newTimestamp = String.valueOf(System.currentTimeMillis());
                    String newDescription = "Reminder at " + String.format("%02d:%02d", newHour, newMinute);

                    // Update all alarm information with the new time and minute
                    reminder.hour = newHour;
                    reminder.minute = newMinute;
                    reminder.timestamp = newTimestamp;
                    reminder.description = newDescription;
                    reminder.title = "Reminder fixed";  // Update title if necessary

                    //Update Firebase
                    reminderRef.setValue(reminder)
                            .addOnSuccessListener(aVoid -> {
                                // Reset the alarm with new hours and minutes
                                setAlarm(newHour, newMinute, newTimestamp);

                                // Update the alarm and notification list for the adapter
                                alarmHistory.set(position, reminder);
                                adapter.notifyItemChanged(position);
                            })
                            .addOnFailureListener(e -> Log.e("FirebaseError", "Error updating alarm: " + e.getMessage()));
                } else {
                    Log.e("UIDError", "Invalid user UID.");
                }
            }
        });

        // "Cancel" button to exit the dialog
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        // Display dialog
        builder.create().show();
    }



    @SuppressLint("UnspecifiedImmutableFlag")
    private void cancelAlarm(int hour, int minute, String timestamp) {
        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getContext(), AlarmReceiver.class);

        int requestCode = (hour * 10000 + minute * 100 + timestamp.hashCode()) & 0xfffffff;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);  // Cancel the old alarm
            Log.d("Alarm", "The old alarm has been canceled.");
        } else {
            Log.e("AlarmError", "Cannot cancel old alarm.");
        }
    }
}
