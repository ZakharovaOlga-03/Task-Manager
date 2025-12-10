package com.example.app;

import android.app.Application;
import android.util.Log;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Обработчик необработанных исключений
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e("GlobalException", "Uncaught exception: ", throwable);
            // Можно отправить лог на сервер или показать сообщение
        });
    }
}