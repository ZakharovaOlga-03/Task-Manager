package com.example.app;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("idusers")
    private int idusers;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("image")
    private String image;

    @SerializedName("coins")
    private int coins;

    @SerializedName("created_at")
    private String created_at;

    @SerializedName("updated_at")
    private String updated_at;

    @SerializedName("last_login")
    private String last_login;

    @SerializedName("is_active")
    private boolean is_active;

    // Для регистрации
    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.coins = 0;
        this.is_active = true;
    }

    // Пустой конструктор для Gson
    public User() {}

    // Геттеры и сеттеры
    public int getIdusers() { return idusers; }
    public void setIdusers(int idusers) { this.idusers = idusers; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }

    public String getCreatedAt() { return created_at; }
    public void setCreatedAt(String created_at) { this.created_at = created_at; }

    public String getUpdatedAt() { return updated_at; }
    public void setUpdatedAt(String updated_at) { this.updated_at = updated_at; }

    public String getLastLogin() { return last_login; }
    public void setLastLogin(String last_login) { this.last_login = last_login; }

    public boolean isActive() { return is_active; }
    public void setActive(boolean active) { is_active = active; }

    @Override
    public String toString() {
        return "User{" +
                "idusers=" + idusers +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", coins=" + coins +
                '}';
    }
}