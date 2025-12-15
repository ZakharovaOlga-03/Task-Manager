package com.example.app.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;

public class NotificationHelper {
    private Context context;

    public static final String CHANNEL_ID = "task_reminder_channel";
    public static final String CHANNEL_NAME = "Напоминания о задачах";
    public static final String CHANNEL_DESC = "Уведомления о предстоящих и просроченных задачах";
    public static final int NOTIFICATION_ID = 1001;

    public NotificationHelper(Context context) {
        this.context = context;
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
            channel.setDescription(CHANNEL_DESC);
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            channel.enableVibration(true);

            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }

    // ПРОВЕРКА РАЗРЕШЕНИЯ (добавьте этот метод)
    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Для Android ниже 13 разрешение не требуется
    }

    // Метод для показа уведомления о задаче
    public void showTaskReminder(String taskTitle, String dueDate, boolean isOverdue) {
        // Проверяем разрешение
        if (!hasNotificationPermission()) {
            return; // Не показываем, если нет разрешения
        }

        String title = isOverdue ? "⚠️ ПРОСРОЧЕНО: " + taskTitle : "📅 Напоминание: " + taskTitle;
        String message = isOverdue ? "Задача просрочена! Срок: " + dueDate : "Срок выполнения: " + dueDate;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
    }

    // Метод для тестового уведомления
    public void showTestNotification() {
        // Проверяем разрешение
        if (!hasNotificationPermission()) {
            return; // Не показываем, если нет разрешения
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("✅ Task Manager работает!")
                .setContentText("Уведомления настроены правильно")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + 1, builder.build());
    }

    // Проверка, включены ли уведомления
    public boolean areNotificationsEnabled() {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
                && hasNotificationPermission(); // Добавили проверку разрешения
    }
}