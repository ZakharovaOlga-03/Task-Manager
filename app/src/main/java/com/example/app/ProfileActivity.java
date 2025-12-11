package com.example.app;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private ApiService apiService;
    private int currentUserId;

    private TextView tv_name_header;
    private TextView tv_email_header;
    private TextView tv_name_details;
    private TextView tv_email_details;
    private TextView tv_account_type;
    private Button btn_edit_profile;
    private Button btn_logout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Проверка авторизации
        if (!SharedPrefs.isLoggedIn(this)) {
            Toast.makeText(this, "Пожалуйста, войдите в систему", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = SharedPrefs.getUserId(this);
        if (currentUserId == -1) {
            Toast.makeText(this, "Ошибка получения данных пользователя", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setup_transparent_navigation();
        setContentView(R.layout.activity_profile);

        // Инициализация Retrofit
        apiService = RetrofitClient.getApiService();

        initViews();
        setupListeners();
        loadUserProfile();

        NavigationHelper.setup_navigation(this, 4);
    }

    private void initViews() {
        tv_name_header = findViewById(R.id.tv_name);
        tv_email_header = findViewById(R.id.tv_email);
        tv_name_details = findViewById(R.id.tv_name_details);
        tv_email_details = findViewById(R.id.tv_email_details);
        tv_account_type = findViewById(R.id.tv_account_type);
        btn_edit_profile = findViewById(R.id.btn_edit_profile);
        btn_logout = findViewById(R.id.btn_logout);
    }

    private void setupListeners() {
        btn_edit_profile.setOnClickListener(v -> {
            Toast.makeText(this, "Переход к редактированию профиля", Toast.LENGTH_SHORT).show();
            // TODO: Реализовать редактирование профиля
        });

        btn_logout.setOnClickListener(v -> performLogout());
    }

    private void loadUserProfile() {
        Log.d(TAG, "Loading profile for user: " + currentUserId);

        Call<UserProfileResponse> call = apiService.getProfile(currentUserId);
        call.enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserProfileResponse profileResponse = response.body();

                    if (profileResponse.isSuccess() && profileResponse.getUser() != null) {
                        updateUIWithUserData(profileResponse.getUser());
                    } else {
                        Log.e(TAG, "Failed to load profile: " + profileResponse.getMessage());
                        showDefaultData();
                    }
                } else {
                    Log.e(TAG, "Server error loading profile: " + response.code());
                    showDefaultData();
                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                Log.e(TAG, "Network error loading profile: " + t.getMessage());
                showDefaultData();
            }
        });
    }

    private void updateUIWithUserData(UserProfile user) {
        // Обновляем заголовок
        tv_name_header.setText(user.getName());
        tv_email_header.setText(user.getEmail());

        // Обновляем детальную информацию
        tv_name_details.setText("Имя: " + user.getName());
        tv_email_details.setText("Email: " + user.getEmail());
        tv_account_type.setText("Тип аккаунта: " + user.getDisplayAccountType());

        // Если есть дата окончания премиума, можно добавить
        if (user.getPremiumUntil() != null && !user.getPremiumUntil().isEmpty()) {
            // Можно добавить дополнительную информацию о премиуме
        }

        Log.d(TAG, "Profile loaded: " + user.getName() + ", " + user.getEmail());
    }

    private void showDefaultData() {
        // Показываем данные из SharedPrefs или дефолтные
        String savedName = SharedPrefs.getUserName(this);
        String savedEmail = SharedPrefs.getUserEmail(this);

        if (savedName != null && savedEmail != null) {
            tv_name_header.setText(savedName);
            tv_email_header.setText(savedEmail);
            tv_name_details.setText("Имя: " + savedName);
            tv_email_details.setText("Email: " + savedEmail);
        }

        tv_account_type.setText("Тип аккаунта: Базовый");

        Toast.makeText(this, "Используются сохраненные данные", Toast.LENGTH_SHORT).show();
    }

    private void performLogout() {
        Log.d(TAG, "Logging out user: " + currentUserId);

        Call<ApiResponse> call = apiService.logout(new LogoutRequest(currentUserId));
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                // Независимо от ответа сервера, выполняем локальный выход
                localLogout();
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                // Даже при ошибке сети выполняем локальный выход
                localLogout();
            }
        });
    }

    private void localLogout() {
        // Очищаем SharedPrefs
        SharedPrefs.clearUserData(this);

        // Завершаем активность
        Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
        finish();

        // Можно запустить LoginActivity
        // startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
    }

    private void setup_transparent_navigation() {
        Window window = getWindow();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false);
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }

        window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
        window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightNavigationBars(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "ProfileActivity resumed");
        // При возвращении в активность можно обновить данные
        if (currentUserId != -1) {
            loadUserProfile();
        }
    }
}