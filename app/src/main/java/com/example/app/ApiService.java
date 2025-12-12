package com.example.app;


import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Field;

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

    @GET("api/stats")
    Call<StatsResponse> getStats(@Query("user_id") int userId);

    // Добавляем метод для обновления статуса задачи
    @FormUrlEncoded
    @POST("update_task_status.php")
    Call<UpdateTaskResponse> updateTaskStatus(
            @Field("task_id") int taskId,
            @Field("status") String status,
            @Field("is_completed") boolean isCompleted
    );

    // Получить профиль пользователя
    @GET("profile.php")
    Call<UserProfileResponse> getProfile(@Query("user_id") int userId);

    // Выйти из аккаунта
    @POST("logout.php")
    Call<ApiResponse> logout(@Body LogoutRequest request);
}