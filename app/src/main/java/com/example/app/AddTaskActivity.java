package com.example.app;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.app.ApiResponse;
import com.example.app.ApiService;
import com.example.app.RetrofitClient;
import com.example.app.Task;
import com.example.app.SharedPrefs;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddTaskActivity extends AppCompatActivity {

    private EditText et_task_name, et_date, et_start_time, et_end_time;
    private Spinner spinner_category, spinner_reminder;
    private Button btn_create_task;
    private Calendar calendar;

    private ApiService apiService;
    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setup_transparent_navigation();

        setContentView(R.layout.activity_add_task);

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

        // Инициализация Retrofit
        apiService = RetrofitClient.getApiService();

        calendar = Calendar.getInstance();

        init_views();
        setup_spinners();
        setup_date_time_pickers();
        NavigationHelper.setup_navigation(this, 2);

        // Установить текущую дату и время по умолчанию
        set_default_datetime();
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

    private void init_views() {
        et_task_name = findViewById(R.id.et_task_name);
        et_date = findViewById(R.id.et_date);
        et_start_time = findViewById(R.id.et_start_time);
        et_end_time = findViewById(R.id.et_end_time);
        spinner_category = findViewById(R.id.spinner_category);
        spinner_reminder = findViewById(R.id.spinner_reminder);
        btn_create_task = findViewById(R.id.btn_create_task);

        btn_create_task.setOnClickListener(v -> create_task());

        View bottom_nav = findViewById(R.id.bottom_nav_container);
        ViewCompat.setOnApplyWindowInsetsListener(bottom_nav, (v, insets) -> {
            Insets nav_bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(0, 0, 0, nav_bars.bottom);
            return insets;
        });
    }

    private void set_default_datetime() {
        // Установить текущую дату
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        et_date.setText(dateFormat.format(calendar.getTime()));

        // Установить текущее время +1 час для начала
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        et_start_time.setText(timeFormat.format(calendar.getTime()));

        // Установить время окончания (через 1 час от начала)
        calendar.add(Calendar.HOUR, 1);
        et_end_time.setText(timeFormat.format(calendar.getTime()));
        calendar.add(Calendar.HOUR, -1); // Вернуть обратно
    }

    private void setup_spinners() {
        String[] categories = {"Здоровье", "Работа", "Образование", "Развлечения"};
        ArrayAdapter<String> category_adapter = new ArrayAdapter<>(this, R.layout.spinner_item, categories);
        category_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner_category.setAdapter(category_adapter);

        String[] reminders = {"Каждый час", "За 30 минут", "За 1 час", "Не напоминать"};
        ArrayAdapter<String> reminder_adapter = new ArrayAdapter<>(this, R.layout.spinner_item, reminders);
        reminder_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner_reminder.setAdapter(reminder_adapter);
    }

    private void setup_date_time_pickers() {
        et_date.setOnClickListener(v -> show_date_picker());
        et_start_time.setOnClickListener(v -> show_time_picker(et_start_time));
        et_end_time.setOnClickListener(v -> show_time_picker(et_end_time));
    }

    private void show_date_picker() {
        DatePickerDialog date_picker = new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, day);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    et_date.setText(sdf.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        date_picker.show();
    }

    private void show_time_picker(EditText time_field) {
        TimePickerDialog time_picker = new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    String time = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                    time_field.setText(time);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        );
        time_picker.show();
    }

    private void create_task() {
        // Получение данных из полей
        String taskName = et_task_name.getText().toString().trim();
        String date = et_date.getText().toString().trim();
        String startTime = et_start_time.getText().toString().trim();
        String endTime = et_end_time.getText().toString().trim();
        String category = spinner_category.getSelectedItem().toString();
        String reminder = spinner_reminder.getSelectedItem().toString();

        // Валидация
        if (taskName.isEmpty()) {
            Toast.makeText(this, "Введите название задачи", Toast.LENGTH_SHORT).show();
            return;
        }

        if (date.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
            Toast.makeText(this, "Заполните дату и время", Toast.LENGTH_SHORT).show();
            return;
        }

        // Преобразование времени для БД
        String taskGoalDate = date + " " + endTime + ":00";
        String notifyStart = date + " " + startTime + ":00";

        // Определение типа задачи на основе категории
        String taskType = mapCategoryToTaskType(category);

        // Определение важности на основе напоминания
        String taskImportance = mapReminderToImportance(reminder);

        // Определение награды
        int taskReward = calculateReward(category, taskImportance);

        // Определение частоты напоминания
        String notifyFrequency = mapReminderToFrequency(reminder);
        String notifyType = "notification"; // По умолчанию уведомление

        // Создание объекта задачи для API
        Task newTask = new Task();
        newTask.setUserId(currentUserId);
        newTask.setTaskName(taskName);
        newTask.setTaskType(taskType);
        newTask.setTaskImportance(taskImportance);
        newTask.setTaskGoalDate(taskGoalDate);
        newTask.setNotifyStart(notifyStart);
        newTask.setNotifyFrequency(notifyFrequency);
        newTask.setNotifyType(notifyType);
        newTask.setTaskNote(category); // Используем категорию как заметку
        newTask.setTaskReward(taskReward);

        // Показать прогресс
        btn_create_task.setEnabled(false);
        btn_create_task.setText("Создание...");

        // Отправка задачи на сервер через API
        Call<ApiResponse> call = apiService.createTask(newTask);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                runOnUiThread(() -> {
                    btn_create_task.setEnabled(true);
                    btn_create_task.setText("Создать задачу");

                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse apiResponse = response.body();

                        if (apiResponse.isSuccess()) {
                            Toast.makeText(AddTaskActivity.this,
                                    apiResponse.getMessage(), Toast.LENGTH_SHORT).show();

                            // Отправить результат обратно в MainActivity
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Toast.makeText(AddTaskActivity.this,
                                    "Ошибка: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ?
                                    response.errorBody().string() : "Неизвестная ошибка";
                            Toast.makeText(AddTaskActivity.this,
                                    "Ошибка сервера: " + response.code() + " - " + errorBody,
                                    Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(AddTaskActivity.this,
                                    "Ошибка сервера: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                runOnUiThread(() -> {
                    btn_create_task.setEnabled(true);
                    btn_create_task.setText("Создать задачу");

                    Toast.makeText(AddTaskActivity.this,
                            "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    t.printStackTrace(); // Для отладки
                });
            }
        });
    }

    // Преобразование категории в тип задачи для БД
    private String mapCategoryToTaskType(String category) {
        Map<String, String> categoryMap = new HashMap<>();
        categoryMap.put("Здоровье", "heart");
        categoryMap.put("Работа", "meeting");
        categoryMap.put("Образование", "book");
        categoryMap.put("Развлечения", "coffee");

        return categoryMap.getOrDefault(category, "book");
    }

    // Преобразование напоминания в важность задачи
    private String mapReminderToImportance(String reminder) {
        Map<String, String> importanceMap = new HashMap<>();
        importanceMap.put("Каждый час", "5");     // Высокая важность
        importanceMap.put("За 30 минут", "4");    // Средне-высокая
        importanceMap.put("За 1 час", "3");       // Средняя
        importanceMap.put("Не напоминать", "2");  // Низкая

        return importanceMap.getOrDefault(reminder, "3");
    }

    // Преобразование напоминания в частоту
    private String mapReminderToFrequency(String reminder) {
        Map<String, String> frequencyMap = new HashMap<>();
        frequencyMap.put("Каждый час", "1hour");
        frequencyMap.put("За 30 минут", "30min");
        frequencyMap.put("За 1 час", "1hour");
        frequencyMap.put("Не напоминать", null); // NULL для отключения

        return frequencyMap.get(reminder);
    }

    // Расчет награды за задачу
    private int calculateReward(String category, String importance) {
        int baseReward = 10;

        // Бонус за категорию
        Map<String, Integer> categoryBonus = new HashMap<>();
        categoryBonus.put("Здоровье", 5);
        categoryBonus.put("Работа", 8);
        categoryBonus.put("Образование", 7);
        categoryBonus.put("Развлечения", 3);

        int bonus = categoryBonus.getOrDefault(category, 0);

        // Множитель важности
        int importanceMultiplier = Integer.parseInt(importance);

        return baseReward + (bonus * importanceMultiplier);
    }
}