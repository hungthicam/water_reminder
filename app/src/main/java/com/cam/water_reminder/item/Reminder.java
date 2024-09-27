package com.cam.water_reminder.item;

public class Reminder {

    public String id;
    public String title;
    public String description;
    public String timestamp;
    public int hour;
    public int minute;

    // Constructor không tham số (cần thiết cho Firebase)
    public Reminder() {
    }

    // Constructor đầy đủ
    public Reminder(String id, String title, String description, String timestamp, int hour, int minute) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.timestamp = timestamp;
        this.hour = hour;
        this.minute = minute;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }
}
