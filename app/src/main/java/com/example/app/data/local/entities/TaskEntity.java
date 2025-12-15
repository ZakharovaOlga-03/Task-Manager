package com.example.app.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.TypeConverters;
import androidx.annotation.NonNull;

import com.example.app.Task;
import com.example.app.data.local.converters.DateConverter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Entity(tableName = "tasks")
public class TaskEntity {

    // Локальный ID (автоинкремент)
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "local_id")
    private int localId;

    // ID с сервера (0 если не синхронизировано)
    @ColumnInfo(name = "server_id", defaultValue = "0")
    private int serverId;

    // ID пользователя (владельца задачи)
    @ColumnInfo(name = "user_id", index = true)
    private int userId;

    // Основные поля задачи
    @ColumnInfo(name = "task_name")
    private String taskName;

    @ColumnInfo(name = "task_type")
    private String taskType;

    @ColumnInfo(name = "task_importance")
    private String taskImportance;

    @TypeConverters(DateConverter.class)
    @ColumnInfo(name = "task_goal_date")
    private Date taskGoalDate;

    @TypeConverters(DateConverter.class)
    @ColumnInfo(name = "notify_start")
    private Date notifyStart;

    @ColumnInfo(name = "notify_frequency")
    private String notifyFrequency;

    @ColumnInfo(name = "notify_type")
    private String notifyType;

    @ColumnInfo(name = "task_note")
    private String taskNote;

    @ColumnInfo(name = "task_reward")
    private int taskReward;

    @TypeConverters(DateConverter.class)
    @ColumnInfo(name = "task_creation_date")
    private Date taskCreationDate;

    // Статус задачи
    @ColumnInfo(name = "status")

    private String status; // "pending", "in_progress", "completed", "cancelled"private boolean status; // "pending", "in_progress", "completed", "cancelled"

    private int status; // 0, 1


    @ColumnInfo(name = "completed_at")
    @TypeConverters(DateConverter.class)
    private Date completedAt;

    // Статус синхронизации
    @ColumnInfo(name = "sync_status")
    private String syncStatus; // "pending", "synced", "failed", "deleted"

    @TypeConverters(DateConverter.class)
    @ColumnInfo(name = "last_sync_date")
    private Date lastSyncDate;

    // Для отслеживания изменений
    @TypeConverters(DateConverter.class)
    @ColumnInfo(name = "updated_at")
    private Date updatedAt;

    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    private boolean isDeleted;

    // Конструкторы
    public TaskEntity() {
        this.taskCreationDate = new Date();
        this.syncStatus = "pending";
        this.status = 0;
        this.updatedAt = new Date();
    }

    // Геттеры и сеттеры
    public int getLocalId() { return localId; }
    public void setLocalId(int localId) { this.localId = localId; }

    public int getServerId() { return serverId; }
    public void setServerId(int serverId) {
        this.serverId = serverId;
        this.updatedAt = new Date();
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) {
        this.userId = userId;
        this.updatedAt = new Date();
    }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) {
        this.taskName = taskName;
        this.updatedAt = new Date();
    }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) {
        this.taskType = taskType;
        this.updatedAt = new Date();
    }

    public String getTaskImportance() { return taskImportance; }
    public void setTaskImportance(String taskImportance) {
        this.taskImportance = taskImportance;
        this.updatedAt = new Date();
    }

    public Date getTaskGoalDate() { return taskGoalDate; }
    public void setTaskGoalDate(Date taskGoalDate) {
        this.taskGoalDate = taskGoalDate;
        this.updatedAt = new Date();
    }

    public Date getNotifyStart() { return notifyStart; }
    public void setNotifyStart(Date notifyStart) {
        this.notifyStart = notifyStart;
        this.updatedAt = new Date();
    }

    public String getNotifyFrequency() { return notifyFrequency; }
    public void setNotifyFrequency(String notifyFrequency) {
        this.notifyFrequency = notifyFrequency;
        this.updatedAt = new Date();
    }

    public String getNotifyType() { return notifyType; }
    public void setNotifyType(String notifyType) {
        this.notifyType = notifyType;
        this.updatedAt = new Date();
    }

    public String getTaskNote() { return taskNote; }
    public void setTaskNote(String taskNote) {
        this.taskNote = taskNote;
        this.updatedAt = new Date();
    }

    public int getTaskReward() { return taskReward; }
    public void setTaskReward(int taskReward) {
        this.taskReward = taskReward;
        this.updatedAt = new Date();
    }

    public Date getTaskCreationDate() { return taskCreationDate; }
    public void setTaskCreationDate(Date taskCreationDate) { this.taskCreationDate = taskCreationDate; }

    public int getStatus() { return status; }
    public void setStatus(int status) {
        this.status = status;
        this.updatedAt = new Date();

        if (status == 1) {
            this.completedAt = new Date();
        }
    }

    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
        if ("synced".equals(syncStatus)) {
            this.lastSyncDate = new Date();
        }
        this.updatedAt = new Date();
    }

    public Date getLastSyncDate() { return lastSyncDate; }
    public void setLastSyncDate(Date lastSyncDate) { this.lastSyncDate = lastSyncDate; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
        this.updatedAt = new Date();
        if (deleted) {
            this.syncStatus = "deleted";
        }
    }
}