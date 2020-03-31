package com.udacity.project4.authentication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.ActivityNavigator
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity
import kotlinx.android.synthetic.main.activity_authentication.*
import org.koin.android.ext.android.inject

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */

class AuthenticationActivity : AppCompatActivity() {
    companion object {
        const val SIGN_IN_RESULT_CODE = 1001
        var hasJustLoggedOut: Boolean = false

    }

    val viewModel: AuthenticationViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_authentication)

        enableLogInBasedOnNetworkConnection()


    }

    private fun enableLogInBasedOnNetworkConnection() {
        //check if user is connected
        if (getConnectionType(this) != 0) {

            observeAuthenticationViewModel()
        } else {
            Toast.makeText(
                this,
                getString(R.string.network_required_to_signin),
                Toast.LENGTH_LONG
            ).show()
            makeSignInButtonVisible()

            //            finish()
        }
    }


    private fun observeAuthenticationViewModel() {

        viewModel.authenticationState.observe(this, Observer { authenticationState ->
            when (authenticationState) {
                AuthenticationViewModel.AuthenticationState.AUTHENTICATED -> {

                    if (!hasJustLoggedOut) {
                        navigateToRemindersActivityAndFinishThisActivity()
                        viewModel.authenticationState.removeObservers(this)
                    }
                    hasJustLoggedOut = false

                }
                else -> {

                    launchSignInFlow()
                    viewModel.authenticationState.removeObservers(this)
                }
            }

        })
    }

    private fun navigateToRemindersActivityAndFinishThisActivity() {
        val activityNavigator = ActivityNavigator(this)
        activityNavigator.navigate(
            activityNavigator.createDestination().setIntent(
                Intent(
                    this,
                    RemindersActivity::class.java
                )
            ), null, null, null
        )
        finish()
    }

    private fun launchSignInFlow() {
        // Give users the option to sign in / register with their email or Google account. If users
        // choose to register with their email, they will need to create a password as well.
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build()
        )

        // Create and launch sign-in intent. We listen to the response of this activity with the
        // SIGN_IN_RESULT_CODE code.
        startActivityForResult(
            AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(
                providers
            )
                .build(), SIGN_IN_RESULT_CODE
        )
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SIGN_IN_RESULT_CODE) {
            IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in user.
                navigateToRemindersActivityAndFinishThisActivity()

            } else {
                // Sign in failed. If response is null the user canceled the sign-in flow using
                // the back button. Otherwise check response.getError().getErrorCode() and handle
                // the error.
                makeSignInButtonVisible()
                Toast.makeText(
                    this,
                    getString(R.string.sihn_in_unsuccessful),
                    Toast.LENGTH_LONG
                ).show()

            }
        }
    }

    private fun makeSignInButtonVisible() {
        login_button.visibility = View.VISIBLE
        login_button.setOnClickListener {
            enableLogInBasedOnNetworkConnection()
        }
    }


    // convenient method to detect whether there is connection
//   @return 0: No Internet available (maybe on airplane mode, or in the process of joining an wi-fi).
//
//   @return 1: Cellular (mobile data, 3G/4G/LTE whatever).
//
//    @return 2: Wi-fi.
    private fun getConnectionType(context: Context): Int {
        var result = 0 // Returns connection type. 0: none; 1: mobile data; 2: wifi
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm?.run {
                cm.getNetworkCapabilities(cm.activeNetwork)?.run {
                    if (hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        result = 2
                    } else if (hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        result = 1
                    }
                }
            }
        } else {
            cm?.run {
                cm.activeNetworkInfo?.run {
                    if (type == ConnectivityManager.TYPE_WIFI) {
                        result = 2
                    } else if (type == ConnectivityManager.TYPE_MOBILE) {
                        result = 1
                    }
                }
            }
        }
        return result
    }

}
