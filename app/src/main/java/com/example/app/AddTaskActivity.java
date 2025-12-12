package com.example.app;

import android.util.Log;
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

import com.example.app.data.local.AppDatabase;
import com.example.app.data.local.dao.TaskDao;
import com.example.app.data.local.entities.TaskEntity;
import com.example.app.data.repository.TaskRepository;
import com.example.app.utils.NetworkUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddTaskActivity extends AppCompatActivity {

    // UI элементы
    private EditText et_task_name, et_date, et_start_time, et_end_time, et_task_note;
    private Spinner spinner_category, spinner_reminder, spinner_status, spinner_priority;
    private Button btn_create_task;

    private Calendar calendar;

    // Для работы с данными
    private TaskRepository taskRepository;
    private NetworkUtils networkUtils;
    private ApiService apiService;
    private int currentUserId;

    // Статические списки для выбора
    private static final String[] CATEGORIES = {"Здоровье", "Работа", "Образование", "Развлечения", "Личное"};
    private static final String[] REMINDERS = {"Не напоминать", "За 30 минут", "За 1 час", "За 3 часа", "За 1 день", "Каждый час"};
    private static final String[] STATUSES = {"pending", "in_progress", "completed", "cancelled"};
    private static final String[] PRIORITIES = {"1 (Низкий)", "2", "3 (Средний)", "4", "5 (Высокий)"};

    // Маппинги для преобразования
    private static final Map<String, String> CATEGORY_TO_TYPE = new HashMap<String, String>() {{
        put("Здоровье", "heart");
        put("Работа", "meeting");
        put("Образование", "book");
        put("Развлечения", "coffee");
        put("Личное", "heart");
    }};

    private static final Map<String, String> REMINDER_TO_FREQUENCY = new HashMap<String, String>() {{
        put("Не напоминать", null);
        put("За 30 минут", "30min");
        put("За 1 час", "1hour");
        put("За 3 часа", "3hours");
        put("За 1 день", "1day");
        put("Каждый час", "1hour_repeat");
    }};

    private static final Map<String, String> PRIORITY_TO_IMPORTANCE = new HashMap<String, String>() {{
        put("1 (Низкий)", "1");
        put("2", "2");
        put("3 (Средний)", "3");
        put("4", "4");
        put("5 (Высокий)", "5");
    }};

    private static final Map<String, Integer> CATEGORY_BONUS = new HashMap<String, Integer>() {{
        put("Здоровье", 15);
        put("Работа", 20);
        put("Образование", 18);
        put("Развлечения", 10);
        put("Личное", 12);
    }};

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

        // Инициализация компонентов
        calendar = Calendar.getInstance();

        // Инициализация локальной БД и репозитория
        AppDatabase database = AppDatabase.getInstance(this);
        taskRepository = new TaskRepository(getApplication());
        networkUtils = new NetworkUtils(this);

        // Инициализация Retrofit (для онлайн-режима)
        apiService = RetrofitClient.getApiService();

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
        et_task_note = findViewById(R.id.et_task_note); // Новое поле для заметок

        spinner_category = findViewById(R.id.spinner_category);
        spinner_reminder = findViewById(R.id.spinner_reminder);
        spinner_priority = findViewById(R.id.spinner_priority); // Новый спиннер для приоритета

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
        // Категории задач
        ArrayAdapter<String> category_adapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, CATEGORIES);
        category_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner_category.setAdapter(category_adapter);

        // Напоминания
        ArrayAdapter<String> reminder_adapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, REMINDERS);
        reminder_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner_reminder.setAdapter(reminder_adapter);

//        // Статусы задач (новый спиннер)
//        ArrayAdapter<String> status_adapter = new ArrayAdapter<>(this,
//                R.layout.spinner_item, STATUSES);
//        status_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
//        spinner_status.setAdapter(status_adapter);

        // Приоритеты (новый спиннер)
        ArrayAdapter<String> priority_adapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, PRIORITIES);
        priority_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner_priority.setAdapter(priority_adapter);

        // Установить значения по умолчанию
//        spinner_status.setSelection(0); // "pending"
        spinner_priority.setSelection(2); // "3 (Средний)"
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

        String priority = spinner_priority.getSelectedItem().toString();
        String taskNote = et_task_note.getText().toString().trim();

        // Валидация
        if (taskName.isEmpty()) {
            Toast.makeText(this, "Введите название задачи", Toast.LENGTH_SHORT).show();
            return;
        }

        if (date.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
            Toast.makeText(this, "Заполните дату и время", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Преобразование времени для БД
            Date taskGoalDate = parseDateTime(date + " " + endTime + ":00");
            Date notifyStart = parseDateTime(date + " " + startTime + ":00");

            // Проверка, что время окончания позже времени начала
            if (taskGoalDate.before(notifyStart)) {
                Toast.makeText(this, "Время окончания должно быть позже времени начала",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Преобразование выбранных значений
            String taskType = mapCategoryToTaskType(category);
            String taskImportance = mapPriorityToImportance(priority);
            String notifyFrequency = mapReminderToFrequency(reminder);
            String notifyType = "notification"; // По умолчанию уведомление
            int taskReward = calculateReward(category, taskImportance);

            // Создание объекта TaskEntity для локальной БД
            TaskEntity newTask = createTaskEntity(
                    taskName, taskType, taskImportance, taskGoalDate,
                    notifyStart, notifyFrequency, notifyType, taskNote,
                    taskReward, "pending"
            );

            // Показать прогресс
            btn_create_task.setEnabled(false);
            btn_create_task.setText("Создание...");

            // Сохранить задачу в локальную БД (оффлайн-первый подход)
            saveTaskToLocalDatabase(newTask);

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка при создании задачи: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private Date parseDateTime(String dateTimeString) throws java.text.ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return format.parse(dateTimeString);
    }

    private TaskEntity createTaskEntity(
            String taskName, String taskType, String taskImportance,
            Date taskGoalDate, Date notifyStart, String notifyFrequency,
            String notifyType, String taskNote, int taskReward, String status) {

        TaskEntity task = new TaskEntity();

        // Основные поля
        task.setUserId(currentUserId);
        task.setTaskName(taskName);
        task.setTaskType(taskType);
        task.setTaskImportance(taskImportance);
        task.setTaskGoalDate(taskGoalDate);
        task.setNotifyStart(notifyStart);
        task.setNotifyFrequency(notifyFrequency);
        task.setNotifyType(notifyType);
        task.setTaskNote(taskNote);
        task.setTaskReward(taskReward);
        task.setStatus(status);

        // Дополнительные поля из TaskEntity
        task.setTaskCreationDate(new Date());
        task.setUpdatedAt(new Date());

        if (currentUserId < 0) { // Гостевая задача
            task.setSyncStatus("offline"); // Явно помечаем как оффлайн
        } else{
            if (networkUtils.isNetworkAvailable()) {
                task.setSyncStatus("pending"); // Будет отправлено на сервер
            } else {
                task.setSyncStatus("offline"); // Только локальное сохранение
            }
        }

        // Установить completedAt если задача создается выполненной
        if ("completed".equals(status)) {
            task.setCompletedAt(new Date());
        }

        return task;
    }

    private void saveTaskToLocalDatabase(TaskEntity task) {
        new Thread(() -> {
            try {
                // Получаем доступ к БД напрямую
                AppDatabase database = AppDatabase.getInstance(AddTaskActivity.this);
                TaskDao taskDao = database.taskDao();

                // Сохраняем задачу в Room
                long localId = taskDao.insertTask(task);
                task.setLocalId((int) localId);

                runOnUiThread(() -> {
                    btn_create_task.setEnabled(true);
                    btn_create_task.setText("Создать задачу");

                    if (networkUtils.isNetworkAvailable()) {
                        // Только если это НЕ гостевая задача
                        if (currentUserId > 0) { // Положительный ID = зарегистрированный пользователь
                            sendTaskToServer(task);
                            Toast.makeText(AddTaskActivity.this,
                                    "Задача создана и отправлена на сервер",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(AddTaskActivity.this,
                                    "Задача сохранена локально (гостевой режим)",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(AddTaskActivity.this,
                                "Задача сохранена локально. Синхронизируется при подключении",
                                Toast.LENGTH_LONG).show();
                    }

                    setResult(RESULT_OK);
                    finish();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btn_create_task.setEnabled(true);
                    btn_create_task.setText("Создать задачу");

                    Toast.makeText(AddTaskActivity.this,
                            "Ошибка сохранения задачи: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    Log.e("AddTaskActivity", "Error saving task: " + e.getMessage(), e);
                });
            }
        }).start();
    }

    private void sendTaskToServer(TaskEntity task) {
        // Преобразовать TaskEntity в Task (для API)
        Task apiTask = convertToApiTask(task);

        // Отправка задачи на сервер через API
        retrofit2.Call<ApiResponse> call = apiService.createTask(apiTask);
        call.enqueue(new retrofit2.Callback<ApiResponse>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse> call,
                                   retrofit2.Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        // Обновить локальную запись с server_id
                        task.setServerId(apiResponse.getTaskId());
                        task.setSyncStatus("synced");
                        task.setLastSyncDate(new Date());

                        // Обновить в БД в фоне
                        updateTaskInDatabase(task);
                    } else {
                        // Сервер вернул ошибку
                        task.setSyncStatus("failed");
                        updateTaskInDatabase(task);
                        Log.e("AddTaskActivity", "Server error: " + apiResponse.getMessage());
                    }
                } else {
                    // Ошибка HTTP
                    task.setSyncStatus("failed");
                    updateTaskInDatabase(task);
                    Log.e("AddTaskActivity", "HTTP error: " + response.code());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse> call, Throwable t) {
                // Ошибка сети
                task.setSyncStatus("failed");
                updateTaskInDatabase(task);
                Log.e("AddTaskActivity", "Network error: " + t.getMessage());
            }
        });
    }

    private void updateTaskInDatabase(TaskEntity task) {
        new Thread(() -> {
            try {
                AppDatabase.getInstance(AddTaskActivity.this)
                        .taskDao()
                        .updateTask(task);
            } catch (Exception e) {
                Log.e("AddTaskActivity", "Error updating task in DB: " + e.getMessage());
            }
        }).start();
    }

    private Task convertToApiTask(TaskEntity entity) {
        Task task = new Task();

        // Основные поля
        task.setUserId(entity.getUserId());
        task.setTaskName(entity.getTaskName());
        task.setTaskType(entity.getTaskType());
        task.setTaskImportance(entity.getTaskImportance());

        // Преобразование дат в строки
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        if (entity.getTaskGoalDate() != null) {
            task.setTaskGoalDate(format.format(entity.getTaskGoalDate()));
        }
        if (entity.getNotifyStart() != null) {
            task.setNotifyStart(format.format(entity.getNotifyStart()));
        }

        task.setNotifyFrequency(entity.getNotifyFrequency());
        task.setNotifyType(entity.getNotifyType());
        task.setTaskNote(entity.getTaskNote());
        task.setTaskReward(entity.getTaskReward());

        // Если есть server_id, установить его
        if (entity.getServerId() > 0) {
            task.setIdTask(entity.getServerId());
        }

        return task;
    }

    // Преобразование категории в тип задачи для БД
    private String mapCategoryToTaskType(String category) {
        return CATEGORY_TO_TYPE.getOrDefault(category, "book");
    }

    // Преобразование приоритета в важность задачи
    private String mapPriorityToImportance(String priority) {
        return PRIORITY_TO_IMPORTANCE.getOrDefault(priority, "3");
    }

    // Преобразование напоминания в частоту
    private String mapReminderToFrequency(String reminder) {
        return REMINDER_TO_FREQUENCY.get(reminder);
    }

    // Расчет награды за задачу
    private int calculateReward(String category, String importance) {
        int baseReward = 10;
        int bonus = CATEGORY_BONUS.getOrDefault(category, 0);
        int importanceMultiplier = Integer.parseInt(importance);

        return baseReward + (bonus * importanceMultiplier);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Можно очистить ресурсы если нужно
    }
}