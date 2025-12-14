package com.example.app.data.local;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import android.content.Context;

import com.example.app.data.local.converters.DateConverter;
import com.example.app.data.local.dao.TaskDao;
import com.example.app.data.local.dao.UserDao;
import com.example.app.data.local.entities.TaskEntity;
import com.example.app.data.local.entities.UserEntity;

@Database(
        entities = {
                TaskEntity.class,
                UserEntity.class,
        },
        version = 2,
        exportSchema = false
)
@TypeConverters({DateConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract TaskDao taskDao();
    public abstract UserDao userDao();

    private static volatile AppDatabase INSTANCE;

    // Singleton для доступа к БД
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "task_tracker.db"
                            )
                            .fallbackToDestructiveMigration() // При изменении версии удаляет старую
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // Метод для закрытия БД
    public static void closeDatabase() {
        if (INSTANCE != null && INSTANCE.isOpen()) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}