package com.mytracker.gpstracker.familytracker.modelview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.mytracker.gpstracker.familytracker.MyNavigationTutorial
import com.mytracker.gpstracker.familytracker.RegisterNameActivity
import timber.log.Timber

class AuthModelView constructor(private val context: Context, private val auth: FirebaseAuth) {

    private lateinit var authInterface: AuthInterface

    fun addListener(authInterface: AuthInterface) {
        this.authInterface = authInterface
    }

    fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(context as Activity) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Timber.d("signInWithCredential:success")
//                        val uri = Uri.parse("android.resource://com.mytracker.gpstracker.familytracker/drawable/defaultprofile")
                        val user: FirebaseUser = task.result!!.user

                        if (user.displayName == null) {
                            context.startActivity(Intent(context, RegisterNameActivity::class.java))
                        } else {
                            context.startActivity(Intent(context, MyNavigationTutorial::class.java))
                        }
                        context.finish()
                        // ...

                    } else {
                        // Sign in failed, display a message and update the UI
                        Timber.d(task.exception, "signInWithCredential:failure")
                        if (task.exception is FirebaseAuthInvalidCredentialsException) {
                            // The verification code entered was invalid
                            authInterface.onException(task.exception as FirebaseAuthInvalidCredentialsException)
                        }
                    }
                }
    }

    interface AuthInterface {
        fun onCompleted(p0: PhoneAuthCredential?)
        fun onFailed(p0: FirebaseException?)
        fun onCodeSent(p0: String?)
        fun onException(p0: FirebaseAuthInvalidCredentialsException)
    }
}