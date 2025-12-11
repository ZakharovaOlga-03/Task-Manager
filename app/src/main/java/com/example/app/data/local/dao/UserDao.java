package com.example.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Query;
import androidx.room.OnConflictStrategy;
import androidx.lifecycle.LiveData;

import com.example.app.data.local.entities.UserEntity;
import java.util.List;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertUser(UserEntity user);

    @Update
    int updateUser(UserEntity user);

    @Query("SELECT * FROM users WHERE user_id = :userId")
    UserEntity getUserById(int userId);

    @Query("SELECT * FROM users WHERE email = :email")
    UserEntity getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE is_guest = 1")
    List<UserEntity> getGuestUsers();

    @Query("UPDATE users SET coins = :coins WHERE user_id = :userId")
    void updateUserCoins(int userId, int coins);

    @Query("UPDATE users SET last_login = :loginDate WHERE user_id = :userId")
    void updateLastLogin(int userId, java.util.Date loginDate);

    @Query("DELETE FROM users WHERE user_id = :userId")
    void deleteUser(int userId);

    @Query("DELETE FROM users WHERE is_guest = 1")
    void deleteAllGuests();

    @Query("SELECT COUNT(*) FROM users WHERE user_id = :userId")
    int userExists(int userId);
}