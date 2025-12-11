package com.example.app.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.TypeConverters;

import com.example.app.data.local.converters.DateConverter;
import java.util.Date;

@Entity(tableName = "users")
public class UserEntity {

    @PrimaryKey
    @ColumnInfo(name = "user_id")
    private int userId;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "email", index = true)
    private String email;

    @ColumnInfo(name = "password_hash")
    private String passwordHash;

    @ColumnInfo(name = "image")
    private String image;

    @ColumnInfo(name = "coins", defaultValue = "0")
    private int coins;

    @TypeConverters(DateConverter.class)
    @ColumnInfo(name = "created_at")
    private Date createdAt;

    @TypeConverters(DateConverter.class)
    @ColumnInfo(name = "updated_at")
    private Date updatedAt;

    @TypeConverters(DateConverter.class)
    @ColumnInfo(name = "last_login")
    private Date lastLogin;

    @ColumnInfo(name = "is_active", defaultValue = "1")
    private boolean isActive;

    @ColumnInfo(name = "is_guest", defaultValue = "0")
    private boolean isGuest;

    @ColumnInfo(name = "guest_id")
    private String guestId; // Для временных гостевых аккаунтов

    @ColumnInfo(name = "sync_status")
    private String syncStatus; // "pending", "synced"

    // Конструкторы
    public UserEntity() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.isActive = true;
        this.syncStatus = "pending";
    }

    // Для гостевого пользователя
    public UserEntity(int userId, String name, boolean isGuest) {
        this();
        this.userId = userId;
        this.name = name;
        this.isGuest = isGuest;
        if (isGuest) {
            this.guestId = "guest_" + System.currentTimeMillis();
            this.email = guestId + "@guest.local";
        }
    }

    // Геттеры и сеттеры
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
        this.updatedAt = new Date();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) {
        this.email = email;
        this.updatedAt = new Date();
    }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.updatedAt = new Date();
    }

    public String getImage() { return image; }
    public void setImage(String image) {
        this.image = image;
        this.updatedAt = new Date();
    }

    public int getCoins() { return coins; }
    public void setCoins(int coins) {
        this.coins = coins;
        this.updatedAt = new Date();
    }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public Date getLastLogin() { return lastLogin; }
    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
        this.updatedAt = new Date();
    }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) {
        isActive = active;
        this.updatedAt = new Date();
    }

    public boolean isGuest() { return isGuest; }
    public void setGuest(boolean guest) {
        isGuest = guest;
        this.updatedAt = new Date();
    }

    public String getGuestId() { return guestId; }
    public void setGuestId(String guestId) {
        this.guestId = guestId;
        this.updatedAt = new Date();
    }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
        this.updatedAt = new Date();
    }
}