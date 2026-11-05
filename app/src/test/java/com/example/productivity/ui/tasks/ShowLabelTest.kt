package com.example.productivity.ui.tasks

import org.junit.Assert.assertEquals
import org.junit.Test

class ShowLabelTest {

    @Test
    fun `counts tasks, and group rows when there are any`() {
        assertEquals("Show 1 task", showLabel(FilterPreview(1, 0)))
        assertEquals("Show 4 tasks", showLabel(FilterPreview(4, 0)))
        assertEquals("Show 0 tasks & 1 group", showLabel(FilterPreview(0, 1)))
        assertEquals("Show 2 tasks & 3 groups", showLabel(FilterPreview(2, 3)))
    }
}
