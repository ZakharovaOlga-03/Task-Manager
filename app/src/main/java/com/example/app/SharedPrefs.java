package com.example.app;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefs {
    private static final String PREFS_NAME = "TaskManagerPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_TOKEN = "user_token";

    // Проверка, авторизован ли пользователь
    public static boolean isLoggedIn(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // Получение ID пользователя
    public static int getUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_USER_ID, -1);
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

    // СОХРАНЕНИЕ СЕССИИ ПОЛЬЗОВАТЕЛЯ (это то же самое, что saveUserData)
    public static void saveUserSession(Context context, int userId, String email, String name) {
        saveUserData(context, userId, name, email, null);
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
        if (!isLoggedIn(context)) {
            return null;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int userId = prefs.getInt(KEY_USER_ID, -1);
        String name = prefs.getString(KEY_USER_NAME, "");
        String email = prefs.getString(KEY_USER_EMAIL, "");

        // Создаем объект User
        User user = new User();
        user.setIdusers(userId);
        user.setName(name);
        user.setEmail(email);

        return user;
    }

    // ОЧИСТКА СЕССИИ ПОЛЬЗОВАТЕЛЯ (это то же самое, что clearUserData)
    public static void clearUserSession(Context context) {
        clearUserData(context);
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