package com.example.app;


import android.content.Context;
import android.util.Log;
import com.example.app.ApiResponse;
import com.example.app.ApiService;
import com.example.app.LoginRequest;
import com.example.app.LoginResponse;
import com.example.app.RetrofitClient;
import com.example.app.User;
import com.example.app.SharedPrefs;
import com.example.app.ValidationUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.File;

public class AuthManager {
    private static final String TAG = "AuthManager";
    private Context context;
    private ApiService apiService;
    private TaskSyncManager taskSyncManager;

    public AuthManager(Context context) {
        this.context = context;
        this.apiService = RetrofitClient.getApiService();
        this.taskSyncManager = new TaskSyncManager(context);
    }

    // Регистрация пользователя через API
    public void registerUser(String name, String email, String password, AuthCallback callback) {
        // 1. Проверка валидности данных
        if (!ValidationUtils.isValidEmail(email)) {
            callback.onError("Неверный формат email");
            return;
        }

        if (!ValidationUtils.isValidPassword(password)) {
            callback.onError("Пароль должен содержать минимум 6 символов");
            return;
        }

        // 2. Создание пользователя для API
        User newUser = new User(name, email, password);

        // 3. Отправка запроса на регистрацию через API
        Call<ApiResponse> call = apiService.register(newUser);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        Log.d(TAG, "Registration successful. User ID: " + apiResponse.getUserId());

                        // 4. Сохранение сессии (используем метод saveUserSession)
                        SharedPrefs.saveUserSession(context, apiResponse.getUserId(), email, name);

                        // 5. Проверка и загрузка задач из файла
                        loadTasksFromFileAfterRegistration(apiResponse.getUserId(), callback);
                    } else {
                        callback.onError(apiResponse.getMessage());
                    }
                } else {
                    String errorMsg = "Ошибка сервера: " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "Registration API call failed", t);
                callback.onError("Ошибка сети: " + t.getMessage());
            }
        });
    }

    // Авторизация пользователя через API
    public void loginUser(String email, String password, AuthCallback callback) {
        // 1. Проверка введенных данных
        if (email.isEmpty() || password.isEmpty()) {
            callback.onError("Заполните все поля");
            return;
        }

        // 2. Создание запроса для API
        LoginRequest loginRequest = new LoginRequest(email, password);

        // 3. Отправка запроса на авторизацию через API
        Call<LoginResponse> call = apiService.login(loginRequest);
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    if (loginResponse.isSuccess() && loginResponse.getUser() != null) {
                        User user = loginResponse.getUser();
                        Log.d(TAG, "Login successful. User ID: " + user.getIdusers());

                        // 4. Сохранение сессии
                        saveUserToSharedPrefs(user);

                        // 5. Проверка и синхронизация задач
                        checkAndSyncTasks(user.getIdusers(), callback);
                    } else {
                        callback.onError(loginResponse.getMessage());
                    }
                } else {
                    String errorMsg = "Ошибка сервера: " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.e(TAG, "Login API call failed", t);
                callback.onError("Ошибка сети: " + t.getMessage());
            }
        });
    }

    // Метод для сохранения пользователя в SharedPrefs
    private void saveUserToSharedPrefs(User user) {
        Log.d(TAG, "Saving user to SharedPrefs: ID=" + user.getIdusers() +
                ", Name=" + user.getName() +
                ", Email=" + user.getEmail());

        if (user.getIdusers() <= 0) {
            Log.e(TAG, "Invalid user ID: " + user.getIdusers());
        }

        SharedPrefs.saveUserSession(
                context,
                user.getIdusers(),
                user.getEmail(),
                user.getName()
        );

        // Проверим сразу после сохранения
        int savedId = SharedPrefs.getUserId(context);
        Log.d(TAG, "Saved user ID in SharedPrefs: " + savedId);
    }

    // Проверка и синхронизация задач после авторизации
    private void checkAndSyncTasks(int userId, AuthCallback callback) {
        // Проверка наличия файла с задачами
        boolean hasFileTasks = taskSyncManager.hasTasksInFile();

        // Проверяем, есть ли задачи на сервере
        checkServerTasks(userId, hasFileTasks, callback);
    }

    private void checkServerTasks(int userId, boolean hasFileTasks, AuthCallback callback) {
        // Загружаем задачи с сервера
        taskSyncManager.loadTasksFromServer(userId, new TaskSyncManager.SyncCallback() {
            @Override
            public void onSyncComplete(String message, int taskCount) {
                // Проверяем, нужно ли синхронизировать с файлом
                if (hasFileTasks) {
                    uploadTasksFromFile(userId, callback);
                } else {
                    // Нет файла, просто завершаем
                    completeAuthProcess(userId, message, callback);
                }
            }

            @Override
            public void onSyncError(String error) {
                // Если ошибка загрузки с сервера, пробуем синхронизировать файл
                if (hasFileTasks) {
                    uploadTasksFromFile(userId, callback);
                } else {
                    // Нет ни задач на сервере, ни в файле
                    completeAuthProcess(userId, "Авторизация успешна", callback);
                }
            }
        });
    }

    private void uploadTasksFromFile(int userId, AuthCallback callback) {
        taskSyncManager.uploadTasksFromFile(userId, new TaskSyncManager.SyncCallback() {
            @Override
            public void onSyncComplete(String message, int taskCount) {
                completeAuthProcess(userId, message, callback);
            }

            @Override
            public void onSyncError(String error) {
                // Даже если синхронизация файла не удалась, всё равно завершаем авторизацию
                completeAuthProcess(userId, "Авторизация успешна (ошибка синхронизации файла)", callback);
            }
        });
    }

    private void completeAuthProcess(int userId, String message, AuthCallback callback) {
        // Получаем пользователя из SharedPrefs
        User user = SharedPrefs.getCurrentUser(context);
        if (user != null) {
            AuthResponse response = new AuthResponse();
            response.setSuccess(true);
            response.setUser(user);
            response.setMessage(message);
            callback.onSuccess(response);
        } else {
            callback.onError("Ошибка получения данных пользователя");
        }
    }

    // Загрузка задач из файла после регистрации
    private void loadTasksFromFileAfterRegistration(int userId, AuthCallback callback) {
        boolean hasFileTasks = taskSyncManager.hasTasksInFile();

        if (hasFileTasks) {
            // Файл существует и не пустой - загружаем задачи
            taskSyncManager.uploadTasksFromFile(userId, new TaskSyncManager.SyncCallback() {
                @Override
                public void onSyncComplete(String message, int taskCount) {
                    completeRegistration(userId, "Регистрация успешна. " + message, callback);
                }

                @Override
                public void onSyncError(String error) {
                    // Даже если не удалось загрузить задачи, регистрация считается успешной
                    completeRegistration(userId, "Регистрация успешна (ошибка загрузки задач)", callback);
                }
            });
        } else {
            // Файла нет или он пустой - просто завершаем регистрацию
            completeRegistration(userId, "Регистрация успешна", callback);
        }
    }

    private void completeRegistration(int userId, String message, AuthCallback callback) {
        // Создаем объект пользователя с данными из SharedPrefs
        String name = SharedPrefs.getUserName(context);
        String email = SharedPrefs.getUserEmail(context);

        User user = new User(name, email, "");
        user.setIdusers(userId);
        user.setCoins(0);

        AuthResponse response = new AuthResponse();
        response.setSuccess(true);
        response.setUser(user);
        response.setMessage(message);
        callback.onSuccess(response);
    }

    // Выход из системы
    public void logout() {
        SharedPrefs.clearUserSession(context);
    }

    // Проверка авторизации
    public boolean isLoggedIn() {
        return SharedPrefs.isLoggedIn(context);
    }

    // Получить текущего пользователя
    public User getCurrentUser() {
        if (isLoggedIn()) {
            return SharedPrefs.getCurrentUser(context);
        }
        return null;
    }

    // Интерфейс обратного вызова
    public interface AuthCallback {
        void onSuccess(AuthResponse response);
        void onError(String error);
    }
}