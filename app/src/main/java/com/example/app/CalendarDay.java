package com.example.app;

public class CalendarDay {
    private String day_name;
    private int day_number;
    private boolean is_selected;
    private boolean is_today;

    public CalendarDay(String day_name, int day_number, boolean is_selected, boolean is_today) {
        this.day_name = day_name;
        this.day_number = day_number;
        this.is_selected = is_selected;
        this.is_today = is_today;
    }

    // Геттеры и сеттеры
    public String get_day_name() { return day_name; }
    public void set_day_name(String day_name) { this.day_name = day_name; }

    public int get_day_number() { return day_number; }
    public void set_day_number(int day_number) { this.day_number = day_number; }

    public boolean is_selected() { return is_selected; }
    public void set_selected(boolean selected) { is_selected = selected; }

    public boolean is_today() { return is_today; }
    public void set_today(boolean today) { is_today = today; }
}