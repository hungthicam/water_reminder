package com.cam.water_reminder.Fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.cam.water_reminder.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private ProgressBar progressBar;
    private TextView hydrationText;
    private TextView goalText, hello, time;
    private Button btnMinus500, btnPlus500;
    private int hydration = 0; // Lượng nước uống ban đầu
    private DatabaseReference waterRef;
    private String currentDate;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "WaterPrefs";
    private static final String KEY_LAST_DATE = "lastDate";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate bố cục của fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Khởi tạo các view
        progressBar = view.findViewById(R.id.progressBar);
        hydrationText = view.findViewById(R.id.hydration_text);
        goalText = view.findViewById(R.id.goal_text);
        hello = view.findViewById(R.id.hi_name);
        time = view.findViewById(R.id.time);
        btnMinus500 = view.findViewById(R.id.btn_minus_500);
        btnPlus500 = view.findViewById(R.id.btn_plus_500);

        // Khởi tạo SharedPreferences
        sharedPreferences = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Lấy ngày hiện tại
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        currentDate = sdf.format(new Date());

        // Kiểm tra nếu là ngày mới
        String lastDate = sharedPreferences.getString(KEY_LAST_DATE, "");
        if (!currentDate.equals(lastDate)) {
            hydration = 0; // Reset lượng nước uống về 0 nếu ngày mới
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_LAST_DATE, currentDate);
            editor.apply();
        }

        // Lấy tên người dùng và tham chiếu Firebase
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();

            // Tham chiếu đến bảng "historyOfDrinkingWater"
            waterRef = FirebaseDatabase.getInstance().getReference("historyOfDrinkingWater").child(uid).child(currentDate);

            // Lấy dữ liệu lượng nước từ Firebase
            waterRef.child("hydration").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        hydration = dataSnapshot.getValue(Integer.class);
                    } else {
                        hydration = 0; // Nếu không có dữ liệu, bắt đầu từ 0
                    }
                    updateUI(); // Cập nhật giao diện người dùng
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Xử lý lỗi nếu cần
                }
            });

            // Hiển thị tên người dùng
            displayUserName(uid);
        }

        // Thiết lập sự kiện khi nhấn nút -500ml và +500ml
        btnMinus500.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addWater(-500); // -500 ml
            }
        });

        btnPlus500.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addWater(500); // +500 ml
            }
        });

        // Cập nhật ngày và giờ hiện tại
        updateDateTime();

        return view;
    }

    // Phương thức cập nhật ngày giờ
    private void updateDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault());
        String currentDate = sdf.format(new Date());
        time.setText(currentDate);
    }

    // Phương thức hiển thị tên người dùng từ Firebase
    private void displayUserName(String uid) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String username = dataSnapshot.child("username").getValue(String.class);
                    if (username != null && !username.isEmpty()) {
                        hello.setText("Hi, " + username + " ✌️");
                    } else {
                        hello.setText("Hi, you ✌️");
                    }
                } else {
                    hello.setText("Hi, you ✌️");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                hello.setText("Hi, you ✌️");
            }
        });
    }

    // Phương thức thêm/trừ nước uống và lưu dữ liệu
    private void addWater(int amount) {
        hydration += amount;

        if (hydration < 0) {
            hydration = 0; // Đảm bảo lượng nước không nhỏ hơn 0
        }

        updateUI();

        // Lưu lượng nước đã uống vào bảng "historyOfDrinkingWater"
        waterRef.child("hydration").setValue(hydration);

        // Lưu dữ liệu lịch sử hàng ngày vào bảng "history"
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference("history").child(uid).child(currentDate);
        historyRef.child("hydration").setValue(hydration);
    }

    // Phương thức cập nhật giao diện người dùng (UI)
    private void updateUI() {
        int progress = hydration / 20; // Giả định rằng 2000ml là 100%
        progressBar.setProgress(progress);
        hydrationText.setText(hydration + " ml");
        goalText.setText("You have achieved\n" + progress + "% of your goal today");
    }
}
