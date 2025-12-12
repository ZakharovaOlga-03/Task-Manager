package com.example.app;

import com.google.gson.annotations.SerializedName;

public class Task {
    // Поля для отображения
    private String time;
    private String icon_name;
    private String title;
    private String duration;
    private String status;
    private boolean is_completed;

    // Поля для удобства (могут дублировать API поля)
    private int id; // Для внутреннего использования

    // Поля для БД (через API) - ТОЧНО КАК В БАЗЕ ДАННЫХ
    @SerializedName("id_task")
    private int idTask;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("task_name")
    private String taskName;

    @SerializedName("task_type")
    private String taskType;

    @SerializedName("task_importance")
    private String taskImportance;

    @SerializedName("task_goal_date")
    private String taskGoalDate;

    @SerializedName("notify_start")
    private String notifyStart;

    @SerializedName("notify_frequency")
    private String notifyFrequency;

    @SerializedName("notify_type")
    private String notifyType;

    @SerializedName("task_note")
    private String taskNote;

    @SerializedName("task_reward")
    private int taskReward;

    @SerializedName("task_creation_date")
    private String taskCreationDate;

    // УДАЛИТЕ ЭТО ПОЛЕ - оно конфликтует с is_completed
    // @SerializedName("is_completed")
    // private boolean dbCompleted;

    // Вместо этого добавьте поле для статуса из БД
    @SerializedName("task_status")
    private String taskStatus;

    @SerializedName("completed_date")
    private String taskCompletionDate;

    // Конструктор для отображения
    public Task(String time, String icon_name, String title, String duration, String status, boolean is_completed) {
        this.time = time;
        this.icon_name = icon_name;
        this.title = title;
        this.duration = duration;
        this.status = status;
        this.is_completed = is_completed;
    }

    // Конструктор по умолчанию для API
    public Task() {}

    // ========== ГЕТТЕРЫ И СЕТТЕРЫ ДЛЯ ОТОБРАЖЕНИЯ ==========
    public String get_time() { return time; }
    public String get_icon_name() { return icon_name; }
    public String get_title() { return title; }
    public String get_duration() { return duration; }
    public String get_status() { return status; }
    public boolean is_completed() { return is_completed; }

    public void set_time(String time) { this.time = time; }
    public void set_icon_name(String icon_name) { this.icon_name = icon_name; }
    public void set_title(String title) { this.title = title; }
    public void set_duration(String duration) { this.duration = duration; }
    public void set_status(String status) { this.status = status; }
    public void set_completed(boolean completed) { is_completed = completed; }

    // ========== УДОБНЫЕ МЕТОДЫ ДЛЯ РАБОТЫ С ID ==========
    public int getId() {
        // Возвращаем либо id, либо idTask если id не установлен
        return (id != 0) ? id : idTask;
    }

    public void setId(int id) {
        this.id = id;
        // Также обновляем idTask для консистентности
        this.idTask = id;
    }

    // ========== ГЕТТЕРЫ ДЛЯ API ==========
    public int getIdTask() { return idTask; }
    public int getUserId() { return userId; }
    public String getTaskName() { return taskName; }
    public String getTaskType() { return taskType; }
    public String getTaskImportance() { return taskImportance; }
    public String getTaskGoalDate() { return taskGoalDate; }
    public String getNotifyStart() { return notifyStart; }
    public String getNotifyFrequency() { return notifyFrequency; }
    public String getNotifyType() { return notifyType; }
    public String getTaskNote() { return taskNote; }
    public int getTaskReward() { return taskReward; }
    public String getTaskCreationDate() { return taskCreationDate; }
    public String getTaskStatus() { return taskStatus; }
    public String getTaskCompletionDate() { return taskCompletionDate; }

    // ========== СЕТТЕРЫ ДЛЯ API ==========
    public void setIdTask(int idTask) {
        this.idTask = idTask;
        this.id = idTask; // Также сохраняем в удобное поле
    }

    public void setUserId(int userId) { this.userId = userId; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public void setTaskImportance(String taskImportance) { this.taskImportance = taskImportance; }
    public void setTaskGoalDate(String taskGoalDate) { this.taskGoalDate = taskGoalDate; }
    public void setNotifyStart(String notifyStart) { this.notifyStart = notifyStart; }
    public void setNotifyFrequency(String notifyFrequency) { this.notifyFrequency = notifyFrequency; }
    public void setNotifyType(String notifyType) { this.notifyType = notifyType; }
    public void setTaskNote(String taskNote) { this.taskNote = taskNote; }
    public void setTaskReward(int taskReward) { this.taskReward = taskReward; }
    public void setTaskCreationDate(String taskCreationDate) { this.taskCreationDate = taskCreationDate; }
    public void setTaskStatus(String taskStatus) { this.taskStatus = taskStatus; }
    public void setTaskCompletionDate(String taskCompletionDate) { this.taskCompletionDate = taskCompletionDate; }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    // Получить удобное имя иконки на основе типа задачи
    public String getDisplayIconName() {
        if (icon_name != null && !icon_name.isEmpty()) {
            return icon_name;
        }
        // Если icon_name не установлен, определяем по taskType
        return getIconNameFromTaskType();
    }

    private String getIconNameFromTaskType() {
        if (taskType == null) return "book";

        switch (taskType.toLowerCase()) {
            case "book":
            case "чтение":
                return "book";
            case "meeting":
            case "встреча":
                return "meeting";
            case "coffee":
            case "отдых":
                return "coffee";
            case "heart":
            case "личное":
                return "heart";
            default:
                return "book";
        }
    }

    // Получить удобное название задачи
    public String getDisplayTitle() {
        if (title != null && !title.isEmpty()) {
            return title;
        }
        return (taskName != null && !taskName.isEmpty()) ? taskName : "Новая задача";
    }

    // Проверить, есть ли у задачи ID
    public boolean hasId() {
        return id != 0 || idTask != 0;
    }

    // Метод для определения, выполнена ли задача по данным из БД
    public boolean isCompletedFromDB() {
        // Проверяем поле task_status
        if (taskStatus != null &&
                (taskStatus.equals("completed") || taskStatus.equals("выполнено"))) {
            return true;
        }

        // Проверяем дату завершения
        if (taskCompletionDate != null && !taskCompletionDate.isEmpty()) {
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + getId() +
                ", title='" + get_title() + '\'' +
                ", taskName='" + taskName + '\'' +
                ", status='" + get_status() + '\'' +
                ", taskStatus='" + taskStatus + '\'' +
                ", is_completed=" + is_completed +
                ", taskCompletionDate='" + taskCompletionDate + '\'' +
                '}';
    }
}