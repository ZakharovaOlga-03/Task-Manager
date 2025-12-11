package com.example.app;

import com.google.gson.annotations.SerializedName;

public class UserProfile {
    @SerializedName("id_user")
    private int idUser;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("account_type")
    private String accountType;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("premium_until")
    private String premiumUntil;

    public int getIdUser() {
        return idUser;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getAccountType() {
        return accountType;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getPremiumUntil() {
        return premiumUntil;
    }

    public String getDisplayAccountType() {
        if (accountType == null) return "Базовый";

        switch (accountType.toLowerCase()) {
            case "premium":
            case "премиум":
                return "Premium";
            case "pro":
            case "про":
                return "Pro";
            default:
                return "Базовый";
        }
    }
}