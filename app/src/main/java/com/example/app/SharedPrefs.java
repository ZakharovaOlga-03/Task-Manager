package com.example.app;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.app.User;
import com.example.app.data.local.AppDatabase;

public class SharedPrefs {
    private static final String PREFS_NAME = "TaskManagerPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_TOKEN = "user_token";
    private static final String KEY_IS_GUEST = "is_guest";
    private static final String KEY_GUEST_ID = "guest_id";
    private static final String KEY_GUEST_NAME = "guest_name";
    private static final String KEY_USER_COINS = "guest_coin";

    public static void saveGuestSession(Context context, int guestId, String guestName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt(KEY_USER_ID, guestId);
        editor.putString(KEY_USER_EMAIL, "guest_" + guestId + "@guest.local");
        editor.putString(KEY_USER_NAME, guestName);
        editor.putString(KEY_GUEST_NAME, guestName);
        editor.putInt(KEY_GUEST_ID, guestId);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putBoolean(KEY_IS_GUEST, true);
        editor.putInt(KEY_USER_COINS, 0);

        editor.apply();
        log("Guest session saved: ID=" + guestId + ", Name=" + guestName);
    }

    // Проверить авторизацию (включая гостей)
    public static boolean isLoggedIn(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public static boolean isGuest(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_GUEST, false);
    }

    // Получить ID пользователя (для гостя будет отрицательный ID)
    public static int getUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_USER_ID, -1);
    }

    public static int getGuestId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_GUEST_ID, -1);
    }

    // Получение имени пользователя
    public static String getUserName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_NAME, null);
    }

    // Получение email пользователя
    public static String getUserEmail(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    // Получение токена пользователя
    public static String getUserToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_TOKEN, null);
    }

    // Сохранить сессию зарегистрированного пользователя
    public static void saveUserSession(Context context, int userId, String email, String name) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_NAME, name);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putBoolean(KEY_IS_GUEST, false);
        editor.putInt(KEY_USER_COINS, 0);

        editor.apply();
        log("User session saved: ID=" + userId + ", Email=" + email);
    }

    // Сохранение данных пользователя (с токеном)
    public static void saveUserData(Context context, int userId, String name, String email, String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        if (token != null) {
            editor.putString(KEY_USER_TOKEN, token);
        }
        editor.putBoolean(KEY_IS_LOGGED_IN, true);

        editor.apply();
    }

    // Сохранение данных пользователя (без токена)
    public static void saveUserData(Context context, int userId, String name, String email) {
        saveUserData(context, userId, name, email, null);
    }

    // ПОЛУЧЕНИЕ ТЕКУЩЕГО ПОЛЬЗОВАТЕЛЯ
    public static User getCurrentUser(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (!isLoggedIn(context)) {
            return null;
        }

        User user = new User();
        user.setIdusers(prefs.getInt(KEY_USER_ID, -1));
        user.setEmail(prefs.getString(KEY_USER_EMAIL, ""));
        user.setName(prefs.getString(KEY_USER_NAME, ""));
        user.setCoins(prefs.getInt(KEY_USER_COINS, 0));
        user.setActive(true);

        if (isGuest(context)) {
            user.setGuest(true);
        }

        return user;
    }

    public static void logoutGuest(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Только сбрасываем флаг авторизации, но сохраняем данные гостя
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.apply();

        log("Guest logged out (data preserved)");
    }

    public static void clearAllGuestData(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int guestId = prefs.getInt(KEY_GUEST_ID, -1);

        if (guestId != -1) {
            // 1. Очищаем SharedPreferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();

            // 2. Очищаем задачи гостя из локальной БД
            clearGuestTasksFromDatabase(context, guestId);

            log("All guest data cleared for ID: " + guestId);
        }
    }

    private static void clearGuestTasksFromDatabase(Context context, int guestId) {
        new Thread(() -> {
            try {
                AppDatabase database = AppDatabase.getInstance(context);
                database.taskDao().deleteAllUserTasks(guestId);
                log("Guest tasks deleted from database for ID: " + guestId);
            } catch (Exception e) {
                log("Error clearing guest tasks: " + e.getMessage());
            }
        }).start();
    }

    // Полная очистка сессии
    public static void clearUserSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        log("All sessions cleared");
    }

    private static void log(String message) {
        android.util.Log.d("SharedPrefs", message);
    }

    // Очистка данных пользователя
    public static void clearUserData(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USER_NAME);
        editor.remove(KEY_USER_EMAIL);
        editor.remove(KEY_USER_TOKEN);
        editor.putBoolean(KEY_IS_LOGGED_IN, false);

        editor.apply();
    }

    // Сохранение только токена
    public static void saveToken(Context context, String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(KEY_USER_TOKEN, token);
        editor.apply();
    }

    // Обновление имени пользователя
    public static void updateUserName(Context context, String name) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(KEY_USER_NAME, name);
        editor.apply();
    }

    // Обновление email пользователя
    public static void updateUserEmail(Context context, String email) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(KEY_USER_EMAIL, email);
        editor.apply();
    }
}