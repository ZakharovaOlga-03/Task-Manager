package com.example.app.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.app.data.local.AppDatabase;
import com.example.app.data.local.dao.TaskDao;
import com.example.app.data.local.entities.TaskEntity;
import com.example.app.utils.NetworkUtils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskRepository {

    private TaskDao taskDao;
    private ExecutorService executorService;
    private NetworkUtils networkUtils;
    private MutableLiveData<Integer> syncStatus = new MutableLiveData<>();

    public TaskRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        this.taskDao = database.taskDao();
        this.executorService = Executors.newSingleThreadExecutor();
        this.networkUtils = new NetworkUtils(application);
    }

    // Основные методы для UI

    public LiveData<List<TaskEntity>> getTasksByUser(int userId) {
        return taskDao.getTasksByUser(userId);
    }

    public LiveData<List<TaskEntity>> getTasksForDate(int userId, Date date) {
        return taskDao.getTasksForDate(userId, date);
    }

    public LiveData<List<TaskEntity>> getTasksByStatus(int userId, String status) {
        return taskDao.getTasksByStatus(userId, status);
    }

    // Создание задачи (оффлайн-первый подход)
    public void createTask(TaskEntity task, TaskCallback callback) {
        executorService.execute(() -> {
            try {
                // 1. Сохраняем локально
                long localId = taskDao.insertTask(task);
                task.setLocalId((int) localId);

                // 2. Если есть интернет - синхронизируем
                if (networkUtils.isNetworkAvailable()) {
                    syncTaskToServer(task);
                }

                callback.onSuccess(task);

            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    // Обновление задачи
    public void updateTask(TaskEntity task, TaskCallback callback) {
        executorService.execute(() -> {
            try {
                task.setSyncStatus("pending");
                taskDao.updateTask(task);

                if (networkUtils.isNetworkAvailable()) {
                    syncTaskToServer(task);
                }

                callback.onSuccess(task);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    // Удаление задачи (мягкое удаление)
    public void deleteTask(TaskEntity task, TaskCallback callback) {
        executorService.execute(() -> {
            try {
                task.setDeleted(true);
                task.setSyncStatus("pending");
                taskDao.updateTask(task);

                if (networkUtils.isNetworkAvailable()) {
                    // Отправляем запрос на удаление на сервер
                }

                callback.onSuccess(task);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    // Пометить задачу выполненной
    public void markTaskCompleted(int taskId, int reward, TaskCallback callback) {
        executorService.execute(() -> {
            try {
                TaskEntity task = taskDao.getTaskByLocalId(taskId);
                if (task != null) {
                    task.setStatus("completed");
                    task.setTaskReward(reward);
                    task.setSyncStatus("pending");
                    taskDao.updateTask(task);

                    callback.onSuccess(task);
                } else {
                    callback.onError("Задача не найдена");
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    // Синхронизация с сервером
    private void syncTaskToServer(TaskEntity task) {
        // Здесь будет логика отправки на ваш сервер через Retrofit
        // После успешной отправки:
        // taskDao.markAsSynced(task.getLocalId(), serverResponse.getId(), new Date());
    }

    // Получить задачи для синхронизации
    public List<TaskEntity> getPendingSyncTasks() {
        return taskDao.getPendingSyncTasks();
    }

    // Интерфейс обратного вызова
    public interface TaskCallback {
        void onSuccess(TaskEntity task);
        void onError(String error);
    }
}