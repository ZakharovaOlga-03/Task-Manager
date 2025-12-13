package com.example.app;

import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
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

public class CalendarActivity extends AppCompatActivity {

    private static final String TAG = "CalendarActivity";

    private RecyclerView rv_calendar, rv_day_tasks;
    private CalendarGridAdapter calendar_adapter;
    private CalendarTaskAdapter task_adapter;
    private List<CalendarDay> calendar_days;
    private List<Task> day_tasks;
    private TextView tv_selected_date;
    private BottomSheetBehavior<LinearLayout> bottom_sheet_behavior;
    private TextView tv_month_year;

    private ApiService apiService;
    private int currentUserId;
    private Calendar currentCalendar;
    private int selectedDay;
    private SimpleDateFormat monthYearFormat;
    private SimpleDateFormat dayNameFormat;

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
        setContentView(R.layout.activity_calendar);

        // Инициализация Retrofit
        apiService = RetrofitClient.getApiService();
        currentCalendar = Calendar.getInstance();
        selectedDay = currentCalendar.get(Calendar.DAY_OF_MONTH);

        // Инициализация форматов дат
        monthYearFormat = new SimpleDateFormat("MMMM yyyy", new Locale("ru"));
        dayNameFormat = new SimpleDateFormat("EEEE, d MMMM", new Locale("ru"));

        init_views();
        setup_bottom_sheet();
        setup_calendar();
        load_tasks_for_selected_day();
        NavigationHelper.setup_navigation(this, 1);
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
        rv_calendar = findViewById(R.id.rv_calendar);
        rv_day_tasks = findViewById(R.id.rv_day_tasks);
        tv_selected_date = findViewById(R.id.tv_selected_date);
        tv_month_year = findViewById(R.id.tv_month_year);

        GridLayoutManager grid_layout = new GridLayoutManager(this, 7);
        rv_calendar.setLayoutManager(grid_layout);
        rv_day_tasks.setLayoutManager(new LinearLayoutManager(this));

        View bottom_nav = findViewById(R.id.bottom_nav_container);
        ViewCompat.setOnApplyWindowInsetsListener(bottom_nav, (v, insets) -> {
            Insets nav_bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(0, 0, 0, nav_bars.bottom);
            return insets;
        });

        // Настройка кликов на кнопки меню и поиска
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

    private void setup_bottom_sheet() {
        LinearLayout tasks_panel = findViewById(R.id.tasks_panel);
        bottom_sheet_behavior = BottomSheetBehavior.from(tasks_panel);

        bottom_sheet_behavior.setGestureInsetBottomIgnored(true);
        bottom_sheet_behavior.setFitToContents(false);
        bottom_sheet_behavior.setSkipCollapsed(false);

        tasks_panel.post(() -> {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int screen_height = displayMetrics.heightPixels;

            int peek_height = dpToPx(200);
            int expanded_offset = dpToPx(100);

            bottom_sheet_behavior.setPeekHeight(peek_height);
            bottom_sheet_behavior.setExpandedOffset(expanded_offset);
            bottom_sheet_behavior.setHideable(false);
            bottom_sheet_behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        });

        bottom_sheet_behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(View bottomSheet, int newState) {
                // Можно добавить логику при изменении состояния
            }

            @Override
            public void onSlide(View bottomSheet, float slideOffset) {
                // Можно добавить логику при скольжении
            }
        });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    private void setup_calendar() {
        calendar_days = new ArrayList<>();

        // Обновляем заголовок месяца
        update_month_header();

        int currentMonth = currentCalendar.get(Calendar.MONTH);
        int currentYear = currentCalendar.get(Calendar.YEAR);

        // Получаем количество дней в текущем месяце
        Calendar tempCalendar = Calendar.getInstance();
        tempCalendar.set(currentYear, currentMonth, 1);
        int daysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Получаем день недели для 1-го числа месяца
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK);

        // Добавляем пустые дни для выравнивания (Воскресенье = 1)
        int emptyDays = firstDayOfWeek - 1;
        for (int i = 0; i < emptyDays; i++) {
            calendar_days.add(new CalendarDay("", 0, false, false));
        }

        // Добавляем дни месяца
        Calendar today = Calendar.getInstance();
        for (int i = 1; i <= daysInMonth; i++) {
            boolean isToday = (currentYear == today.get(Calendar.YEAR) &&
                    currentMonth == today.get(Calendar.MONTH) &&
                    i == today.get(Calendar.DAY_OF_MONTH));
            boolean isSelected = (i == selectedDay);
            calendar_days.add(new CalendarDay("", i, isSelected, isToday));
        }

        calendar_adapter = new CalendarGridAdapter(calendar_days, this, position -> {
            // Проверяем, что это не пустой день
            CalendarDay clickedDay = calendar_days.get(position);
            if (clickedDay.get_day_number() > 0) {
                for (int i = 0; i < calendar_days.size(); i++) {
                    CalendarDay day = calendar_days.get(i);
                    day.set_selected(i == position);
                }
                calendar_adapter.notifyDataSetChanged();

                selectedDay = clickedDay.get_day_number();
                update_selected_date();
                load_tasks_for_selected_day();
            }
        });

        rv_calendar.setAdapter(calendar_adapter);
        update_selected_date();
    }

    private void update_month_header() {
        // Обновляем заголовок месяца и года
        String monthYear = monthYearFormat.format(currentCalendar.getTime());
        monthYear = monthYear.substring(0, 1).toUpperCase() + monthYear.substring(1);

        // Обновляем TextView если он есть в layout
        if (tv_month_year != null) {
            tv_month_year.setText(monthYear);
        }
    }

    private void load_tasks_for_selected_day() {
        Log.d(TAG, "Loading tasks for day: " + selectedDay + ", User: " + currentUserId);

        // Загружаем все задачи пользователя
        Call<TasksResponse> call = apiService.getTasks(currentUserId);
        call.enqueue(new Callback<TasksResponse>() {
            @Override
            public void onResponse(Call<TasksResponse> call, Response<TasksResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TasksResponse tasksResponse = response.body();

                    if (tasksResponse.isSuccess() && tasksResponse.getTasks() != null) {
                        List<Task> allTasks = tasksResponse.getTasks();
                        Log.d(TAG, "Total tasks loaded: " + allTasks.size());

                        // Фильтруем задачи по выбранному дню
                        List<Task> filteredTasks = filterTasksByDay(allTasks, selectedDay);
                        Log.d(TAG, "Tasks for selected day: " + filteredTasks.size());

                        // Конвертируем задачи для отображения
                        List<Task> displayTasks = convertTasksForDisplay(filteredTasks);

                        // Обновляем UI
                        update_tasks_list(displayTasks);
                    } else {
                        Log.e(TAG, "API returned error: " + tasksResponse.getMessage());
                        show_no_tasks_message();
                    }
                } else {
                    Log.e(TAG, "Failed to load tasks. Code: " + response.code());
                    show_no_tasks_message();
                }
            }

            @Override
            public void onFailure(Call<TasksResponse> call, Throwable t) {
                Log.e(TAG, "Network error loading tasks: " + t.getMessage());
                show_no_tasks_message();
            }
        });
    }

    private List<Task> filterTasksByDay(List<Task> allTasks, int day) {
        List<Task> filteredTasks = new ArrayList<>();

        try {
            // Создаем дату для выбранного дня
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(Calendar.YEAR, currentCalendar.get(Calendar.YEAR));
            selectedDate.set(Calendar.MONTH, currentCalendar.get(Calendar.MONTH));
            selectedDate.set(Calendar.DAY_OF_MONTH, day);

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String targetDateStr = dateFormat.format(selectedDate.getTime());
            Log.d(TAG, "Filtering tasks for date: " + targetDateStr);

            for (Task task : allTasks) {
                if (task.getTaskGoalDate() != null && !task.getTaskGoalDate().isEmpty()) {
                    try {
                        // Извлекаем дату из поля task_goal_date
                        String taskDateStr = task.getTaskGoalDate().substring(0, 10);

                        if (taskDateStr.equals(targetDateStr)) {
                            filteredTasks.add(task);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error extracting date from task: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error filtering tasks: " + e.getMessage(), e);
        }

        return filteredTasks;
    }

    private List<Task> convertTasksForDisplay(List<Task> apiTasks) {
        List<Task> displayTasks = new ArrayList<>();

        for (Task apiTask : apiTasks) {
            Task displayTask = convertApiTaskToDisplayTask(apiTask);
            if (displayTask != null) {
                displayTasks.add(displayTask);
            }
        }

        return displayTasks;
    }

    private Task convertApiTaskToDisplayTask(Task apiTask) {
        try {
            Log.d(TAG, "Converting task for calendar: ID=" + apiTask.getIdTask() +
                    ", Name=" + apiTask.getTaskName() +
                    ", Status=" + apiTask.getTaskStatus() +
                    ", IsCompleted=" + apiTask.is_completed());

            Task displayTask = new Task();

            // Копируем ID из API
            displayTask.setIdTask(apiTask.getIdTask());
            displayTask.setId(apiTask.getIdTask());

            // 1. Время (формат: "10:00 - 17:00")
            String timeRange = formatTimeRangeFromDatabase(
                    apiTask.getTaskGoalDate(),
                    apiTask.getNotifyStart()
            );
            displayTask.set_time(timeRange);

            // 2. Иконка на основе типа задачи (будет использоваться в адаптере)
            String iconName = getIconNameForTaskType(apiTask.getTaskType());
            displayTask.set_icon_name(iconName);

            // 3. Название задачи
            String title = apiTask.getTaskName();
            if (title == null || title.isEmpty()) {
                title = "Новая задача";
            }
            displayTask.set_title(title);

            // 4. Длительность
            String duration = calculateDurationFromDatabase(
                    apiTask.getTaskGoalDate(),
                    apiTask.getNotifyStart()
            );
            displayTask.set_duration(duration);

            // 5. Статус задачи - проверяем, выполнена ли задача
            String status;
            boolean isTaskCompleted = isTaskCompleted(apiTask);

            if (isTaskCompleted) {
                status = "выполнено";
            } else {
                status = getTaskStatusForCalendar(apiTask);
            }
            displayTask.set_status(status);

            // 6. Завершена ли задача
            displayTask.set_completed(isTaskCompleted);

            // Копируем другие поля из API
            displayTask.setTaskName(apiTask.getTaskName());
            displayTask.setTaskType(apiTask.getTaskType());
            displayTask.setTaskGoalDate(apiTask.getTaskGoalDate());
            displayTask.setNotifyStart(apiTask.getNotifyStart());
            displayTask.setTaskStatus(apiTask.getTaskStatus());
            displayTask.setTaskCompletionDate(apiTask.getTaskCompletionDate());

            Log.d(TAG, "Converted for calendar: Title=" + displayTask.get_title() +
                    ", Status=" + displayTask.get_status() +
                    ", Completed=" + displayTask.is_completed());

            return displayTask;

        } catch (Exception e) {
            Log.e(TAG, "Error converting task for calendar: " + e.getMessage(), e);
            return null;
        }
    }

    private boolean isTaskCompleted(Task apiTask) {
        // 1. Проверяем локальное поле
        if (apiTask.is_completed()) {
            return true;
        }

        // 2. Проверяем статус из БД
        if (apiTask.getTaskStatus() != null &&
                (apiTask.getTaskStatus().equals("completed") ||
                        apiTask.getTaskStatus().equals("выполнено"))) {
            return true;
        }

        // 3. Проверяем через метод isCompletedFromDB (если он есть в Task)
        try {
            // Проверяем наличие метода isCompletedFromDB
            java.lang.reflect.Method method = apiTask.getClass().getMethod("isCompletedFromDB");
            if ((boolean) method.invoke(apiTask)) {
                return true;
            }
        } catch (Exception e) {
            // Метод не существует, продолжаем проверку
        }

        // 4. Проверяем дату завершения
        if (apiTask.getTaskCompletionDate() != null &&
                !apiTask.getTaskCompletionDate().isEmpty()) {
            return true;
        }

        return false;
    }

    private String formatTimeRangeFromDatabase(String goalDate, String notifyStart) {
        try {
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

            String startTime = "--:--";
            String endTime = "--:--";

            // Парсим время уведомления (начало)
            if (notifyStart != null && !notifyStart.isEmpty()) {
                Date notifyDate = dbFormat.parse(notifyStart);
                startTime = timeFormat.format(notifyDate);
            }

            // Парсим время цели (окончание)
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

    private String getTaskStatusForCalendar(Task task) {
        try {
            // Сначала проверяем, выполнена ли задача
            if (isTaskCompleted(task)) {
                return "выполнено";
            }

            if (task.getTaskGoalDate() != null && !task.getTaskGoalDate().isEmpty()) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date goalDate = format.parse(task.getTaskGoalDate());
                Date now = new Date();

                // Если задача на сегодня
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String todayStr = dateFormat.format(now);
                String taskDateStr = task.getTaskGoalDate().substring(0, 10);

                if (taskDateStr.equals(todayStr)) {
                    if (goalDate.before(now)) {
                        return "просрочено";
                    } else {
                        return "текущая";
                    }
                } else if (goalDate.before(now)) {
                    return "просрочено";
                } else {
                    return "назначена";
                }
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date for status", e);
        } catch (Exception e) {
            Log.e(TAG, "Error determining task status", e);
        }

        return "назначена";
    }

    private void update_tasks_list(List<Task> tasks) {
        if (tasks.isEmpty()) {
            show_no_tasks_message();
            return;
        }

        day_tasks = tasks;

        if (task_adapter == null) {
            task_adapter = new CalendarTaskAdapter(day_tasks, this);
            rv_day_tasks.setAdapter(task_adapter);
        } else {
            task_adapter = new CalendarTaskAdapter(day_tasks, this);
            rv_day_tasks.setAdapter(task_adapter);
        }

        // Прокручиваем к началу списка
        rv_day_tasks.scrollToPosition(0);
    }

    private void show_no_tasks_message() {
        // Создаем специальную задачу для сообщения "Нет задач"
        Task noTasksMessage = new Task();
        noTasksMessage.set_title("Нет задач на этот день");
        noTasksMessage.set_time("");
        noTasksMessage.set_status("");

        List<Task> messageList = new ArrayList<>();
        messageList.add(noTasksMessage);

        task_adapter = new CalendarTaskAdapter(messageList, this);
        rv_day_tasks.setAdapter(task_adapter);

        Toast.makeText(this, "Нет задач на выбранный день", Toast.LENGTH_SHORT).show();
    }

    private void update_selected_date() {
        try {
            // Создаем дату для выбранного дня
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(Calendar.YEAR, currentCalendar.get(Calendar.YEAR));
            selectedDate.set(Calendar.MONTH, currentCalendar.get(Calendar.MONTH));
            selectedDate.set(Calendar.DAY_OF_MONTH, selectedDay);

            // Форматируем дату по-русски
            String formattedDate = dayNameFormat.format(selectedDate.getTime());

            // Делаем первую букву заглавной
            if (!formattedDate.isEmpty()) {
                formattedDate = formattedDate.substring(0, 1).toUpperCase() + formattedDate.substring(1);
            }

            tv_selected_date.setText(formattedDate);
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date: " + e.getMessage(), e);
            tv_selected_date.setText("Выберите дату");
        }
    }

    // Методы для навигации по месяцам
    public void goToPreviousMonth(View view) {
        currentCalendar.add(Calendar.MONTH, -1);
        selectedDay = 1; // Сбрасываем на первый день
        setup_calendar();
        load_tasks_for_selected_day();
    }

    public void goToNextMonth(View view) {
        currentCalendar.add(Calendar.MONTH, 1);
        selectedDay = 1; // Сбрасываем на первый день
        setup_calendar();
        load_tasks_for_selected_day();
    }

    public void goToToday(View view) {
        currentCalendar = Calendar.getInstance();
        selectedDay = currentCalendar.get(Calendar.DAY_OF_MONTH);
        setup_calendar();
        load_tasks_for_selected_day();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "CalendarActivity resumed");
        // При возвращении в активность обновляем задачи
        if (currentUserId != -1) {
            load_tasks_for_selected_day();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "CalendarActivity started");
        // Также обновляем при старте активности
        if (currentUserId != -1) {
            load_tasks_for_selected_day();
        }
    }
}
