package com.ifmo.necracker.warehouse_app

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.fasterxml.jackson.annotation.JsonProperty
import com.ifmo.necracker.warehouse_app.model.TaskStatus
import com.ifmo.necracker.warehouse_app.model.User
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClientException


/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : AppCompatActivity() {

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private var mAuthTask: UserLoginTask? = null
    private var mEmailView: AutoCompleteTextView? = null
    private var mPasswordView: EditText? = null
    private var mProgressView: View? = null
    private var mLoginFormView: View? = null
    private var restTemplate = com.ifmo.necracker.warehouse_app.restTemplate.restTemplate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        // Set up the login form.
        mEmailView = findViewById(R.id.username) as AutoCompleteTextView
        mPasswordView = findViewById(R.id.password) as EditText
        mPasswordView?.setOnEditorActionListener(TextView.OnEditorActionListener { textView, id, keyEvent ->
            if (id == R.id.login || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })

        val mEmailSignInButton = findViewById(R.id.email_sign_in_button) as Button
        mEmailSignInButton.setOnClickListener { attemptLogin() }

        mLoginFormView = findViewById(R.id.login_form)
        mProgressView = findViewById(R.id.login_progress)
        if (savedInstanceState != null) {
            mAuthTask = lastCustomNonConfigurationInstance?.let { it as UserLoginTask }
            mAuthTask?.let {
                mAuthTask!!.loginActivity = this
                if (mAuthTask!!.status == TaskStatus.PROCESSING) {
                    showProgress(true)
                }
            }
        }
    }

    override fun onRetainCustomNonConfigurationInstance(): Any? {
        return mAuthTask
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        mAuthTask?.let {
            mAuthTask!!.loginActivity = this
            if (mAuthTask!!.status == TaskStatus.PROCESSING) {
                showProgress(true)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
    }

    /* Note: Methods and definitions below are only used to provide the UI for this sample and are
    not relevant for the execution of the runtime permissions API. */


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */

    data class UnknownUser(@JsonProperty("login") var login: String, @JsonProperty("password") val password: String)

    private fun attemptLogin() {
        if (mAuthTask != null) {
            return
        }

        // Reset errors.
        mEmailView?.error = null
        mPasswordView?.error = null

        // Store values at the time of the login attempt.
        val userName = mEmailView!!.text.toString()
        val password = mPasswordView!!.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid password, if the user entered one.
        if (!isPasswordValid(password)) {
            mPasswordView?.error = getString(R.string.error_invalid_password)
            focusView = mPasswordView
            cancel = true
        }


        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView!!.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true)
            mAuthTask = UserLoginTask(userName, password, this)
            mAuthTask?.execute(null)
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 4
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private fun showProgress(show: Boolean) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime)

            mLoginFormView!!.visibility = if (show) View.GONE else View.VISIBLE
            mLoginFormView!!.animate().setDuration(shortAnimTime.toLong()).alpha(
                    (if (show) 0 else 1).toFloat()).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    mLoginFormView!!.visibility = if (show) View.GONE else View.VISIBLE
                }
            })

            mProgressView!!.visibility = if (show) View.VISIBLE else View.GONE
            mProgressView!!.animate().setDuration(shortAnimTime.toLong()).alpha(
                    (if (show) 1 else 0).toFloat()).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    mProgressView!!.visibility = if (show) View.VISIBLE else View.GONE
                }
            })
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView?.visibility = if (show) View.VISIBLE else View.GONE
            mLoginFormView?.visibility = if (show) View.GONE else View.VISIBLE
        }
    }


    private fun loggedSuccess(user: com.ifmo.necracker.warehouse_app.model.User) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("user", user)
        startActivity(intent)
    }


    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    fun requestPremission() {
        ActivityCompat.requestPermissions(this, Array(1, { Manifest.permission.INTERNET }), 101)
    }

    inner class UserLoginTask internal constructor(private val login: String, private val mPassword: String, var loginActivity: LoginActivity) : AsyncTask<Void, Void, Boolean>() {

        private var response: ResponseEntity<Int>? = null
        private var error = ""
        var status = TaskStatus.NONE

        override fun doInBackground(vararg params: Void): Boolean? {

            requestPremission()

            status = TaskStatus.PROCESSING

            for (attempt in 1..MAX_ATTEMPTS_COUNT) {
                try {
                    response = restTemplate.getForEntity("$serverAddress/user_existence/$login/$mPassword", Int::class.java)
                    break
                } catch (e: HttpStatusCodeException) {
                    if (e.statusCode == HttpStatus.INTERNAL_SERVER_ERROR) {
                        error = "Internal server error"
                        return false
                    }
                    break
                } catch (e: RestClientException) {
                    if (attempt == MAX_ATTEMPTS_COUNT) {
                        error = "Unable to connect to server"
                        return false
                    }
                }
            }
            if (response == null || response!!.statusCode != HttpStatus.OK) {
                for (attempt in 1..MAX_ATTEMPTS_COUNT) {
                    try {
                        response = restTemplate.postForEntity("$serverAddress/new_user", UnknownUser(login, mPassword), Int::class.java)
                        break
                    } catch (e: HttpStatusCodeException) {
                        if (e.statusCode == HttpStatus.INTERNAL_SERVER_ERROR) {
                            error = "Internal server error"
                            return false
                        }
                        error = "Incorrect password"
                        return false
                    } catch (e: RestClientException) {
                        if (attempt == MAX_ATTEMPTS_COUNT) {
                            error = "Unable to connect to server"
                            return false
                        }
                    }
                }
            }
            return true
        }

        override fun onPostExecute(success: Boolean?) {
            status = TaskStatus.READY
            loginActivity.mAuthTask = null
            loginActivity.showProgress(false)

            if (success!!) {
                loginActivity.loggedSuccess(User(response!!.body, login, mPassword))
                finish()
            } else {
                loginActivity.mPasswordView!!.error = error
                loginActivity.mPasswordView!!.requestFocus()
            }
        }

        override fun onCancelled() {
            mAuthTask = null
            loginActivity.showProgress(false)
        }
    }


}

