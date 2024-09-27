package com.cam.water_reminder.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cam.water_reminder.model.BarChartView;
import com.cam.water_reminder.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryFragment extends Fragment {

    private BarChartView barChartView;
    private Spinner spinnerTime;
    private DatabaseReference historyRef;
    private FirebaseUser user;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        // Khởi tạo các view
        barChartView = view.findViewById(R.id.barChartView);
        spinnerTime = view.findViewById(R.id.spinner_time);

        // Lấy tham chiếu đến Firebase
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            historyRef = FirebaseDatabase.getInstance().getReference("historyOfDrinkingWater").child(uid);
        }

        // Xử lý sự kiện khi người dùng chọn khoảng thời gian từ Spinner
        spinnerTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedTime = parent.getItemAtPosition(position).toString();
                if (selectedTime.equals("Week")) {
                    loadWeeklyData();
                } else if (selectedTime.equals("Month")) {
                    loadMonthlyData();
                } else if (selectedTime.equals("Year")) {
                    loadYearlyData();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        return view;
    }

    // Phương thức load dữ liệu theo tuần (giữ nguyên như cũ)
    private void loadWeeklyData() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        String startDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());


        List<Integer> hydrationData = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        String[] weekDays = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        for (int i = 0; i < 7; i++) {
            hydrationData.add(0);
            labels.add(weekDays[i]); // Sử dụng danh sách các ngày trong tuần
        }


        historyRef.orderByKey().startAt(startDate).endAt(currentDate).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    if (map != null && map.get("hydration") != null) {
                        int hydration = ((Long) map.get("hydration")).intValue();

                        try {
                            Date date = sdf.parse(snapshot.getKey());
                            Calendar tempCal = Calendar.getInstance();
                            tempCal.setTime(date);

                            int dayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY;
                            if (dayOfWeek >= 0 && dayOfWeek < 7) {
                                hydrationData.set(dayOfWeek, hydration);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                barChartView.setData(hydrationData, labels); // Truyền cả dữ liệu và nhãn
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Xử lý lỗi
            }
        });
    }


    // Phương thức load dữ liệu theo tháng
    private void loadMonthlyData() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        String startDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Tính số ngày của tháng hiện tại
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Tạo danh sách mặc định cho tất cả các ngày trong tháng
        List<Integer> hydrationData = new ArrayList<>();
        List<String> labels = new ArrayList<>(); // Danh sách nhãn cho các ngày trong tháng
        for (int i = 0; i < daysInMonth; i++) {
            hydrationData.add(0);
            labels.add("" + (i + 1)); // Nhãn cho từng ngày
        }

        historyRef.orderByKey().startAt(startDate).endAt(currentDate).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    if (map != null && map.get("hydration") != null) {
                        int hydration = ((Long) map.get("hydration")).intValue();

                        try {
                            Date date = sdf.parse(snapshot.getKey());
                            Calendar tempCal = Calendar.getInstance();
                            tempCal.setTime(date);

                            int dayOfMonth = tempCal.get(Calendar.DAY_OF_MONTH) - 1; // Tính từ 0
                            if (dayOfMonth >= 0 && dayOfMonth < daysInMonth) {
                                hydrationData.set(dayOfMonth, hydration);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                barChartView.setData(hydrationData, labels); // Truyền cả dữ liệu và nhãn
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Xử lý lỗi
            }
        });
    }


    // Phương thức load dữ liệu theo năm
    private void loadYearlyData() {
        String startDate = "2024-01-01";
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Tạo danh sách mặc định cho 12 tháng
        List<Integer> hydrationData = new ArrayList<>();
        List<String> labels = new ArrayList<>(); // Danh sách nhãn cho 12 tháng
        for (int i = 0; i < 12; i++) {
            hydrationData.add(0);
            labels.add("" + (i + 1));
        }

        historyRef.orderByKey().startAt(startDate).endAt(currentDate).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                // Dữ liệu để tính trung bình cho mỗi tháng
                int[] monthlyTotals = new int[12];
                int[] dayCounts = new int[12];

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    if (map != null && map.get("hydration") != null) {
                        int hydration = ((Long) map.get("hydration")).intValue();

                        try {
                            Date date = sdf.parse(snapshot.getKey());
                            Calendar tempCal = Calendar.getInstance();
                            tempCal.setTime(date);

                            int month = tempCal.get(Calendar.MONTH); // Tính từ 0, tháng 1 là 0, tháng 12 là 11
                            monthlyTotals[month] += hydration;
                            dayCounts[month]++;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                // Tính trung bình cho mỗi tháng
                for (int i = 0; i < 12; i++) {
                    if (dayCounts[i] > 0) {
                        hydrationData.set(i, monthlyTotals[i] / dayCounts[i]); // Lấy trung bình cộng
                    }
                }

                barChartView.setData(hydrationData, labels);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Xử lý lỗi
            }
        });
    }

}
