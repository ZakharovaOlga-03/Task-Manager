package com.example.app;

import android.app.Application;
import android.util.Log;
import com.example.app.data.local.AppDatabase;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Обработчик необработанных исключений
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e("GlobalException", "Uncaught exception: ", throwable);
            // Можно отправить лог на сервер или показать сообщение
        });
        // Инициализация БД при запуске приложения
        AppDatabase.getInstance(this);

        // Можно инициализировать системные данные
        initializeSystemData();
    }

    private void initializeSystemData() {
        // Создать системные типы задач, если их нет
        new Thread(() -> {
            // Логика инициализации
        }).start();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // Закрыть БД при завершении приложения
        AppDatabase.closeDatabase();
    }
}