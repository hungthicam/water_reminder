package com.cam.water_reminder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cam.water_reminder.Fragment.AccountFragment;
import com.cam.water_reminder.Fragment.HistoryFragment;
import com.cam.water_reminder.Fragment.HomeFragment;
import com.cam.water_reminder.Fragment.SetDrinkingTimeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import androidx.core.app.NotificationCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;

public class MainActivity extends AppCompatActivity {

    private FrameLayout frameLayout;
    private BottomNavigationView bottomNavigationView;
    private static final int REQUEST_CODE_NOTIFICATION_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo widget
        frameLayout = findViewById(R.id.frameLayout);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        // Yêu cầu quyền thông báo nếu chưa được cấp
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_CODE_NOTIFICATION_PERMISSION);
        } else {
            // Quyền đã được cấp, có thể tạo thông báo
            showNotification();
        }

        // Đặt fragment mặc định nếu chưa có
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout, new HomeFragment()).commit();
        }

        // Bottom Navigation
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                // Lấy id của MenuItem
                int id = menuItem.getItemId();

                // Điều hướng fragment dựa trên id
                if (id == R.id.bottom_nav_home) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout, new HomeFragment()).commit();
                } else if (id == R.id.bottom_nav_history) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout, new HistoryFragment()).commit();
                } else if (id == R.id.bottom_nav_notification) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout, new SetDrinkingTimeFragment()).commit();
                } else if (id == R.id.bottom_nav_account) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout, new AccountFragment()).commit();
                }
                return true;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Quyền thông báo đã được cấp
                showNotification();
            } else {
                // Quyền thông báo bị từ chối
                // Có thể hướng dẫn người dùng cấp quyền hoặc xử lý tình huống không có quyền
            }
        }
    }

    private void showNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("CHANNEL_ID", "Channel Name", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "CHANNEL_ID")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Welcome")
                .setContentText("Welcome to the Reminder Water!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify(1, builder.build());
    }
}
