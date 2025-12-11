package com.example.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;
import androidx.room.OnConflictStrategy;
import androidx.lifecycle.LiveData;

import com.example.app.data.local.entities.TaskEntity;
import java.util.List;
import java.util.Date;

@Dao
public interface TaskDao {

    // Вставка задач
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertTask(TaskEntity task);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertTasks(List<TaskEntity> tasks);

    // Обновление задач
    @Update
    int updateTask(TaskEntity task);

    // Удаление задач
    @Delete
    void deleteTask(TaskEntity task);

    @Query("DELETE FROM tasks WHERE local_id = :localId")
    void deleteTaskById(int localId);

    @Query("DELETE FROM tasks WHERE user_id = :userId")
    void deleteAllUserTasks(int userId);

    // Получение задач
    @Query("SELECT * FROM tasks WHERE user_id = :userId AND is_deleted = 0 ORDER BY task_goal_date ASC")
    LiveData<List<TaskEntity>> getTasksByUser(int userId);

    @Query("SELECT * FROM tasks WHERE user_id = :userId AND status = :status AND is_deleted = 0")
    LiveData<List<TaskEntity>> getTasksByStatus(int userId, String status);

    @Query("SELECT * FROM tasks WHERE user_id = :userId AND date(task_goal_date) = date(:date) AND is_deleted = 0")
    LiveData<List<TaskEntity>> getTasksForDate(int userId, Date date);

    @Query("SELECT * FROM tasks WHERE user_id = :userId AND task_goal_date BETWEEN :startDate AND :endDate AND is_deleted = 0")
    LiveData<List<TaskEntity>> getTasksInRange(int userId, Date startDate, Date endDate);

    @Query("SELECT * FROM tasks WHERE local_id = :localId")
    TaskEntity getTaskByLocalId(int localId);

    @Query("SELECT * FROM tasks WHERE server_id = :serverId AND user_id = :userId")
    TaskEntity getTaskByServerId(int serverId, int userId);

    // Для синхронизации
    @Query("SELECT * FROM tasks WHERE sync_status = 'pending' AND is_deleted = 0")
    List<TaskEntity> getPendingSyncTasks();

    @Query("SELECT * FROM tasks WHERE sync_status = 'failed'")
    List<TaskEntity> getFailedSyncTasks();

    @Query("SELECT * FROM tasks WHERE is_deleted = 1 AND sync_status != 'deleted'")
    List<TaskEntity> getDeletedTasksToSync();

    // Для гостевого режима (user_id < 0)
    @Query("SELECT * FROM tasks WHERE user_id < 0 AND is_deleted = 0")
    List<TaskEntity> getGuestTasks();

    // Статистика
    @Query("SELECT COUNT(*) FROM tasks WHERE user_id = :userId AND status = 'completed'")
    LiveData<Integer> getCompletedTaskCount(int userId);

    @Query("SELECT COUNT(*) FROM tasks WHERE user_id = :userId AND status = 'pending'")
    LiveData<Integer> getPendingTaskCount(int userId);

    @Query("SELECT SUM(task_reward) FROM tasks WHERE user_id = :userId AND status = 'completed'")
    LiveData<Integer> getTotalCoinsEarned(int userId);

    // Поиск
    @Query("SELECT * FROM tasks WHERE user_id = :userId AND task_name LIKE '%' || :query || '%' AND is_deleted = 0")
    LiveData<List<TaskEntity>> searchTasks(int userId, String query);

    // Обновление статусов
    @Query("UPDATE tasks SET sync_status = :syncStatus WHERE local_id = :localId")
    void updateSyncStatus(int localId, String syncStatus);

    @Query("UPDATE tasks SET status = :status WHERE local_id = :localId")
    void updateTaskStatus(int localId, String status);

    @Query("UPDATE tasks SET server_id = :serverId, sync_status = 'synced', last_sync_date = :syncDate WHERE local_id = :localId")
    void markAsSynced(int localId, int serverId, Date syncDate);
}