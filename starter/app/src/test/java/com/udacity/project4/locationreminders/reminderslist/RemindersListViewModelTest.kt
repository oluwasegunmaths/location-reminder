package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.core.Is.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class RemindersListViewModelTest {
    // Subject under test
    private lateinit var remindersListViewModel: RemindersListViewModel

    // Use a fake repository to be injected into the viewmodel
    private lateinit var fakeDataSource: FakeDataSource

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setupViewModel() {
        fakeDataSource = FakeDataSource(mutableListOf(ReminderDTO("test", "", "", 0.0, 0.0)))
        remindersListViewModel = RemindersListViewModel(fakeDataSource)
    }

    @Test
    fun setUpAViewModelWithADataSourceThatHasAReminder_reminderListLiveDataListValueHasFirstElementWithSameTitleAsTheOriginalReminder() {

        remindersListViewModel.loadReminders()

        val value = remindersListViewModel.remindersList.getOrAwaitValue()

        MatcherAssert.assertThat(
            value[0].title,
            `is`("test")
        )

    }

    @Test
    fun shouldReturnError() {
        // Make the repository return errors

        fakeDataSource.setReturnError(true)
        remindersListViewModel.loadReminders()

        // Then an error message is shown
        MatcherAssert.assertThat(
            remindersListViewModel.showErrorMessage.getOrAwaitValue(),
            CoreMatchers.`is`("Reminders not found")
        )
    }

    @Test
    fun check_loading() {
        mainCoroutineRule.pauseDispatcher()

        remindersListViewModel.loadReminders()

        MatcherAssert.assertThat(
            remindersListViewModel.showLoading.getOrAwaitValue(),
            CoreMatchers.`is`(true)
        )

        mainCoroutineRule.resumeDispatcher()

        MatcherAssert.assertThat(
            remindersListViewModel.showLoading.getOrAwaitValue(),
            CoreMatchers.`is`(false)
        )
    }
}