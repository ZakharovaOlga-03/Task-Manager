package com.example.app;

import com.google.gson.annotations.SerializedName;

public class CategoryStats {
    @SerializedName("category_name")
    private String categoryName;

    @SerializedName("category_display_name")
    private String categoryDisplayName;

    @SerializedName("total_tasks")
    private int totalTasks;

    @SerializedName("completed_tasks")
    private int completedTasks;

    @SerializedName("percentage")
    private int percentage;

    @SerializedName("color")
    private String color;

    public String getCategoryName() {
        return categoryName;
    }

    public String getCategoryDisplayName() {
        return categoryDisplayName;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public int getPercentage() {
        return percentage;
    }

    public String getColor() {
        return color;
    }
}