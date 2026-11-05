package com.example.productivity.data.tasks

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskFilterStoreTest {

    // A file of its own, so the app's saved filter is never touched.
    private val prefs = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("task_filter_store_test", Context.MODE_PRIVATE)

    @After
    fun tearDown() {
        prefs.edit().clear().commit()
    }

    @Test
    fun theFilterIsRememberedByTheNextStore() {
        val chosen = TaskFilter(grouping = Grouping.All, states = setOf(StateFilter.Done), sort = SortOrder.Created)
        TaskFilterStore(prefs).set(chosen.copy(showHidden = true, groupId = 5))

        // As after an app restart: the eye and group aren't part of what's remembered.
        assertEquals(chosen, TaskFilterStore(prefs).filter.value)
    }
}
