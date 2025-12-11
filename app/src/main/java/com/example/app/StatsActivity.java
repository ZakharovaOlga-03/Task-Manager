package com.example.app;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import java.util.HashMap;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatsActivity extends AppCompatActivity {

    private static final String TAG = "StatsActivity";

    private ApiService apiService;
    private int currentUserId;

    // UI элементы
    private TextView tvTotalTasks;
    private TextView tvCompletedTasks;
    private ProgressBar pbWork;
    private ProgressBar pbPersonal;
    private ProgressBar pbHealth;
    private ProgressBar pbStudy;
    private TextView tvWorkPercent;
    private TextView tvPersonalPercent;
    private TextView tvHealthPercent;
    private TextView tvStudyPercent;

    // Мапы для категорий
    private HashMap<String, String> categoryDisplayNames = new HashMap<String, String>() {{
        put("book", "Учеба");
        put("meeting", "Работа");
        put("coffee", "Здоровье");
        put("heart", "Личное");
        put("reading", "Учеба");
        put("work", "Работа");
        put("health", "Здоровье");
        put("personal", "Личное");
    }};

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
        setContentView(R.layout.activity_stats);

        // Инициализация Retrofit
        apiService = RetrofitClient.getApiService();

        initViews();
        setupNavigation();
        loadStats();

        // Настройка отступов для навигации
        setupNavigationPadding();
    }

    private void initViews() {
        // Находим все элементы
        tvTotalTasks = findViewById(R.id.tv_total_tasks);
        tvCompletedTasks = findViewById(R.id.tv_completed_tasks);

        // Прогресс-бары
        pbWork = findViewById(R.id.pb_work);
        pbPersonal = findViewById(R.id.pb_personal);
        pbHealth = findViewById(R.id.pb_health);
        pbStudy = findViewById(R.id.pb_study);

        // Проценты
        tvWorkPercent = findViewById(R.id.tv_work_percent);
        tvPersonalPercent = findViewById(R.id.tv_personal_percent);
        tvHealthPercent = findViewById(R.id.tv_health_percent);
        tvStudyPercent = findViewById(R.id.tv_study_percent);

        // Кнопки меню и поиска
        View menu_btn = findViewById(R.id.menu_btn);
        View search_btn = findViewById(R.id.search_btn);

        if (menu_btn != null) {
            menu_btn.setOnClickListener(v -> {
                Toast.makeText(this, "Меню", Toast.LENGTH_SHORT).show();
            });
        }

        if (search_btn != null) {
            search_btn.setOnClickListener(v -> {
                Toast.makeText(this, "Поиск", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupNavigation() {
        NavigationHelper.setup_navigation(this, 3);
    }

    private void setupNavigationPadding() {
        ScrollView scrollView = findViewById(R.id.scroll_view);
        int navigationBarHeightPx = (int) (80 * getResources().getDisplayMetrics().density);
        scrollView.setPadding(scrollView.getPaddingLeft(), scrollView.getPaddingTop(),
                scrollView.getPaddingRight(), navigationBarHeightPx);
        scrollView.setClipToPadding(false);

        LinearLayout bottom_card = findViewById(R.id.bottom_card);
        float translationYdp = 20f;
        float translationYpx = translationYdp * getResources().getDisplayMetrics().density;
        bottom_card.setTranslationY(translationYpx);
    }

    private void loadStats() {
        Log.d(TAG, "Loading stats for user: " + currentUserId);

        // Пытаемся загрузить статистику через API
        Call<StatsResponse> call = apiService.getStats(currentUserId);
        call.enqueue(new Callback<StatsResponse>() {
            @Override
            public void onResponse(Call<StatsResponse> call, Response<StatsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    StatsResponse statsResponse = response.body();

                    if (statsResponse.isSuccess()) {
                        runOnUiThread(() -> {
                            updateStatsUI(statsResponse);
                        });
                    } else {
                        runOnUiThread(() -> {
                            Log.w(TAG, "API returned unsuccessful: " + statsResponse.getMessage());
                            loadTasksAndCalculateStats();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Log.e(TAG, "Server error: " + response.code());
                        loadTasksAndCalculateStats();
                    });
                }
            }

            @Override
            public void onFailure(Call<StatsResponse> call, Throwable t) {
                Log.e(TAG, "Network error loading stats: " + t.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(StatsActivity.this,
                            "Ошибка соединения, загружаем данные из задач...", Toast.LENGTH_SHORT).show();
                    loadTasksAndCalculateStats();
                });
            }
        });
    }

    private void loadTasksAndCalculateStats() {
        // Если не удалось загрузить статистику, загружаем задачи и вычисляем сами
        Call<TasksResponse> call = apiService.getTasks(currentUserId);
        call.enqueue(new Callback<TasksResponse>() {
            @Override
            public void onResponse(Call<TasksResponse> call, Response<TasksResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TasksResponse tasksResponse = response.body();

                    if (tasksResponse.isSuccess() && tasksResponse.getTasks() != null) {
                        List<Task> allTasks = tasksResponse.getTasks();
                        calculateAndDisplayStats(allTasks);
                    } else {
                        showDefaultStats();
                    }
                } else {
                    showDefaultStats();
                }
            }

            @Override
            public void onFailure(Call<TasksResponse> call, Throwable t) {
                showDefaultStats();
            }
        });
    }

    private void updateStatsUI(StatsResponse stats) {
        // Общее количество задач
        tvTotalTasks.setText(String.valueOf(stats.getTotalTasks()));
        tvCompletedTasks.setText(String.valueOf(stats.getCompletedTasks()));

        // Статистика по категориям
        if (stats.getCategories() != null && !stats.getCategories().isEmpty()) {
            updateCategoriesUI(stats.getCategories());
        } else {
            // Если API не вернул категории, вычисляем их из задач
            loadTasksAndCalculateStats();
        }
    }

    private void updateCategoriesUI(List<CategoryStats> categories) {
        // Сбрасываем все прогресс-бары
        resetProgressBars();

        // Обновляем каждую категорию
        for (CategoryStats category : categories) {
            String displayName = category.getCategoryDisplayName();
            int percentage = category.getPercentage();

            if ("Работа".equals(displayName)) {
                pbWork.setProgress(percentage);
                tvWorkPercent.setText(percentage + "%");
            } else if ("Личное".equals(displayName)) {
                pbPersonal.setProgress(percentage);
                tvPersonalPercent.setText(percentage + "%");
            } else if ("Здоровье".equals(displayName)) {
                pbHealth.setProgress(percentage);
                tvHealthPercent.setText(percentage + "%");
            } else if ("Учеба".equals(displayName)) {
                pbStudy.setProgress(percentage);
                tvStudyPercent.setText(percentage + "%");
            }
        }
    }

    private void calculateAndDisplayStats(List<Task> tasks) {
        // Инициализируем счетчики
        HashMap<String, Integer> categoryTotal = new HashMap<>();
        HashMap<String, Integer> categoryCompleted = new HashMap<>();

        // Подсчитываем задачи по категориям
        for (Task task : tasks) {
            String taskType = task.getTaskType();
            if (taskType != null) {
                // Приводим к нижнему регистру для сравнения
                taskType = taskType.toLowerCase();

                // Определяем категорию
                String category = determineCategory(taskType);

                // Увеличиваем счетчик задач в категории
                int currentTotal = categoryTotal.getOrDefault(category, 0);
                categoryTotal.put(category, currentTotal + 1);

                // Проверяем, выполнена ли задача
                if (task.is_completed()) {
                    int currentCompleted = categoryCompleted.getOrDefault(category, 0);
                    categoryCompleted.put(category, currentCompleted + 1);
                }

                // Если у задачи нет явного флага выполнения, можно определить по статусу
                if (task.get_status() != null && task.get_status().equals("выполнено")) {
                    int currentCompleted = categoryCompleted.getOrDefault(category, 0);
                    categoryCompleted.put(category, currentCompleted + 1);
                }
            }
        }

        // Вычисляем общие счетчики - делаем финальные копии
        final int totalTasks = tasks.size();
        int completedCount = 0;
        for (Integer completed : categoryCompleted.values()) {
            completedCount += completed;
        }
        final int completedTasks = completedCount;

        // Создаем финальные копии для использования в лямбде
        final HashMap<String, Integer> finalCategoryTotal = new HashMap<>(categoryTotal);
        final HashMap<String, Integer> finalCategoryCompleted = new HashMap<>(categoryCompleted);

        // Обновляем UI
        runOnUiThread(() -> {
            tvTotalTasks.setText(String.valueOf(totalTasks));
            tvCompletedTasks.setText(String.valueOf(completedTasks));

            // Обновляем прогресс-бары
            updateCategoryProgress("Работа", finalCategoryTotal, finalCategoryCompleted, pbWork, tvWorkPercent);
            updateCategoryProgress("Личное", finalCategoryTotal, finalCategoryCompleted, pbPersonal, tvPersonalPercent);
            updateCategoryProgress("Здоровье", finalCategoryTotal, finalCategoryCompleted, pbHealth, tvHealthPercent);
            updateCategoryProgress("Учеба", finalCategoryTotal, finalCategoryCompleted, pbStudy, tvStudyPercent);
        });
    }

    private String determineCategory(String taskType) {
        // Определяем категорию на основе типа задачи
        if (taskType.contains("book") || taskType.contains("чтение") ||
                taskType.contains("учеба") || taskType.contains("учеб") ||
                taskType.contains("study") || taskType.contains("learning")) {
            return "Учеба";
        } else if (taskType.contains("meeting") || taskType.contains("встреча") ||
                taskType.contains("работа") || taskType.contains("work") ||
                taskType.contains("business") || taskType.contains("office")) {
            return "Работа";
        } else if (taskType.contains("coffee") || taskType.contains("отдых") ||
                taskType.contains("здоровье") || taskType.contains("спорт") ||
                taskType.contains("health") || taskType.contains("sport") ||
                taskType.contains("fitness")) {
            return "Здоровье";
        } else if (taskType.contains("heart") || taskType.contains("личное") ||
                taskType.contains("семья") || taskType.contains("друг") ||
                taskType.contains("personal") || taskType.contains("family") ||
                taskType.contains("friends")) {
            return "Личное";
        }

        return "Работа"; // Категория по умолчанию
    }

    private void updateCategoryProgress(String category,
                                        HashMap<String, Integer> totalMap,
                                        HashMap<String, Integer> completedMap,
                                        ProgressBar progressBar,
                                        TextView percentText) {
        int total = totalMap.getOrDefault(category, 0);
        int completed = completedMap.getOrDefault(category, 0);

        if (total > 0) {
            int percentage = (int) Math.round((completed * 100.0) / total);
            progressBar.setProgress(percentage);
            percentText.setText(percentage + "%");
        } else {
            progressBar.setProgress(0);
            percentText.setText("0%");
        }
    }

    private void resetProgressBars() {
        pbWork.setProgress(0);
        pbPersonal.setProgress(0);
        pbHealth.setProgress(0);
        pbStudy.setProgress(0);

        tvWorkPercent.setText("0%");
        tvPersonalPercent.setText("0%");
        tvHealthPercent.setText("0%");
        tvStudyPercent.setText("0%");
    }

    private void showDefaultStats() {
        // Показываем нулевые значения, если не удалось загрузить данные
        tvTotalTasks.setText("0");
        tvCompletedTasks.setText("0");
        resetProgressBars();

        Toast.makeText(this, "Не удалось загрузить статистику", Toast.LENGTH_SHORT).show();
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
        Log.d(TAG, "StatsActivity resumed");
        // При возвращении в активность обновляем статистику
        if (currentUserId != -1) {
            loadStats();
        }
    }
}