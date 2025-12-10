package com.example.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SplashActivity extends AppCompatActivity {

    private AuthManager authManager;
    private Handler handler = new Handler();
    private static final int AUTO_REDIRECT_DELAY = 2000; // 2 секунды

    @Override
    protected void onCreate(Bundle saved_instance_state) {
        super.onCreate(saved_instance_state);
        setContentView(R.layout.activity_splash);

        authManager = new AuthManager(this);

        TextView time_view = findViewById(R.id.splash_time);
        Button start_btn = findViewById(R.id.start_btn);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        time_view.setText(sdf.format(new Date()));

        // Проверяем авторизацию сразу при запуске
        checkAuthAndRedirect();

        // Обработчик кнопки "Начать"
        start_btn.setOnClickListener(v -> {
            // Отменяем автоматический переход
            handler.removeCallbacksAndMessages(null);
            // Запускаем ручной переход
            redirectBasedOnAuth();
        });

        // Автоматический переход через 2 секунды
        handler.postDelayed(() -> {
            redirectBasedOnAuth();
        }, AUTO_REDIRECT_DELAY);
    }

    // Метод для проверки авторизации и перенаправления
    private void checkAuthAndRedirect() {
        // Можно предзагрузить какие-то данные здесь
        // Например, проверить наличие локальных задач
    }

    private void redirectBasedOnAuth() {
        if (authManager.isLoggedIn()) {
            // Пользователь авторизован - сразу на MainActivity
            startMainActivity();
        } else {
            // Не авторизован - на экран входа
            startLoginActivity();
        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void startLoginActivity() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Очищаем Handler при уничтожении Activity
        handler.removeCallbacksAndMessages(null);
    }
}