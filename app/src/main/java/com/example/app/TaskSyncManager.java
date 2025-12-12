package com.example.app;

import android.content.Context;
import android.util.Log;

import com.example.app.ApiResponse;
import com.example.app.ApiService;
import com.example.app.RetrofitClient;
import com.example.app.TasksResponse;
import com.example.app.Task;
import com.example.app.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TaskSyncManager {
    private static final String TAG = "TaskSyncManager";
    private Context context;
    private ApiService apiService;

    public TaskSyncManager(Context context) {
        this.context = context;
        this.apiService = RetrofitClient.getApiService();
    }

    // Синхронизация задач между сервером и локальным файлом
    public void syncTasks(int userId, boolean hasTasksOnServer, boolean hasFileTasks, SyncCallback callback) {
        Log.d(TAG, "Starting sync - User: " + userId +
                ", HasServerTasks: " + hasTasksOnServer +
                ", HasFileTasks: " + hasFileTasks);

        if (hasTasksOnServer && hasFileTasks) {
            // Ситуация 1: Есть задачи и на сервере, и в файле → сравнить и объединить
            syncAndMergeTasks(userId, callback);
        } else if (!hasTasksOnServer && hasFileTasks) {
            // Ситуация 2: Нет задач на сервере, но есть в файле → загрузить из файла на сервер
            uploadTasksFromFile(userId, callback);
        } else if (hasTasksOnServer && !hasFileTasks) {
            // Ситуация 3: Есть задачи на сервере, но нет в файле → скачать с сервера в файл
            downloadTasksToFile(userId, callback);
        } else {
            // Ситуация 4: Нет задач нигде
            callback.onSyncComplete("Нет задач для синхронизации", 0);
        }
    }

    // Синхронизация и слияние задач (сервер + файл)
    private void syncAndMergeTasks(int userId, SyncCallback callback) {
        // 1. Получить задачи с сервера
        loadTasksFromServer(userId, new SyncCallback() {
            @Override
            public void onSyncComplete(String message, int serverTaskCount) {
                // 2. Получить задачи из файла
                List<Task> fileTasks = loadTasksFromFile();

                if (fileTasks.isEmpty()) {
                    callback.onSyncComplete("Нет задач в файле для слияния", serverTaskCount);
                    return;
                }

                // 3. Для каждой задачи из файла проверить, есть ли она на сервере
                checkAndUploadNewTasks(userId, fileTasks, serverTaskCount, callback);
            }

            @Override
            public void onSyncError(String error) {
                callback.onSyncError("Ошибка получения задач с сервера: " + error);
            }
        });
    }

    // Проверить и загрузить новые задачи на сервер
    private void checkAndUploadNewTasks(int userId, List<Task> fileTasks, int existingCount, SyncCallback callback) {
        // TODO: Реализовать проверку существующих задач на сервере
        // Пока просто считаем, что все задачи из файла - новые

        Log.d(TAG, "Found " + fileTasks.size() + " tasks in file");

        if (fileTasks.isEmpty()) {
            callback.onSyncComplete("Нет новых задач для загрузки", existingCount);
            return;
        }

        // Устанавливаем user_id для всех задач
        for (Task task : fileTasks) {
            task.setUserId(userId);
        }

        // Загружаем задачи на сервер (пока по одной)
        uploadTasksSequentially(fileTasks, 0, existingCount, callback);
    }

    private void uploadTasksSequentially(List<Task> tasks, int index, int existingCount, SyncCallback callback) {
        if (index >= tasks.size()) {
            // Все задачи загружены
            callback.onSyncComplete("Загружено " + tasks.size() + " новых задач", existingCount + tasks.size());
            return;
        }

        Task task = tasks.get(index);
        Call<ApiResponse> call = apiService.createTask(task);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d(TAG, "Task uploaded: " + task.getTaskName());
                    // Загружаем следующую задачу
                    uploadTasksSequentially(tasks, index + 1, existingCount, callback);
                } else {
                    String error = response.body() != null ? response.body().getMessage() : "Ошибка сервера";
                    Log.e(TAG, "Failed to upload task: " + error);
                    // Продолжаем со следующей задачей, даже если эта не удалась
                    uploadTasksSequentially(tasks, index + 1, existingCount, callback);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "Network error uploading task: " + t.getMessage());
                // Продолжаем со следующей задачей
                uploadTasksSequentially(tasks, index + 1, existingCount, callback);
            }
        });
    }

    // Загрузить задачи из файла на сервер
    public void uploadTasksFromFile(int userId, SyncCallback callback) {
        Log.d(TAG, "Uploading tasks from file for user: " + userId);

        List<Task> fileTasks = loadTasksFromFile();

        if (fileTasks.isEmpty()) {
            callback.onSyncComplete("Файл задач пуст", 0);
            return;
        }

        // Установить user_id для всех задач
        for (Task task : fileTasks) {
            task.setUserId(userId);
        }

        // Загружаем задачи на сервер
        uploadTasksSequentially(fileTasks, 0, 0, callback);
    }

    // Скачать задачи с сервера в файл
    private void downloadTasksToFile(int userId, SyncCallback callback) {
        Log.d(TAG, "Downloading tasks to file for user: " + userId);

        Call<TasksResponse> call = apiService.getTasks(userId);
        call.enqueue(new Callback<TasksResponse>() {
            @Override
            public void onResponse(Call<TasksResponse> call, Response<TasksResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TasksResponse tasksResponse = response.body();

                    if (tasksResponse.isSuccess() && tasksResponse.getTasks() != null) {
                        List<Task> serverTasks = tasksResponse.getTasks();
                        boolean success = saveTasksToFile(serverTasks);

                        if (success) {
                            callback.onSyncComplete("Сохранено " + serverTasks.size() + " задач в файл", serverTasks.size());
                        } else {
                            callback.onSyncError("Ошибка сохранения файла");
                        }
                    } else {
                        callback.onSyncError("Нет задач на сервере");
                    }
                } else {
                    callback.onSyncError("Ошибка сервера: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<TasksResponse> call, Throwable t) {
                callback.onSyncError("Ошибка сети: " + t.getMessage());
            }
        });
    }

    // Загрузить задачи с сервера
    public void loadTasksFromServer(int userId, SyncCallback callback) {
        Log.d(TAG, "Loading tasks from server for user: " + userId);

        Call<TasksResponse> call = apiService.getTasks(userId);
        call.enqueue(new Callback<TasksResponse>() {
            @Override
            public void onResponse(Call<TasksResponse> call, Response<TasksResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TasksResponse tasksResponse = response.body();

                    if (tasksResponse.isSuccess() && tasksResponse.getTasks() != null) {
                        int taskCount = tasksResponse.getTasks().size();
                        callback.onSyncComplete("Загружено " + taskCount + " задач с сервера", taskCount);
                    } else {
                        callback.onSyncError("Нет задач на сервере");
                    }
                } else {
                    callback.onSyncError("Ошибка сервера: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<TasksResponse> call, Throwable t) {
                callback.onSyncError("Ошибка сети: " + t.getMessage());
            }
        });
    }

    // Загрузить задачи из файла
    public List<Task> loadTasksFromFile() {
        List<Task> tasks = new ArrayList<>();

        try {
            File tasksFile = FileUtils.getTasksFile(context);
            if (!tasksFile.exists()) {
                Log.d(TAG, "Tasks file doesn't exist");
                return tasks;
            }

            String jsonContent = FileUtils.readFile(tasksFile);
            if (jsonContent.isEmpty()) {
                Log.d(TAG, "Tasks file is empty");
                return tasks;
            }

            JSONArray jsonArray = new JSONArray(jsonContent);
            Log.d(TAG, "Found " + jsonArray.length() + " tasks in file");

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                Task task = new Task();
                task.setTaskName(jsonObject.optString("task_name", ""));
                task.setTaskType(jsonObject.optString("task_type", "book"));
                task.setTaskImportance(jsonObject.optString("task_importance", "3"));
                task.setTaskGoalDate(jsonObject.optString("task_goal_date", ""));
                task.setNotifyStart(jsonObject.optString("notify_start", ""));
                task.setTaskNote(jsonObject.optString("task_note", ""));
                task.setTaskReward(jsonObject.optInt("task_reward", 0));

                tasks.add(task);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading tasks from file", e);
        }

        return tasks;
    }

    // Сохранить задачи в файл
    public boolean saveTasksToFile(List<Task> tasks) {
        try {
            JSONArray jsonArray = new JSONArray();

            for (Task task : tasks) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("task_name", task.getTaskName());
                jsonObject.put("task_type", task.getTaskType());
                jsonObject.put("task_importance", task.getTaskImportance());
                jsonObject.put("task_goal_date", task.getTaskGoalDate());
                jsonObject.put("notify_start", task.getNotifyStart());
                jsonObject.put("task_note", task.getTaskNote());
                jsonObject.put("task_reward", task.getTaskReward());

                jsonArray.put(jsonObject);
            }

            File tasksFile = FileUtils.getTasksFile(context);
            boolean success = FileUtils.writeFile(tasksFile, jsonArray.toString());

            if (success) {
                Log.d(TAG, "Saved " + tasks.size() + " tasks to file");
            } else {
                Log.e(TAG, "Failed to save tasks to file");
            }

            return success;

        } catch (Exception e) {
            Log.e(TAG, "Error saving tasks to file", e);
            return false;
        }
    }

    // Найти новые задачи (есть в файле, но нет на сервере)
    private List<Task> findNewTasks(List<Task> serverTasks, List<Task> fileTasks) {
        List<Task> newTasks = new ArrayList<>();

        if (serverTasks.isEmpty()) {
            return fileTasks;
        }

        // Создаем список названий задач с сервера для быстрого поиска
        List<String> serverTaskNames = new ArrayList<>();
        for (Task task : serverTasks) {
            serverTaskNames.add(task.getTaskName());
        }

        // Находим задачи, которых нет на сервере
        for (Task fileTask : fileTasks) {
            if (!serverTaskNames.contains(fileTask.getTaskName())) {
                newTasks.add(fileTask);
            }
        }

        Log.d(TAG, "Found " + newTasks.size() + " new tasks in file");
        return newTasks;
    }

    // Создать файл с тестовыми задачами (для демонстрации)
    public void createSampleTasksFile() {
        List<Task> sampleTasks = new ArrayList<>();

        // Создаем несколько тестовых задач
        Task task1 = new Task();
        task1.setTaskName("Прочитать книгу");
        task1.setTaskType("book");
        task1.setTaskImportance("3");
        task1.setTaskGoalDate("2024-12-10 18:00:00");
        task1.setNotifyStart("2024-12-10 17:00:00");
        task1.setTaskNote("Первая глава");
        task1.setTaskReward(10);
        sampleTasks.add(task1);

        Task task2 = new Task();
        task2.setTaskName("Встреча с командой");
        task2.setTaskType("meeting");
        task2.setTaskImportance("4");
        task2.setTaskGoalDate("2024-12-10 14:00:00");
        task2.setNotifyStart("2024-12-10 13:45:00");
        task2.setTaskNote("Обсуждение проекта");
        task2.setTaskReward(15);
        sampleTasks.add(task2);

        Task task3 = new Task();
        task3.setTaskName("Отдохнуть");
        task3.setTaskType("coffee");
        task3.setTaskImportance("2");
        task3.setTaskGoalDate("2024-12-10 15:30:00");
        task3.setNotifyStart("2024-12-10 15:00:00");
        task3.setTaskNote("Кофе-брейк");
        task3.setTaskReward(5);
        sampleTasks.add(task3);

        // Сохраняем в файл
        saveTasksToFile(sampleTasks);
        Log.d(TAG, "Created sample tasks file with " + sampleTasks.size() + " tasks");
    }

    // Проверить наличие задач в файле
    public boolean hasTasksInFile() {
        File tasksFile = FileUtils.getTasksFile(context);
        return tasksFile.exists() && tasksFile.length() > 0;
    }

    // Проверить наличие задач на сервере (асинхронно)
    public void checkServerTasks(int userId, ServerCheckCallback callback) {
        Call<TasksResponse> call = apiService.getTasks(userId);
        call.enqueue(new Callback<TasksResponse>() {
            @Override
            public void onResponse(Call<TasksResponse> call, Response<TasksResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TasksResponse tasksResponse = response.body();
                    boolean hasTasks = tasksResponse.isSuccess() &&
                            tasksResponse.getTasks() != null &&
                            !tasksResponse.getTasks().isEmpty();
                    callback.onCheckComplete(hasTasks);
                } else {
                    callback.onCheckError("Ошибка проверки сервера");
                }
            }

            @Override
            public void onFailure(Call<TasksResponse> call, Throwable t) {
                callback.onCheckError("Ошибка сети: " + t.getMessage());
            }
        });
    }

    public interface ServerCheckCallback {
        void onCheckComplete(boolean hasTasks);
        void onCheckError(String error);
    }

    public void checkGuestTasks(int guestId, ServerCheckCallback callback) {
        Log.d(TAG, "Checking tasks for guest: " + guestId);

        // Для гостей проверяем только локальное хранилище
        boolean hasTasks = hasGuestTasksInStorage(guestId);

        if (hasTasks) {
            callback.onCheckComplete(true);
        } else {
            callback.onCheckComplete(false);
        }
    }

    private boolean hasGuestTasksInStorage(int guestId) {
        // Проверяем локальную БД или файл на наличие задач гостя
        try {
            // Здесь логика проверки локального хранилища
            // Например, проверка файла tasks_guest_[id].json
            return false; // Пока возвращаем false
        } catch (Exception e) {
            Log.e(TAG, "Error checking guest tasks: " + e.getMessage());
            return false;
        }
    }

    // Загрузить задачи гостя
    public List<Task> loadGuestTasks(int guestId) {
        List<Task> tasks = new ArrayList<>();
        Log.d(TAG, "Loading tasks for guest: " + guestId);

        try {
            // Логика загрузки из локального хранилища
            // Например, из файла или локальной БД
            return tasks;
        } catch (Exception e) {
            Log.e(TAG, "Error loading guest tasks: " + e.getMessage());
            return tasks;
        }
    }

    // Сохранить задачи гостя
    public boolean saveGuestTasks(int guestId, List<Task> tasks) {
        Log.d(TAG, "Saving " + tasks.size() + " tasks for guest: " + guestId);

        try {
            // Логика сохранения в локальное хранилище
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving guest tasks: " + e.getMessage());
            return false;
        }
    }

    // Интерфейс обратного вызова
    public interface SyncCallback {
        void onSyncComplete(String message, int taskCount);
        void onSyncError(String error);
    }
}