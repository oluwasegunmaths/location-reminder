package com.udacity.project4

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private lateinit var saveViewModel: SaveReminderViewModel

    // An Idling Resource that waits for Data Binding to have no pending bindings
    private val dataBindingIdlingResource = DataBindingIdlingResource()
    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()
        //get a saveviewmodel
        saveViewModel = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    /**
     * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
     * are not scheduled in the main Looper (for example when executed on a different thread).
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    /**
     * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
     */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun moveToSelectLocationFragment_saveReminderAndVerifyItIsDisplayedInRemindersList() =
        runBlocking {

            val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
            dataBindingIdlingResource.monitorActivity(activityScenario)

            onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())

            onView(withId(R.id.reminderTitle))
                .perform(ViewActions.typeText("TITLE"), ViewActions.closeSoftKeyboard())
            onView(withId(R.id.reminderDescription))
                .perform(ViewActions.typeText("DESCRIPTION"), ViewActions.closeSoftKeyboard())

            onView(withId(R.id.selectLocation)).perform(ViewActions.click())


            onView(withId(R.id.map_fragment)).check(matches(isDisplayed()))

            Espresso.pressBack()

            repository.saveReminder(ReminderDTO("TITLE", "DESCRIPTION", "location", 0.0, 0.0))
            Espresso.pressBack()

            onView(withId(R.id.title))
                .check(matches(withText("TITLE")))
            onView(withId(R.id.description))
                .check(matches(withText("DESCRIPTION")))
            onView(withId(R.id.location))
                .check(matches(withText("location")))
            // Make sure the activity is closed before resetting the db:
            activityScenario.close()
        }

    @Test
    fun saveReminderWithTheSaveReminderFAB_verifyItIsShownInRemindersList() {

        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())

        onView(withId(R.id.reminderTitle))
            .perform(ViewActions.typeText("TITLE"), ViewActions.closeSoftKeyboard())
        onView(withId(R.id.reminderDescription))
            .perform(ViewActions.typeText("DESCRIPTION"), ViewActions.closeSoftKeyboard())


        saveViewModel.reminderTitle.postValue("changed title")

        saveViewModel.reminderSelectedLocationStr.postValue("location")
        saveViewModel.latitude.postValue(0.0)
        saveViewModel.longitude.postValue(0.0)

        onView(withId(R.id.reminderTitle))
            .check(matches(withText("changed title")))
        onView(withId(R.id.reminderDescription))
            .check(matches(withText("DESCRIPTION")))
        onView(withId(R.id.selectedLocation))
            .check(matches(withText("location")))


        onView(withId(R.id.saveReminder)).perform(ViewActions.click())


        onView(withId(R.id.title))
            .check(matches(withText("changed title")))
        onView(withId(R.id.description))
            .check(matches(withText("DESCRIPTION")))
        onView(withId(R.id.location))
            .check(matches(withText("location")))
        // Make sure the activity is closed before resetting the db:
        activityScenario.close()
    }

    @Test
    fun saveReminderWithoutAnyDetails_verifySnackBarMessageIsShownInformingUserToEnterTitle() {

        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())


        onView(withId(R.id.saveReminder)).perform(ViewActions.click())
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(appContext.getString(R.string.err_enter_title))))

        // Make sure the activity is closed before resetting the db:
        activityScenario.close()
    }
}
