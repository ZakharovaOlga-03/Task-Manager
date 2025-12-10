package com.example.app;

import com.example.app.Task;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TasksResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("tasks")
    private List<Task> tasks;

    @SerializedName("message")
    private String message;

    @SerializedName("task_count")
    private int taskCount;

    // Геттеры и сеттеры
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(int taskCount) {
        this.taskCount = taskCount;
    }
}