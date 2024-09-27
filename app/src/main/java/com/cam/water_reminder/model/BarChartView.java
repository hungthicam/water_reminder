package com.cam.water_reminder.model;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

public class BarChartView extends View {

    private Paint barPaint;
    private Paint textPaint;
    private List<Integer> data;
    private List<String> labels; // Danh sách nhãn ngày/tháng/năm

    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        barPaint = new Paint();
        barPaint.setColor(Color.BLUE);
        barPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(40);
    }

    // Phương thức để truyền dữ liệu và nhãn cho biểu đồ
    public void setData(List<Integer> hydrationData, List<String> labels) {
        this.data = hydrationData;
        this.labels = labels;
        invalidate(); // Bắt buộc gọi lại để vẽ lại view sau khi thay đổi dữ liệu
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (data == null || data.isEmpty() || labels == null || labels.isEmpty()) {
            return;
        }

        int barWidth = 200; // Đặt chiều rộng cố định cho mỗi cột là 200px
        int totalWidth = barWidth * data.size(); // Tính tổng chiều rộng dựa trên số lượng cột
        int maxHeight = getHeight() - 1000; // Chừa thêm không gian cho text bên dưới

        // Đặt chiều rộng của biểu đồ dựa trên số lượng cột
        setMinimumWidth(totalWidth);

        // Tìm giá trị lớn nhất để tỉ lệ hóa chiều cao cột
        int maxValue = 0;
        for (int value : data) {
            if (value > maxValue) {
                maxValue = value;
            }
        }

        for (int i = 0; i < data.size(); i++) {
            int hydrationValue = data.get(i);

            // Tùy chỉnh màu sắc theo mức độ hydration
            if (hydrationValue < maxValue * 0.3) {
                barPaint.setColor(Color.RED); // Mức độ thấp - màu đỏ
            } else if (hydrationValue < maxValue * 0.6) {
                barPaint.setColor(Color.YELLOW); // Mức độ trung bình - màu vàng
            } else {
                barPaint.setColor(Color.GREEN); // Mức độ cao - màu xanh lá cây
            }

            // Tính toán chiều cao cột dựa trên giá trị lớn nhất
            int barHeight = (int) (((float) hydrationValue / maxValue) * maxHeight);
            int left = i * barWidth;
            int top = getHeight() - barHeight - 150; // Chừa không gian cho nhãn dưới
            int right = left + barWidth - 10; // Khoảng cách giữa các cột
            int bottom = getHeight() - 150; // Cột không che mất nhãn dưới

            // Vẽ cột
            canvas.drawRect(left, top, right, bottom, barPaint);

            // Nếu giá trị khác 0 thì mới vẽ giá trị lên trên cột
            if (hydrationValue != 0) {
                canvas.drawText(String.valueOf(hydrationValue), left + (barWidth / 4), top - 20, textPaint);
            }

            // Vẽ nhãn ngày dưới cột (dịch nhãn lên trên)
            canvas.drawText(labels.get(i), left + (barWidth / 4), getHeight() - 80, textPaint); // Dịch nhãn ngày lên cao hơn
        }
    }



}
