package com.udacity.project4.locationreminders.savereminder

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceTransitionsJobIntentService
import com.udacity.project4.locationreminders.geofence.GeofenceTransitionsJobIntentService.Companion.REMINDER
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class SaveReminderFragment : BaseFragment() {
    companion object {
        const val GEOFENCE_RADIUS_IN_METERS = 100f
        val GEOFENCE_EXPIRATION_IN_MILLISECONDS: Long = TimeUnit.HOURS.toMillis(1)

        internal const val ACTION_GEOFENCE_EVENT =
            "LocationReminders.action.ACTION_GEOFENCE_EVENT"
    }


    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value

            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value

            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value
            val reminderDataItem =
                ReminderDataItem(title, description, location, latitude, longitude)
            if (_viewModel.validateEnteredData(reminderDataItem)) {
                val intent = Intent()
                intent.putExtra(REMINDER, reminderDataItem)
                GeofenceTransitionsJobIntentService.enqueueWork(requireContext(), intent)
                _viewModel.saveReminder(reminderDataItem)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //if saveinstancestate is null, then the fragment is just starting and wasn't
        //recreated after rotation. So clear the viewmodel livedatas to start afresh
        savedInstanceState ?: _viewModel.onClear()
    }

    //clearing the viewmodel in ondestroy will not preserve state of the edittexts and textview on rotation of the device
    override fun onDestroy() {
        super.onDestroy()
//        _viewModel.onClear()
    }
}