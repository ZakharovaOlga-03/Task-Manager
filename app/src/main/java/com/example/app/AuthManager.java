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
import java.util.Random;

public class AuthManager {
    private static final String TAG = "AuthManager";
    private Context context;
    private ApiService apiService;
    private TaskSyncManager taskSyncManager;
    private Random random;

    // Константы для гостевых пользователей
    private static final int GUEST_ID_START = -1000; // Отрицательные ID для гостей
    private static final String GUEST_PREFIX = "Гость_";

    public AuthManager(Context context) {
        this.context = context;
        this.apiService = RetrofitClient.getApiService();
        this.taskSyncManager = new TaskSyncManager(context);
        this.random = new Random();
    }

    // ========== ОБНОВЛЁННЫЕ МЕТОДЫ ДЛЯ ГОСТЕВОЙ АУТЕНТИФИКАЦИИ ==========

    // Вход как новый гость (создание нового гостевого аккаунта)
    public void loginAsGuest(AuthCallback callback) {
        Log.d(TAG, "Starting new guest login");

        // Генерируем уникальный ID для гостя
        int guestId = generateGuestId();
        String guestName = generateGuestName();

        Log.d(TAG, "Generated new guest: ID=" + guestId + ", Name=" + guestName);

        // Сохраняем гостевую сессию
        SharedPrefs.saveGuestSession(context, guestId, guestName);

        // Создаем объект пользователя для ответа
        User guestUser = createGuestUser(guestId, guestName);

        // Загружаем локальные задачи гостя (если есть)
        loadGuestTasks(guestId, guestUser, callback);
    }

    // Восстановление существующей гостевой сессии
    public void restoreGuestSession(AuthCallback callback) {
        Log.d(TAG, "Restoring existing guest session");

        // Получаем сохраненного гостя из SharedPrefs
        int guestId = SharedPrefs.getGuestId(context);
        String guestName = SharedPrefs.getUserName(context);

        if (guestId != -1 && guestName != null && !guestName.isEmpty()) {
            Log.d(TAG, "Restoring existing guest: ID=" + guestId + ", Name=" + guestName);

            // Восстанавливаем сессию
            SharedPrefs.saveGuestSession(context, guestId, guestName);

            User guestUser = createGuestUser(guestId, guestName);

            // Загружаем задачи
            loadGuestTasks(guestId, guestUser, callback);
        } else {
            // Нет сохраненного гостя - создаем нового
            Log.d(TAG, "No existing guest found, creating new one");
            loginAsGuest(callback);
        }
    }

    // Проверка и восстановление любой сессии (гость или пользователь)
    public void checkAndRestoreSession(AuthCallback callback) {
        Log.d(TAG, "Checking and restoring session");

        if (SharedPrefs.isLoggedIn(context)) {
            // Уже есть какая-то сессия
            if (SharedPrefs.isGuest(context)) {
                // Это гостевая сессия
                restoreGuestSession(callback);
            } else {
                // Это сессия зарегистрированного пользователя
                User user = SharedPrefs.getCurrentUser(context);
                if (user != null) {
                    AuthResponse response = new AuthResponse();
                    response.setSuccess(true);
                    response.setUser(user);
                    response.setMessage("Добро пожаловать обратно!");
                    callback.onSuccess(response);
                } else {
                    callback.onError("Ошибка восстановления сессии");
                }
            }
        } else {
            // Нет активной сессии
            callback.onError("Требуется авторизация");
        }
    }

    // Генерация уникального гостевого ID
    private int generateGuestId() {
        // Генерируем отрицательное число в широком диапазоне
        return GUEST_ID_START - random.nextInt(1000000);
    }

    // Генерация имени гостя
    private String generateGuestName() {
        String[] adjectives = {"Весёлый", "Серьёзный", "Умный", "Быстрый", "Спокойный",
                "Смелый", "Добрый", "Любознательный", "Терпеливый", "Оптимистичный"};
        String[] nouns = {"Искатель", "Путешественник", "Исследователь", "Мечтатель", "Творец",
                "Наблюдатель", "Строитель", "Мыслитель", "Помощник", "Ученик"};

        String adjective = adjectives[random.nextInt(adjectives.length)];
        String noun = nouns[random.nextInt(nouns.length)];

        return adjective + " " + noun;
    }

    // Создание объекта гостя
    private User createGuestUser(int guestId, String guestName) {
        User guestUser = new User();
        guestUser.setIdusers(guestId);
        guestUser.setName(guestName);
        guestUser.setEmail("guest_" + Math.abs(guestId) + "@guest.local");
        guestUser.setCoins(0);
        guestUser.setActive(true);
        guestUser.setGuest(true);

        return guestUser;
    }

    // Загрузка задач гостя
    private void loadGuestTasks(int guestId, User guestUser, AuthCallback callback) {
        // Проверяем, есть ли локальные задачи для этого гостя
        taskSyncManager.checkGuestTasks(guestId, new TaskSyncManager.ServerCheckCallback() {
            @Override
            public void onCheckComplete(boolean hasTasks) {
                Log.d(TAG, "Guest has tasks: " + hasTasks);

                // Создаем ответ об успешной авторизации
                AuthResponse response = new AuthResponse();
                response.setSuccess(true);
                response.setUser(guestUser);
                response.setMessage("Добро пожаловать, " + guestUser.getName() + "!");

                if (hasTasks) {
                    response.setMessage("Добро пожаловать, " + guestUser.getName() + "! Ваши задачи загружены.");
                }

                callback.onSuccess(response);
            }

            @Override
            public void onCheckError(String error) {
                Log.e(TAG, "Error checking guest tasks: " + error);

                // Все равно считаем авторизацию успешной
                AuthResponse response = new AuthResponse();
                response.setSuccess(true);
                response.setUser(guestUser);
                response.setMessage("Добро пожаловать, " + guestUser.getName() + "!");

                callback.onSuccess(response);
            }
        });
    }

    // Проверка, является ли ID гостевым
    public boolean isGuestId(int userId) {
        return userId < 0;
    }

    // Проверка, является ли текущий пользователь гостем
    public boolean isCurrentUserGuest() {
        User user = getCurrentUser();
        return user != null && isGuestId(user.getIdusers());
    }

    // Миграция гостя в зарегистрированного пользователя
//    public void migrateGuestToUser(int oldGuestId, User newUser, AuthCallback callback) {
//        Log.d(TAG, "Migrating guest " + oldGuestId + " to user " + newUser.getIdusers());
//
//        // 1. Переносим задачи гостя в новый аккаунт
//        migrateGuestTasks(oldGuestId, newUser.getIdusers(), new TaskSyncManager.SyncCallback() {
//            @Override
//            public void onSyncComplete(String message, int taskCount) {
//                Log.d(TAG, "Guest tasks migrated: " + taskCount + " tasks");
//
//                // 2. Очищаем гостевую сессию (только сессию, не данные)
//                logoutGuest();
//
//                // 3. Сохраняем новую сессию пользователя
//                SharedPrefs.saveUserSession(context,
//                        newUser.getIdusers(),
//                        newUser.getEmail(),
//                        newUser.getName()
//                );
//
//                // 4. Отправляем успешный ответ
//                AuthResponse response = new AuthResponse();
//                response.setSuccess(true);
//                response.setUser(newUser);
//                response.setMessage(message);
//                response.setTaskCount(taskCount);
//
//                callback.onSuccess(response);
//            }
//
//            @Override
//            public void onSyncError(String error) {
//                Log.e(TAG, "Error migrating guest tasks: " + error);
//
//                // Даже если миграция не удалась, сохраняем пользователя
//                SharedPrefs.saveUserSession(context,
//                        newUser.getIdusers(),
//                        newUser.getEmail(),
//                        newUser.getName()
//                );
//
//                AuthResponse response = new AuthResponse();
//                response.setSuccess(true);
//                response.setUser(newUser);
//                response.setMessage("Регистрация успешна. Ошибка при переносе задач: " + error);
//
//                callback.onSuccess(response);
//            }
//        });
//    }

//    private void migrateGuestTasks(int oldGuestId, int newUserId, TaskSyncManager.SyncCallback callback) {
//        // Реализация переноса задач из гостевого аккаунта в новый
//        taskSyncManager.migrateGuestTasks(oldGuestId, newUserId, callback);
//    }

    // Выход для гостя (сохраняет данные для будущего восстановления)
    public void logoutGuest() {
        Log.d(TAG, "Logging out guest (preserving data)");
        SharedPrefs.logoutGuest(context);
    }

    // Полная очистка гостевых данных (по желанию пользователя)
    public void clearAllGuestData() {
        if (isCurrentUserGuest()) {
            User guest = getCurrentUser();
            if (guest != null) {
                int guestId = guest.getIdusers();
                Log.d(TAG, "Clearing all data for guest ID: " + guestId);
                SharedPrefs.clearAllGuestData(context);
            }
        }
    }

    // ========== СУЩЕСТВУЮЩИЙ ФУНКЦИОНАЛ ДЛЯ ЗАРЕГИСТРИРОВАННЫХ ПОЛЬЗОВАТЕЛЕЙ ==========

    // Регистрация пользователя через API
    public void registerUser(String name, String email, String password, AuthCallback callback) {
        Log.d(TAG, "Starting user registration: " + email);

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

                        // 4. Сохранение сессии
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
        Log.d(TAG, "Starting user login: " + email);

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

    // ========== ОБЩИЕ МЕТОДЫ ==========

    // Выход из системы (для всех типов пользователей)
    public void logout() {
        if (isCurrentUserGuest()) {
            logoutGuest(); // Для гостя - сохраняем данные
        } else {
            SharedPrefs.clearUserSession(context); // Для пользователя - полная очистка
        }
        Log.d(TAG, "User logged out");
    }

    // Проверка авторизации (включая гостей)
    public boolean isLoggedIn() {
        return SharedPrefs.isLoggedIn(context);
    }

    // Получить текущего пользователя (работает и для гостя)
    public User getCurrentUser() {
        if (isLoggedIn()) {
            User user = SharedPrefs.getCurrentUser(context);
            if (user != null && isGuestId(user.getIdusers())) {
                user.setGuest(true);
            }
            return user;
        }
        return null;
    }

    // Проверить, есть ли сохраненный гость (для показа кнопки восстановления)
    public boolean hasSavedGuest() {
        return SharedPrefs.getGuestId(context) != -1;
    }

    // Интерфейс обратного вызова
    public interface AuthCallback {
        void onSuccess(AuthResponse response);
        void onError(String error);
    }
}