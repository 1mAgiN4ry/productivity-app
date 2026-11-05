package com.example.productivity

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import com.example.productivity.data.AppDatabase
import com.example.productivity.data.tasks.SampleData
import com.example.productivity.data.tasks.TaskFilterStore
import com.example.productivity.data.tasks.TaskRepository
import com.example.productivity.ui.tasks.TaskUndo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** App-wide singletons, created once per process. */
class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.create(context)
    val taskRepository = TaskRepository(database)
    val taskUndo = TaskUndo()
    val taskFilter = TaskFilterStore(context.getSharedPreferences("tasks", Context.MODE_PRIVATE))

    /**
     * For writes that must finish even if the screen that started them closes (e.g. the last
     * keystroke of a title edit as you tap Back). Screen-scoped coroutines would cancel them.
     */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

class ProductivityApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            loadSampleDataOnce()
        }
    }

    /** Debug builds only: fill the database with the mockup's examples the first time. */
    private fun loadSampleDataOnce() {
        val prefs = getSharedPreferences("debug", MODE_PRIVATE)
        if (prefs.getBoolean(SAMPLE_DATA_LOADED, false)) return
        container.appScope.launch {
            SampleData.load(container.taskRepository)
            prefs.edit().putBoolean(SAMPLE_DATA_LOADED, true).apply()
        }
    }

    private companion object {
        const val SAMPLE_DATA_LOADED = "sample_data_loaded"
    }
}
