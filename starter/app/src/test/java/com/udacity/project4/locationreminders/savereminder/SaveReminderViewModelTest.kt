package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
//@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {
    // Subject under test
    private lateinit var saveReminderViewModel: SaveReminderViewModel

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
        // We initialise the tasks to 3, with one active and two completed
        fakeDataSource = FakeDataSource()
//        val task1 = Task("Title1", "Description1")
//        val task2 = Task("Title2", "Description2", true)
//        val task3 = Task("Title3", "Description3", true)
//        tasksRepository.addTasks(task1, task2, task3)

        saveReminderViewModel = SaveReminderViewModel(fakeDataSource)
    }

    @Test
    fun check_loading() {

        // Pause dispatcher so we can verify initial values
        mainCoroutineRule.pauseDispatcher()

        saveReminderViewModel.saveReminder(ReminderDataItem("", "", "", 0.0, 0.0))

        MatcherAssert.assertThat(
            saveReminderViewModel.showLoading.getOrAwaitValue(),
            CoreMatchers.`is`(true)
        )

        mainCoroutineRule.resumeDispatcher()

        MatcherAssert.assertThat(
            saveReminderViewModel.showLoading.getOrAwaitValue(),
            CoreMatchers.`is`(false)
        )
    }

    @Test
    fun saveNewReminder_verifyItWasSaved() = runBlockingTest {
        fakeDataSource.deleteAllReminders()
        saveReminderViewModel.saveReminder(ReminderDataItem("save_test", "", "", 0.0, 0.0))

        val value = fakeDataSource.getReminders()
        var listOfReminders: List<ReminderDTO>? = null
        if (value is Result.Success<*>) listOfReminders = value.data as List<ReminderDTO>

        MatcherAssert.assertThat(
            listOfReminders!![0].title,
            CoreMatchers.`is`("save_test")
        )

    }

}