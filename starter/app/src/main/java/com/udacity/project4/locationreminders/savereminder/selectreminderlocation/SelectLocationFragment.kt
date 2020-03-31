package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.TargetApi
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
private const val TAG = "SelectLocationFragment"

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {
    private var selectedLatLng: LatLng? = null
    private var selectedPOI: PointOfInterest? = null

    //boolean to prevent the map object from moving the camera to home everytime the onlocationcallback object is invoked
    private var cameraHasBeenMovedToHome: Boolean = false

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val zoomLevel = 15f
    private var snackbar: Snackbar? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)
        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
        observeSelectLocationLiveData()

        return binding.root
    }

    private fun observeSelectLocationLiveData() {
        _viewModel.showSnackBarIntWithAnAction.observe(
            viewLifecycleOwner,
            Observer {
                snackbar = Snackbar.make(this.view!!, getString(it), Snackbar.LENGTH_INDEFINITE)
                snackbar?.setAction("Save") {
                    onLocationSelected()
                }
                snackbar?.show()
            }
        )
    }

    override fun onStart() {
        super.onStart()
        checkPermissionsAndShowLocation()
    }

    private fun onLocationSelected() {
        //either selectedPOI or selectedLatTng would be null
        selectedPOI?.let {
            _viewModel.selectedPOI.value = it
            _viewModel.reminderSelectedLocationStr.value = it.name
            _viewModel.latitude.value = it.latLng.latitude
            _viewModel.longitude.value = it.latLng.longitude
        }
        selectedLatLng?.let {
            _viewModel.reminderSelectedLocationStr.value = getString(R.string.anonymous_location)
            _viewModel.latitude.value = it.latitude
            _viewModel.longitude.value = it.longitude

        }
        _viewModel.navigationCommand.value =
            NavigationCommand.Back
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)


        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {

            checkDeviceLocationSettingsAndShowLocation(false)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        ) {

            // Permission denied.

            snackbar = Snackbar.make(
                binding.selectLocationParentLayout,
                R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE
            )
            snackbar?.setAction(R.string.settings) {
                // Displays App settings screen.
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
            snackbar?.show()

        } else {
            Log.d("aaaaaaaaaaaaa", "2")

            checkDeviceLocationSettingsAndShowLocation()
            Log.d("aaaaaaaaaaaaa", "3")

        }
    }

    private fun checkPermissionsAndShowLocation() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndShowLocation()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    /*
     *  Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn on location services within our app.
     */
    private fun checkDeviceLocationSettingsAndShowLocation(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(requireContext())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error geting location settings resolution: " + sendEx.message)
                }
            } else {
                snackbar = Snackbar.make(
                    binding.selectLocationParentLayout,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                )
                snackbar?.setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndShowLocation()
                }
                snackbar?.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                updateLocationUI()
                getDeviceLocation()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        //dismisses any snackbar being shown when the fragment is no longer visible
        snackbar?.dismiss()
    }

    /*
     *  Determines whether the app has the appropriate permissions across Android 10+ and all other
     *  Android versions.
     */
    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        checkSelfPermission(
                            requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    /*
     *  Requests ACCESS_FINE_LOCATION and (on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.
     */
    @TargetApi(29)
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return

        // Else request the permission
        // this provides the result[LOCATION_PERMISSION_INDEX]
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        val resultCode = when {
            runningQOrLater -> {
                // this provides the result[BACKGROUND_LOCATION_PERMISSION_INDEX]
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }

        Log.d(TAG, "Request foreground only location permission")
        requestPermissions(
            permissionsArray,
            resultCode
        )
    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setMapLongClick(map)
        setPoiClick(map)
        setMapStyle(map)
    }


    private fun updateLocationUI() {

        try {
            if (foregroundAndBackgroundLocationPermissionApproved()) {
                map.isMyLocationEnabled = true
                map.uiSettings.isMyLocationButtonEnabled = true
            } else {
                map.isMyLocationEnabled = false
                map.uiSettings.isMyLocationButtonEnabled = false
                requestForegroundAndBackgroundLocationPermissions()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message ?: "security exception")
        }
    }


    private fun getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (foregroundAndBackgroundLocationPermissionApproved()) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        if (task.result == null) {
                            val locationCallback = object : LocationCallback() {
                                override fun onLocationResult(locationResult: LocationResult?) {
                                    if (locationResult != null && locationResult.locations.isNotEmpty()) {
                                        //if statement prevents the map from focusing on home everytime
                                        //a location update is made
                                        if (!cameraHasBeenMovedToHome) {
                                            map.moveCamera(
                                                CameraUpdateFactory.newLatLngZoom(
                                                    LatLng(
                                                        locationResult.lastLocation.latitude,
                                                        locationResult.lastLocation.longitude
                                                    ), zoomLevel
                                                )
                                            )
                                            cameraHasBeenMovedToHome = true
                                            _viewModel.showSnackBarInt.value =
                                                R.string.select_location

                                        }
                                    }
                                }
                            }
                            //if  locationresult is null when location settings is just selected, set a callback that moves the
                            //map to current location once the locationresult object is no longer null
                            fusedLocationProviderClient.requestLocationUpdates(
                                getLocationRequest(),
                                locationCallback,
                                null
                            )
                        } else {
                            // Set the map's camera position to the current location of the device.
                            task.result?.let {
                                map.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(
                                            it.latitude,
                                            it.longitude
                                        ), zoomLevel
                                    )
                                )
                                cameraHasBeenMovedToHome = true
                                _viewModel.showSnackBarInt.value = R.string.select_location
                            }
                        }
                    } else {
                        Log.d(TAG, "Current location is null.")
                        Log.e(TAG, "Exception: %s", task.exception)
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message ?: "security exception")
        }
    }

    private fun getLocationRequest(): LocationRequest? {
        val locationRequest = LocationRequest()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        return locationRequest
    }

    // Called when user makes a long press gesture on the map.
    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            selectedPOI = null
            // A Snippet is Additional text that's displayed below the title.
            selectedLatLng = latLng
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )
            map.clear()
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
            _viewModel.showSnackBarIntWithAnAction.value =
                R.string.are_you_sure_you_want_to_receive_a_reminder_at_this_location

        }
    }

    // Places a marker on the map and displays an info window that contains POI name.
    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            selectedLatLng = null
            selectedPOI = poi
            map.clear()
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)

            )
            poiMarker.showInfoWindow()

            _viewModel.showSnackBarIntWithAnAction.value =
                R.string.are_you_sure_you_want_to_receive_a_reminder_at_this_location

        }
    }

    // Allows map styling and theming to be customized.
    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )

            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }
}
