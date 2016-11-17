package com.ifmo.necracker.warehouse_app

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.support.v7.app.AppCompatActivity
import android.app.LoaderManager.LoaderCallbacks

import android.content.CursorLoader
import android.content.Intent
import android.content.Loader
import android.database.Cursor
import android.net.Uri
import android.os.AsyncTask

import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.support.v4.app.ActivityCompat
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.web.client.RestTemplate

import java.util.ArrayList
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter
import android.widget.*
import org.springframework.web.client.RestClientException


/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : AppCompatActivity(), LoaderCallbacks<Cursor> {
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private var mAuthTask: UserLoginTask? = null

    // UI references.
    private var mEmailView: AutoCompleteTextView? = null
    private var mPasswordView: EditText? = null
    private var mProgressView: View? = null
    private var mLoginFormView: View? = null
    var restTemplate: RestTemplate = RestTemplate()
    private val serverAddress = "http://10.0.0.105:1487/mh/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        // Set up the login form.
        mEmailView = findViewById(R.id.email) as AutoCompleteTextView
        restTemplate.messageConverters.add(MappingJacksonHttpMessageConverter())
        mPasswordView = findViewById(R.id.password) as EditText
        mPasswordView!!.setOnEditorActionListener(TextView.OnEditorActionListener { textView, id, keyEvent ->
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

    data class User(@JsonProperty("login") var login: String, @JsonProperty("password") val password: String) {

        override fun toString(): String {
            return String.format("User [ Login = %s, password = %s]", login, password)
        }
    }

    private fun attemptLogin() {
        if (mAuthTask != null) {
            return
        }

        // Reset errors.
        mEmailView!!.error = null
        mPasswordView!!.error = null

        // Store values at the time of the login attempt.
        val email = mEmailView!!.text.toString()
        val password = mPasswordView!!.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView!!.error = getString(R.string.error_invalid_password)
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
            mAuthTask = UserLoginTask(email, password)
            mAuthTask!!.execute(null)
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        //TODO: Replace this with your own logic
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
            mProgressView!!.visibility = if (show) View.VISIBLE else View.GONE
            mLoginFormView!!.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    override fun onCreateLoader(i: Int, bundle: Bundle): Loader<Cursor> {
        return CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE + " = ?", arrayOf(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE),

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC")
    }

    override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor) {
        val emails = ArrayList<String>()
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS))
            cursor.moveToNext()
        }

    }

    override fun onLoaderReset(cursorLoader: Loader<Cursor>) {

    }


    private interface ProfileQuery {
        companion object {
            val PROJECTION = arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS, ContactsContract.CommonDataKinds.Email.IS_PRIMARY)

            val ADDRESS = 0
            val IS_PRIMARY = 1
        }
    }


    protected fun loggedSuccess(user: com.ifmo.necracker.warehouse_app.model.User) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("user", user)
        startActivity(intent)
    }

    fun makeToast(text: String) {
        val toast = Toast.makeText(this, text, Toast.LENGTH_LONG)
        toast.show()
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    fun requestPremission() {
        val prem = ActivityCompat.requestPermissions(this, Array(1, { Manifest.permission.INTERNET }), 101)
    }

    inner class UserLoginTask internal constructor(private val login: String, private val mPassword: String) : AsyncTask<Void, Void, Boolean>() {
        private var userId = -1
        private var error = ""
        override fun doInBackground(vararg params: Void): Boolean? {
            // TODO: attempt authentication against a network service.

            requestPremission()

            println(login)
            println(mPassword)
            try {
                userId = restTemplate.postForObject(serverAddress + "user/", User(login, mPassword), Int::class.java)
            } catch (e: RestClientException) {
                //makeToast("Error during authification")
                error = "Unable to connect to server"
                return false
            }

            println(userId)
            if (userId == -1) {
                error = "Fail to register"
                return false
            }
            /*try {
                // Simulate network access.
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
                return false
            }*/

            /*for (credential in DUMMY_CREDENTIALS) {
                val pieces = credential.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (pieces[0] == login) {
                    // Account exists, return true if the password matches.
                    return pieces[1] == mPassword
                }
            }*/

            // TODO: register the new account here.
            return true
        }

        override fun onPostExecute(success: Boolean?) {
            mAuthTask = null
            showProgress(false)

            if (success!!) {
                loggedSuccess(com.ifmo.necracker.warehouse_app.model.User(userId, login, mPassword))
                finish()
            } else {
                mPasswordView!!.error = error
                mPasswordView!!.requestFocus()
            }
        }

        override fun onCancelled() {
            mAuthTask = null
            showProgress(false)
        }
    }


}

