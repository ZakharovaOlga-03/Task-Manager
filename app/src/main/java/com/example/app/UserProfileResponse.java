package com.example.app;

import com.google.gson.annotations.SerializedName;

public class UserProfileResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("user")
    private UserProfile user;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public UserProfile getUser() {
        return user;
    }
}