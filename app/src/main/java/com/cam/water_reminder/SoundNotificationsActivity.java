package com.cam.water_reminder;

import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;



public class SoundNotificationsActivity extends AppCompatActivity {

    private static final int RINGTONE_PICKER_REQUEST_CODE = 1;

    private Switch soundSwitch;
    private TextView ringtoneTextView;
    private Button changeRingtoneButton;
    private Uri ringtoneUri;  // Lưu URI của nhạc chuông đã chọn

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound_notifications);

        // Initialize views
        soundSwitch = findViewById(R.id.sound_switch);
        ringtoneTextView = findViewById(R.id.ringtone_textview);
        changeRingtoneButton = findViewById(R.id.change_ringtone_button);
        ImageButton backButton = findViewById(R.id.imageButton);

        // Thiết lập sự kiện cho nút Back
        backButton.setOnClickListener(v -> onBackPressed());

        // Lấy nhạc chuông mặc định và hiển thị
        ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_NOTIFICATION);
        updateRingtoneTextView(ringtoneUri);

        // Event when the user wants to change the ringtone
        changeRingtoneButton.setOnClickListener(v -> {
            // Open the ringtone chooser
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select notification ringtone");
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneUri);
            startActivityForResult(intent, RINGTONE_PICKER_REQUEST_CODE);
        });

        // Event when notification sound is turned on or off
        soundSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(SoundNotificationsActivity.this, "Notification sounds enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(SoundNotificationsActivity.this, "Notification sounds turned off", Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RINGTONE_PICKER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            // Lấy URI của nhạc chuông đã chọn
            ringtoneUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (ringtoneUri != null) {
                // Cập nhật TextView với tên nhạc chuông đã chọn
                updateRingtoneTextView(ringtoneUri);
                // Lưu URI vào SharedPreferences
                saveSelectedSound(ringtoneUri);
            }
        }
    }

    // Cập nhật TextView với tên nhạc chuông đã chọn
    private void updateRingtoneTextView(Uri ringtoneUri) {
        Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
        String ringtoneTitle = ringtone.getTitle(this);
        ringtoneTextView.setText("Current ringtone: " + ringtoneTitle);
    }

    // Lưu URI của nhạc chuông đã chọn vào SharedPreferences
    private void saveSelectedSound(Uri soundUri) {
        getSharedPreferences("myPrefs", MODE_PRIVATE)
                .edit()
                .putString("notification_sound", soundUri.toString())
                .apply();
    }
}
