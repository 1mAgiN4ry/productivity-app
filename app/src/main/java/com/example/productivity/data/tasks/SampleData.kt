package com.example.productivity.data.tasks

import java.time.LocalDate
import java.time.LocalTime

/**
 * The Task List mockup's example data, with dates relative to today so it stays meaningful.
 * Loaded once into debug builds (see ProductivityApp) to build the UI against.
 */
object SampleData {

    suspend fun load(repo: TaskRepository, today: LocalDate = LocalDate.now()) {
        // Loose tasks
        repo.createTask("Reply to landlord email", dueDate = today)
        repo.createTask("Book dentist appointment", dueDate = today.plusDays(1))
        val parcel = repo.createTask("Return parcel")
        repo.setState(parcel, TaskState.Waiting, waitingNote = "label")
        repo.createTask("Renew bike lock", dueDate = today.plusDays(4))
        val desk = repo.createTask("Post the old desk")
        repo.addDependency(desk, DependencyTarget.OnTask(parcel))
        repo.createTask("Renew TV licence", startDate = today.plusDays(12))

        // App build group
        val app = repo.createGroup("App build")
        val room = repo.createTask("Set up Room database", groupId = app, dueDate = today.plusDays(2))
        repo.createTask("Wire up timer to task link", groupId = app)
        val icon = repo.createTask("Pick an app icon", groupId = app)
        repo.setState(icon, TaskState.Waiting, waitingNote = "feedback")
        val tests = repo.createTask("Write data layer tests", groupId = app, dueDate = today.plusDays(9))
        repo.addDependency(tests, DependencyTarget.OnTask(room))
        val testsTask = checkNotNull(repo.getTask(tests))
        repo.updateTask(testsTask.copy(reminder = Reminder(daysBeforeDue = 0, time = LocalTime.of(9, 0))))
        val release = repo.createTask("Prepare release build", groupId = app)
        repo.addDependency(release, DependencyTarget.OnTask(tests))
        repo.setState(repo.createTask("Initialise project", groupId = app), TaskState.Done)
        repo.setState(repo.createTask("Add navbar", groupId = app), TaskState.Done)

        // Other groups
        val uni = repo.createGroup("Uni")
        repo.createTask("Submit coursework draft", groupId = uni, dueDate = today.plusDays(6))
        val home = repo.createGroup("Home")
        repo.createTask("Fix the bathroom tap", groupId = home)
    }
}
