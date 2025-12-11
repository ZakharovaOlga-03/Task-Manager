package com.example.app;

import com.google.gson.annotations.SerializedName;

public class UpdateTaskResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("task_id")
    private int taskId;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public int getTaskId() {
        return taskId;
    }
}