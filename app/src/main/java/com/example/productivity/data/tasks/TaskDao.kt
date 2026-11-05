package com.example.productivity.data.tasks

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Raw table access. The app reads whole tables and derives status in Kotlin ([TaskGraph]),
 * which keeps the rules in one testable place; a personal task list is small enough for that.
 */
@Dao
interface TaskDao {

    // Live reads: each re-emits whenever its table changes.

    @Query("SELECT * FROM tasks")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM `groups`")
    fun observeGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM task_depends_on_task")
    fun observeTaskDependencies(): Flow<List<TaskDependency>>

    @Query("SELECT * FROM task_depends_on_group")
    fun observeGroupDependencies(): Flow<List<GroupDependency>>

    // One-shot reads, for validating a change inside a transaction.

    @Query("SELECT * FROM tasks")
    suspend fun getTasks(): List<TaskEntity>

    @Query("SELECT * FROM `groups`")
    suspend fun getGroups(): List<GroupEntity>

    @Query("SELECT * FROM task_depends_on_task")
    suspend fun getTaskDependencies(): List<TaskDependency>

    @Query("SELECT * FROM task_depends_on_group")
    suspend fun getGroupDependencies(): List<GroupDependency>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTask(id: Long): TaskEntity?

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun countTasks(): Int

    @Query("SELECT COUNT(*) FROM `groups`")
    suspend fun countGroups(): Int

    // Writes.

    @Insert
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Insert
    suspend fun insertGroup(group: GroupEntity): Long

    @Query("UPDATE `groups` SET title = :title WHERE id = :id")
    suspend fun renameGroup(id: Long, title: String)

    @Query("DELETE FROM `groups` WHERE id = :id")
    suspend fun deleteGroup(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTaskDependency(dependency: TaskDependency)

    @Delete
    suspend fun deleteTaskDependency(dependency: TaskDependency)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroupDependency(dependency: GroupDependency)

    @Delete
    suspend fun deleteGroupDependency(dependency: GroupDependency)
}
