package com.example.app;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class StatsResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("total_tasks")
    private int totalTasks;

    @SerializedName("completed_tasks")
    private int completedTasks;

    @SerializedName("categories")
    private List<CategoryStats> categories;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public List<CategoryStats> getCategories() {
        return categories;
    }
}