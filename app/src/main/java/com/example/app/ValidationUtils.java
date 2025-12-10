package com.example.app;

import java.util.regex.Pattern;

public class ValidationUtils {

    // Проверка email
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }

    // Проверка пароля
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    // Проверка имени
    public static boolean isValidName(String name) {
        return name != null && name.length() >= 2 && name.length() <= 50;
    }

    // Проверка телефона
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }

        // Простая проверка на цифры и длину
        String phoneRegex = "^[0-9]{10,15}$";
        return phone.matches(phoneRegex);
    }
}