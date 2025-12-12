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
    private static final int AUTO_REDIRECT_DELAY = 7000; // 7 секунд

    @Override
    protected void onCreate(Bundle saved_instance_state) {
        super.onCreate(saved_instance_state);
        setContentView(R.layout.activity_splash);

        authManager = new AuthManager(this);

        TextView time_view = findViewById(R.id.splash_time);
        Button start_btn = findViewById(R.id.start_btn);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        time_view.setText(sdf.format(new Date()));

        // Обработчик кнопки "Начать"
        start_btn.setOnClickListener(v -> {
            // Отменяем автоматический переход
            handler.removeCallbacksAndMessages(null);
            // Запускаем ручной переход
            checkAndRestoreSession();
        });

        // Автоматический переход через 2 секунды
        handler.postDelayed(this::checkAndRestoreSession, AUTO_REDIRECT_DELAY);
    }

    private void checkAndRestoreSession() {
        authManager.checkAndRestoreSession(new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(AuthResponse response) {
                runOnUiThread(() -> {
                    // Уже авторизован - сразу переходим на MainActivity
                    startMainActivity();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    // Не авторизован - показываем экран выбора
                    startLoginActivity();
                });
            }
        });
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