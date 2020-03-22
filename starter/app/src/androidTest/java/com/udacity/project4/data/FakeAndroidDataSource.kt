package com.udacity.project4.data

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeAndroidDataSource(private var reminders: MutableList<ReminderDTO>? = mutableListOf()) :
    ReminderDataSource {


    override suspend fun getReminders(): Result<List<ReminderDTO>> {

        if (!shouldReturnError) {
            reminders?.let { return Result.Success(it) }
            return Result.Error(
                "Reminders not found"
            )
        } else {
            return Result.Error(
                "Reminders not found"
            )
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        reminders?.forEach {
            if (it.id == id) return Result.Success(it)
        }
        return Result.Error("Reminder with given id not found")
    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()

//        observableTasks.value = getReminders()

    }

    private var shouldReturnError = false

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }


}