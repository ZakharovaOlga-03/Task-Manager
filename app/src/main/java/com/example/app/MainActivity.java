package com.example.app;

import android.app.ComponentCaller;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.app.ApiService;
import com.example.app.RetrofitClient;
import com.example.app.TasksResponse;
import com.example.app.SharedPrefs;
import com.example.app.data.local.AppDatabase;
import com.example.app.data.local.dao.TaskDao;
import com.example.app.data.local.entities.TaskEntity;
import com.example.app.utils.NetworkUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private RecyclerView rv_tasks;
    private TaskAdapter task_adapter;
    private List<Task> task_list;

    private TextView[] dayViews = new TextView[7];
    private TextView tv_month_year;
    private TextView tv_day_of_week;
    private TextView tv_no_tasks;
    private ImageView btn_calendar_prev;
    private ImageView btn_calendar_next;

    private AuthManager authManager;
    private ApiService apiService;
    private NetworkUtils networkUtils;

    private Calendar currentCalendar;
    private int selectedDayPosition = 0;
    private Calendar[] weekDays = new Calendar[7];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "MainActivity onCreate started");

        // Проверить авторизацию
        if (!SharedPrefs.isLoggedIn(this)) {
            Log.d(TAG, "User not logged in, redirecting to LoginActivity");
            startLoginActivity();
            return;
        }

        boolean isGuest = SharedPrefs.isGuest(this);
        int userId = SharedPrefs.getUserId(this);

        Log.d(TAG, "User logged in with ID: " + userId + ", IsGuest: " + isGuest);

        setup_transparent_navigation();
        setContentView(R.layout.activity_main);

        authManager = new AuthManager(this);
        apiService = RetrofitClient.getApiService();

        networkUtils = new NetworkUtils(this); // Инициализируем NetworkUtils

        currentCalendar = Calendar.getInstance();

        init_views();
        setup_calendar();
        setup_navigation();
        load_user_tasks_for_selected_day();
    }

    private void startLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void init_views() {
        // Календарь
        dayViews[0] = findViewById(R.id.day_mon);
        dayViews[1] = findViewById(R.id.day_tue);
        dayViews[2] = findViewById(R.id.day_wed);
        dayViews[3] = findViewById(R.id.day_thu);
        dayViews[4] = findViewById(R.id.day_fri);
        dayViews[5] = findViewById(R.id.day_sat);
        dayViews[6] = findViewById(R.id.day_sun);

        tv_month_year = findViewById(R.id.tv_month_year);
        tv_day_of_week = findViewById(R.id.tv_day_of_week);
        tv_no_tasks = findViewById(R.id.tv_no_tasks);

        btn_calendar_prev = findViewById(R.id.btn_calendar_prev);
        btn_calendar_next = findViewById(R.id.btn_calendar_next);

        // Список задач
        rv_tasks = findViewById(R.id.rv_tasks);
        rv_tasks.setLayoutManager(new LinearLayoutManager(this));

        Log.d(TAG, "Views initialized");
    }

    private void setup_navigation() {
        // Обработчики для стрелок календаря
        btn_calendar_prev.setOnClickListener(v -> navigateCalendarPrevious());
        btn_calendar_next.setOnClickListener(v -> navigateCalendarNext());

        // Настройка нижней навигации
        NavigationHelper.setup_navigation(this, 0);
    }

    private void setup_calendar() {
        // Установить текущий месяц и год
        updateMonthYearText();

        // Рассчитать дни недели
        calculateWeekDays();

        // Обновить отображение дней
        updateDayViews();

        // Установить выбранный день (сегодня по умолчанию)
        selectDay(findTodayPosition());
    }
    private void updateMonthYearText() {
        // Используем именительный падеж для месяцев
        String monthYear = getMonthYearInNominative(currentCalendar);
        tv_month_year.setText(monthYear);
    }

    private String getMonthYearInNominative(Calendar calendar) {
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        String monthName;
        switch (month) {
            case Calendar.JANUARY:
                monthName = "Январь";
                break;
            case Calendar.FEBRUARY:
                monthName = "Февраль";
                break;
            case Calendar.MARCH:
                monthName = "Март";
                break;
            case Calendar.APRIL:
                monthName = "Апрель";
                break;
            case Calendar.MAY:
                monthName = "Май";
                break;
            case Calendar.JUNE:
                monthName = "Июнь";
                break;
            case Calendar.JULY:
                monthName = "Июль";
                break;
            case Calendar.AUGUST:
                monthName = "Август";
                break;
            case Calendar.SEPTEMBER:
                monthName = "Сентябрь";
                break;
            case Calendar.OCTOBER:
                monthName = "Октябрь";
                break;
            case Calendar.NOVEMBER:
                monthName = "Ноябрь";
                break;
            case Calendar.DECEMBER:
                monthName = "Декабрь";
                break;
            default:
                monthName = "";
        }

        return monthName + ", " + year;
    }



    private void calculateWeekDays() {
        Calendar tempCalendar = (Calendar) currentCalendar.clone();

        // Находим понедельник текущей недели
        int currentDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK);
        int daysToMonday;

        if (currentDayOfWeek == Calendar.SUNDAY) {
            daysToMonday = -6; // Воскресенье -> понедельник предыдущей недели
        } else {
            daysToMonday = Calendar.MONDAY - currentDayOfWeek;
        }

        tempCalendar.add(Calendar.DAY_OF_MONTH, daysToMonday);

        // Заполняем массив дней недели
        for (int i = 0; i < 7; i++) {
            weekDays[i] = (Calendar) tempCalendar.clone();
            tempCalendar.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void updateDayViews() {
        SimpleDateFormat dayNameFormat = new SimpleDateFormat("EE", new Locale("ru"));
        SimpleDateFormat dayNumberFormat = new SimpleDateFormat("d", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            Calendar day = weekDays[i];

            String dayName = dayNameFormat.format(day.getTime());
            String dayNumber = dayNumberFormat.format(day.getTime());

            // Преобразуем первую букву в заглавную
            dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);

            dayViews[i].setText(dayName + "\n" + dayNumber);

            // Проверяем, сегодня ли этот день
            Calendar today = Calendar.getInstance();
            boolean isToday = day.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    day.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    day.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH);

            if (isToday && i != selectedDayPosition) {
                // Сегодняшний день, но не выбранный
                dayViews[i].setBackgroundResource(R.drawable.calendar_day_bg);
                dayViews[i].setTextColor(getResources().getColor(R.color.white_text));
            }
        }
    }


    private int findTodayPosition() {
        Calendar today = Calendar.getInstance();

        for (int i = 0; i < 7; i++) {
            Calendar day = weekDays[i];
            if (day.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    day.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    day.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)) {
                return i;
            }
        }

        return 0; // Если сегодня не в этой неделе, выбираем первый день
    }

    // Обработчик клика по дню (из XML)
    public void onDayClick(View view) {
        int position = Integer.parseInt(view.getTag().toString());
        selectDay(position);
        load_tasks_for_selected_day();
    }

    private void selectDay(int position) {
        // Сбросить выделение с предыдущего дня
        if (selectedDayPosition >= 0 && selectedDayPosition < 7) {
            dayViews[selectedDayPosition].setBackgroundResource(R.drawable.calendar_day_bg);
            dayViews[selectedDayPosition].setTextColor(getResources().getColor(R.color.light_text));

            // Если это был сегодняшний день (но не выбранный сейчас)
            Calendar today = Calendar.getInstance();
            Calendar oldDay = weekDays[selectedDayPosition];
            if (oldDay.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    oldDay.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    oldDay.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)) {
                dayViews[selectedDayPosition].setBackgroundResource(R.drawable.calendar_day_bg);
                dayViews[selectedDayPosition].setTextColor(getResources().getColor(R.color.white_text));
            }
        }

        // Выделить новый день
        selectedDayPosition = position;
        dayViews[position].setBackgroundResource(R.drawable.calendar_selected_bg);
        dayViews[position].setTextColor(getResources().getColor(R.color.white_text));

        // Обновить информацию о дне
        updateSelectedDayInfo();
    }

    private void updateSelectedDayInfo() {
        if (selectedDayPosition >= 0 && selectedDayPosition < 7) {
            Calendar selectedDay = weekDays[selectedDayPosition];
            String dayInfo = getFormattedDayInfo(selectedDay);
            tv_day_of_week.setText(dayInfo);
        }
    }

    private String getFormattedDayInfo(Calendar calendar) {
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);

        String dayName;
        switch (dayOfWeek) {
            case Calendar.MONDAY:
                dayName = "Понедельник";
                break;
            case Calendar.TUESDAY:
                dayName = "Вторник";
                break;
            case Calendar.WEDNESDAY:
                dayName = "Среда";
                break;
            case Calendar.THURSDAY:
                dayName = "Четверг";
                break;
            case Calendar.FRIDAY:
                dayName = "Пятница";
                break;
            case Calendar.SATURDAY:
                dayName = "Суббота";
                break;
            case Calendar.SUNDAY:
                dayName = "Воскресенье";
                break;
            default:
                dayName = "";
        }

        String monthName;
        switch (month) {
            case Calendar.JANUARY:
                monthName = "января";
                break;
            case Calendar.FEBRUARY:
                monthName = "февраля";
                break;
            case Calendar.MARCH:
                monthName = "марта";
                break;
            case Calendar.APRIL:
                monthName = "апреля";
                break;
            case Calendar.MAY:
                monthName = "мая";
                break;
            case Calendar.JUNE:
                monthName = "июня";
                break;
            case Calendar.JULY:
                monthName = "июля";
                break;
            case Calendar.AUGUST:
                monthName = "августа";
                break;
            case Calendar.SEPTEMBER:
                monthName = "сентября";
                break;
            case Calendar.OCTOBER:
                monthName = "октября";
                break;
            case Calendar.NOVEMBER:
                monthName = "ноября";
                break;
            case Calendar.DECEMBER:
                monthName = "декабря";
                break;
            default:
                monthName = "";
        }

        return dayName + ", " + dayOfMonth + " " + monthName;
    }

    private void load_user_tasks_for_selected_day() {
        load_tasks_for_selected_day();
    }



    private List<Task> filterTasksForDate(List<Task> allTasks, String targetDate) {
        List<Task> filteredTasks = new ArrayList<>();

        for (Task task : allTasks) {
            if (task.getTaskGoalDate() != null && !task.getTaskGoalDate().isEmpty()) {
                try {
                    SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    Date taskDate = dbFormat.parse(task.getTaskGoalDate());

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String taskDateStr = dateFormat.format(taskDate);

                    if (taskDateStr.equals(targetDate)) {
                        filteredTasks.add(task);
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "Error parsing task date: " + e.getMessage());
                }
            }
        }

        return filteredTasks;
    }

//    private void setup_tasks_from_api(List<Task> apiTasks) {
//        task_list = new ArrayList<>();
//        Log.d(TAG, "Converting " + apiTasks.size() + " tasks from API to display format");
//
//        for (Task apiTask : apiTasks) {
//            Task displayTask = convertApiTaskToDisplayTask(apiTask);
//            if (displayTask != null) {
//                task_list.add(displayTask);
//            }
//        }
//
//        if (task_list.isEmpty()) {
//            show_no_tasks_message();
//        } else {
//            tv_no_tasks.setVisibility(View.GONE);
//            task_adapter = new TaskAdapter(task_list, this);
//
//            // Добавляем слушатель изменения статуса
//            task_adapter.setOnTaskStatusChangedListener(new TaskAdapter.OnTaskStatusChangedListener() {
//                @Override
//                public void onTaskStatusChanged() {
//                    // При изменении статуса задачи можно обновить статистику
//                    Log.d(TAG, "Task status changed");
//                    // Можно добавить обновление статистики или другие действия
//                }
//            });
//
//            rv_tasks.setAdapter(task_adapter);
//            rv_tasks.setVisibility(View.VISIBLE);
//        }
//    }
//
//
//    private Task convertApiTaskToDisplayTask(Task apiTask) {
//        try {
//            Log.d(TAG, "Converting task: ID=" + apiTask.getId() +
//                    ", Name=" + apiTask.getTaskName() +
//                    ", Status=" + apiTask.getTaskStatus() +
//                    ", IsCompleted=" + apiTask.is_completed());
//
//            Task displayTask = new Task();
//
//            // Копируем ID из API
//            displayTask.setIdTask(apiTask.getIdTask());
//            displayTask.setId(apiTask.getIdTask());
//
//            // 1. Время
//            String timeRange = formatTimeRangeFromDatabase(
//                    apiTask.getTaskGoalDate(),
//                    apiTask.getNotifyStart()
//            );
//            displayTask.set_time(timeRange);
//
//            // 2. Иконка
//            String iconName = getIconNameForTaskType(apiTask.getTaskType());
//            displayTask.set_icon_name(iconName);
//
//            // 3. Название задачи
//            String title = apiTask.getTaskName();
//            if (title == null || title.isEmpty()) {
//                title = "Новая задача";
//            }
//            displayTask.set_title(title);
//
//            // 4. Длительность
//            String duration = calculateDurationFromDatabase(
//                    apiTask.getTaskGoalDate(),
//                    apiTask.getNotifyStart()
//            );
//            displayTask.set_duration(duration);
//
//            // 5. Статус - проверяем, есть ли уже статус в API
//            int status;
//            if (apiTask.get_status() != null && !apiTask.get_status().isEmpty()) {
//                status = apiTask.get_status();
//            } else if (!(apiTask.getTaskStatus()>1)) {
//                status = apiTask.getTaskStatus();
//            } else {
//                status = getTaskStatus(apiTask); // вычисляем по дате
//            }
//            displayTask.set_status(status);
//
//            // 6. Завершена ли задача
//            boolean isCompleted = apiTask.isTaskCompleted();
//            displayTask.set_completed(isCompleted);
//
//            // Копируем другие поля из API
//            displayTask.setTaskName(apiTask.getTaskName());
//            displayTask.setTaskType(apiTask.getTaskType());
//            displayTask.setTaskGoalDate(apiTask.getTaskGoalDate());
//            displayTask.setNotifyStart(apiTask.getNotifyStart());
//            displayTask.setTaskStatus(apiTask.getTaskStatus());
//
//            Log.d(TAG, "Converted task: Title=" + displayTask.get_title() +
//                    ", Status=" + displayTask.get_status() +
//                    ", Completed=" + displayTask.is_completed());
//
//            return displayTask;
//
//        } catch (Exception e) {
//            Log.e(TAG, "Error converting task: " + e.getMessage(), e);
//            return null;
//        }
//    }

    private void show_no_tasks_message() {
        task_list = new ArrayList<>();
        task_adapter = new TaskAdapter(task_list, this);
        rv_tasks.setAdapter(task_adapter);
        rv_tasks.setVisibility(View.GONE);
        tv_no_tasks.setVisibility(View.VISIBLE);
    }

    private String formatTimeRangeFromDatabase(String goalDate, String notifyStart) {
        try {
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

            String startTime = "--:--";
            String endTime = "--:--";

            if (notifyStart != null && !notifyStart.isEmpty()) {
                Date notifyDate = dbFormat.parse(notifyStart);
                startTime = timeFormat.format(notifyDate);
            }

            if (goalDate != null && !goalDate.isEmpty()) {
                Date goalDateObj = dbFormat.parse(goalDate);
                endTime = timeFormat.format(goalDateObj);
            }

            return startTime + " - " + endTime;

        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + e.getMessage(), e);
            return "--:-- - --:--";
        }
    }

    private String calculateDurationFromDatabase(String goalDate, String notifyStart) {
        try {
            if (goalDate != null && notifyStart != null &&
                    !goalDate.isEmpty() && !notifyStart.isEmpty()) {

                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date goal = format.parse(goalDate);
                Date notify = format.parse(notifyStart);

                long diffMillis = goal.getTime() - notify.getTime();
                long diffMinutes = diffMillis / (1000 * 60);

                if (diffMinutes < 60) {
                    return diffMinutes + " минут";
                } else {
                    long hours = diffMinutes / 60;
                    long minutes = diffMinutes % 60;

                    if (minutes == 0) {
                        return hours + " час" + (hours > 1 ? "а" : "");
                    } else {
                        return hours + " час" + (hours > 1 ? "а" : "") + " " + minutes + " минут";
                    }
                }
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error calculating duration: " + e.getMessage(), e);
        }

        return "";
    }

    private String getIconNameForTaskType(String taskType) {
        if (taskType == null) {
            return "book";
        }

        taskType = taskType.toLowerCase();

        if (taskType.contains("book") || taskType.contains("чтение") || taskType.contains("книга")) {
            return "book";
        } else if (taskType.contains("meeting") || taskType.contains("встреча") ||
                taskType.contains("созвон") || taskType.contains("звонок")) {
            return "meeting";
        } else if (taskType.contains("coffee") || taskType.contains("отдых") ||
                taskType.contains("перерыв") || taskType.contains("чай")) {
            return "coffee";
        } else if (taskType.contains("heart") || taskType.contains("личное") ||
                taskType.contains("семья") || taskType.contains("друг")) {
            return "heart";
        } else {
            return "book";
        }
    }

    private String getTaskStatus(Task apiTask) {
        try {
            if (apiTask.getTaskGoalDate() != null && !apiTask.getTaskGoalDate().isEmpty()) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date goalDate = format.parse(apiTask.getTaskGoalDate());
                Date now = new Date();

                if (goalDate.before(now)) {
                    return "просрочено";
                } else {
                    return "в процессе";
                }
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date for status", e);
        }

        return "в процессе";
    }

    public void navigateCalendarPrevious() {
        // Перейти к предыдущей неделе
        currentCalendar.add(Calendar.WEEK_OF_YEAR, -1);
        setup_calendar();
        load_tasks_for_selected_day();
    }

    public void navigateCalendarNext() {
        // Перейти к следующей неделе
        currentCalendar.add(Calendar.WEEK_OF_YEAR, 1);
        setup_calendar();
        load_tasks_for_selected_day();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_logout) {
            Log.d(TAG, "User logging out");
            authManager.logout();
            startLoginActivity();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        Log.d(TAG, "MainActivity resumed");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity destroyed");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK) {
            Log.d(TAG, "Returned from AddTaskActivity, refreshing tasks");
            // Перезагрузить задачи из локальной БД
            load_tasks_for_selected_day();

            // Если есть интернет, запустить фоновую синхронизацию
            if (networkUtils != null && networkUtils.isNetworkAvailable()) {
                startSyncService();
            } else {
                // Если networkUtils null, инициализируем его
                networkUtils = new NetworkUtils(this);
                if (networkUtils.isNetworkAvailable()) {
                    startSyncService();
                }
            }
        }
    }

    private void startSyncService() {
        // Запустить сервис для синхронизации локальных задач с сервером
        Intent intent = new Intent(this, SyncService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data, @NonNull ComponentCaller caller) {
        super.onActivityResult(requestCode, resultCode, data, caller);
    }

//    private List<Task> convertTaskEntitiesToTasks(List<TaskEntity> taskEntities) {
//        List<Task> tasks = new ArrayList<>();
//
//        for (TaskEntity entity : taskEntities) {
//            Task task = new Task();
//
//            // Заполняем поля для отображения
//            task.setIdTask(entity.getLocalId()); // Используем локальный ID
//            task.setUserId(entity.getUserId());
//            task.setTaskName(entity.getTaskName());
//            task.setTaskType(entity.getTaskType());
//            task.setTaskGoalDate(dateToString(entity.getTaskGoalDate()));
//            task.setNotifyStart(dateToString(entity.getNotifyStart()));
//            task.setTaskNote(entity.getTaskNote());
//            task.setTaskReward(entity.getTaskReward());
//            task.setTaskStatus(entity.getStatus());
//
//            // Для отображения в UI
//            task.set_time(formatTimeRange(entity.getNotifyStart(), entity.getTaskGoalDate()));
//            task.set_title(entity.getTaskName());
//            task.set_status(entity.getStatus());
//            task.set_completed("completed".equals(entity.getStatus()));
//            task.set_icon_name(getIconNameForTaskType(entity.getTaskType()));
//            task.set_duration(calculateDuration(entity.getNotifyStart(), entity.getTaskGoalDate()));
//
//            tasks.add(task);
//        }
//
//        return tasks;
//    }

    private void load_tasks_for_selected_day() {
        int userId = SharedPrefs.getUserId(this);
        if (userId == -1) {
            show_no_tasks_message();
            return;
        }

        // Получаем дату выбранного дня
        Calendar selectedDate = weekDays[selectedDayPosition];
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String selectedDateStr = dateFormat.format(selectedDate.getTime());

        Log.d(TAG, "Loading tasks for date: " + selectedDateStr + " from LOCAL DB");
        tv_no_tasks.setVisibility(View.GONE);

        try {
            // Парсим дату
            Date targetDate = dateFormat.parse(selectedDateStr);

            // Получаем LiveData из Room (на главном потоке)
            AppDatabase database = AppDatabase.getInstance(MainActivity.this);
            LiveData<List<TaskEntity>> liveData = database.taskDao().getTasksForDate(userId, targetDate);

            // Подписываемся на изменения LiveData
            liveData.observe(this, new Observer<List<TaskEntity>>() {
                @Override
                public void onChanged(List<TaskEntity> taskEntities) {
                    if (taskEntities != null && !taskEntities.isEmpty()) {
                        // Конвертируем TaskEntity в Task
                        List<Task> displayTasks = new ArrayList<>();
                        for (TaskEntity entity : taskEntities) {
                            Task task = convertTaskEntityToDisplayTask(entity);
                            if (task != null) {
                                displayTasks.add(task);
                            }
                        }

                        if (displayTasks.isEmpty()) {
                            show_no_tasks_message();
                        } else {
                            setup_tasks_list(displayTasks);
                        }
                    } else {
                        show_no_tasks_message();
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error loading tasks from local DB: " + e.getMessage());
            show_no_tasks_message();
        }
    }

    private Task convertTaskEntityToDisplayTask(TaskEntity entity) {
        try {
            Task task = new Task();

            // Основные поля из TaskEntity
            task.setIdTask(entity.getLocalId()); // Используем локальный ID
            task.setUserId(entity.getUserId());
            task.setTaskName(entity.getTaskName());
            task.setTaskType(entity.getTaskType());
            task.setTaskStatus(entity.getStatus());

            // Конвертируем Date в String для API формата
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            if (entity.getTaskGoalDate() != null) {
                task.setTaskGoalDate(dbFormat.format(entity.getTaskGoalDate()));
            }
            if (entity.getNotifyStart() != null) {
                task.setNotifyStart(dbFormat.format(entity.getNotifyStart()));
            }

            task.setTaskNote(entity.getTaskNote());
            task.setTaskReward(entity.getTaskReward());

            // Поля для отображения в UI
            task.set_time(formatTimeRangeForDisplay(entity.getNotifyStart(), entity.getTaskGoalDate()));
            task.set_title(entity.getTaskName() != null ? entity.getTaskName() : "Новая задача");
            task.set_status(entity.getStatus()==1 ? "выполнено" : "в процессе");
            task.set_completed(entity.getStatus()==1);
            task.set_icon_name(getIconNameForTaskType(entity.getTaskType()));
            task.set_duration(calculateDurationForDisplay(entity.getNotifyStart(), entity.getTaskGoalDate()));

            return task;

        } catch (Exception e) {
            Log.e(TAG, "Error converting TaskEntity: " + e.getMessage(), e);
            return null;
        }
    }

    private String formatTimeRangeForDisplay(Date startDate, Date endDate) {
        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String startTime = startDate != null ? timeFormat.format(startDate) : "--:--";
            String endTime = endDate != null ? timeFormat.format(endDate) : "--:--";
            return startTime + " - " + endTime;
        } catch (Exception e) {
            return "--:-- - --:--";
        }
    }

    private String calculateDurationForDisplay(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return "";
        }

        try {
            long diffMillis = endDate.getTime() - startDate.getTime();
            long diffMinutes = diffMillis / (1000 * 60);

            if (diffMinutes < 60) {
                return diffMinutes + " минут";
            } else {
                long hours = diffMinutes / 60;
                long minutes = diffMinutes % 60;

                if (minutes == 0) {
                    return hours + " час" + (hours > 1 ? "а" : "");
                } else {
                    return hours + " час" + (hours > 1 ? "а" : "") + " " + minutes + " минут";
                }
            }
        } catch (Exception e) {
            return "";
        }
    }

    private void setup_tasks_list(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            show_no_tasks_message();
            return;
        }

        task_list = tasks;
        Log.d(TAG, "Setting up " + task_list.size() + " tasks in UI");

        if (task_adapter == null) {
            task_adapter = new TaskAdapter(task_list, this);
            rv_tasks.setAdapter(task_adapter);
        } else {
            task_adapter.updateTasks(task_list);
        }

        rv_tasks.setVisibility(View.VISIBLE);
        tv_no_tasks.setVisibility(View.GONE);
    }

}