package com.example.productivity.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.productivity.data.tasks.GroupDependency
import com.example.productivity.data.tasks.GroupEntity
import com.example.productivity.data.tasks.TaskDao
import com.example.productivity.data.tasks.TaskDependency
import com.example.productivity.data.tasks.TaskEntity

@Database(
    entities = [
        TaskEntity::class,
        GroupEntity::class,
        TaskDependency::class,
        GroupDependency::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "productivity.db").build()
    }
}
