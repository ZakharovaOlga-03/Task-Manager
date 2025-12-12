package com.example.app;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.example.app.data.local.AppDatabase;
import com.example.app.data.local.entities.TaskEntity;

import java.util.List;

public class SyncService extends Service {
    private static final String TAG = "SyncService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SyncService started");

        // Запустить синхронизацию в отдельном потоке
        new Thread(() -> {
            syncPendingTasks();
            stopSelf(); // Остановить сервис после завершения
        }).start();

        return START_STICKY;
    }

    private void syncPendingTasks() {
        try {
            List<TaskEntity> pendingTasks = AppDatabase.getInstance(this)
                    .taskDao()
                    .getPendingSyncTasks();

            Log.d(TAG, "Found " + pendingTasks.size() + " tasks to sync");

            for (TaskEntity task : pendingTasks) {
                syncSingleTask(task);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error syncing tasks: " + e.getMessage());
        }
    }

    private void syncSingleTask(TaskEntity task) {
        // Здесь будет логика отправки на сервер
        // Используйте ваш существующий ApiService
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}