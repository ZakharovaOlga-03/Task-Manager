package com.example.app;


import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    // Регистрация
    @POST("register.php")
    Call<ApiResponse> register(@Body User user);

    // Авторизация
    @POST("login.php")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    // Получить задачи пользователя
    @GET("tasks.php")
    Call<TasksResponse> getTasks(@Query("user_id") int userId);

    // Создать задачу
    @POST("tasks.php")
    Call<ApiResponse> createTask(@Body Task task);

    // Получить данные пользователя
    @GET("users.php")
    Call<User> getUser(@Query("user_id") int userId);
}