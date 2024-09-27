package com.cam.water_reminder.model;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.cam.water_reminder.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "drink_water_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Tạo thông báo
        createNotificationChannel(context);
        sendNotification(context);

        // Kiểm tra và kích hoạt rung
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(2000); // Dành cho các phiên bản Android cũ
                }
            } else {
                Toast.makeText(context, "No vibration permission. Please grant permission.", Toast.LENGTH_SHORT).show();
            }
        }

        // Play the alarm sound
        playAlarmSound(context);

        // Get the alarm ID from the intent
        String alarmId = intent.getStringExtra("alarmId");

        // Save alarm history to Firebase
        if (alarmId != null) {
            saveAlarmHistoryToFirebase(context, alarmId);
            // Delete the alarm after triggering
            removeAlarmFromFirebase(context, alarmId);
        }
    }

    private void saveAlarmHistoryToFirebase(Context context, String alarmId) {
        String uid = getUserUid(context);
        if (uid.equals("default_uid")) {
            Toast.makeText(context, "Invalid UID, cannot save alarm history.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        String timestamp = String.valueOf(System.currentTimeMillis());
        database.child("users").child(uid).child("reminder_history").child(alarmId).setValue(timestamp)
                .addOnSuccessListener(aVoid -> Log.d("AlarmReceiver", "Alarm history has been saved."))
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error saving alarm history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void removeAlarmFromFirebase(Context context, String alarmId) {
        String uid = getUserUid(context);
        Log.d("AlarmReceiver", "UID: " + uid + ", AlarmId: " + alarmId);

        if (!uid.equals("default_uid")) {
            DatabaseReference database = FirebaseDatabase.getInstance().getReference();
            database.child("users").child(uid).child("alarmHistory").child(alarmId)
                    .removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "The alarm was cleared after activation.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Error clearing alarm after triggering: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.e("AlarmReceiver", "Invalid UID, cannot delete alarm.");
        }
    }

    private void createNotificationChannel(Context context) {
        // Tạo Notification Channel cho các phiên bản Android từ API 26 trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Drink Water Reminder";
            String description = "Channel for water drinking reminders";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void sendNotification(Context context) {
        // Check permission to send notifications
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "No permission to send notifications. Please grant permission.", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sharedPreferences = context.getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        String soundUriString = sharedPreferences.getString("notification_sound", null);
        Uri soundUri;

        if (soundUriString != null) {
            soundUri = Uri.parse(soundUriString);
        } else {
            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Replace with your icon
                .setContentTitle("Reminder to drink water")
                .setContentText("It's time for a drink!")
                .setSound(soundUri) // Use the selected sound
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(1, builder.build());
    }

    private void playAlarmSound(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        String soundUriString = sharedPreferences.getString("notification_sound", null);
        Uri alarmUri;

        if (soundUriString != null) {
            alarmUri = Uri.parse(soundUriString);
        } else {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        }

        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(context, alarmUri);
            mediaPlayer.prepare();
            mediaPlayer.start();

            // Dừng âm thanh sau 5 giây
            new android.os.Handler().postDelayed(() -> {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release(); // Giải phóng tài nguyên sau khi dừng
            }, 5000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private String getUserUid(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("uid", "default_uid");
    }
}
