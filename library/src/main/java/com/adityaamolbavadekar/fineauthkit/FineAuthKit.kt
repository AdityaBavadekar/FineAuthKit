package com.adityaamolbavadekar.fineauthkit

import android.app.Activity
import android.content.Intent
import android.text.TextUtils
import android.util.Patterns
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task

object FineAuthKit {

    class GoogleSignInKit(
        private val c: FragmentActivity,
        private val listener: OnGoogleSignInResult,
        googleSignInOptions: GoogleSignInOptions? = null
    ) {

        private var gso: GoogleSignInOptions = googleSignInOptions
            ?: GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()

        private var googleSignInClient: GoogleSignInClient
        private var googleSignInAccount: GoogleSignInAccount? = null

        private var intentLauncher: ActivityResultLauncher<Intent> =
            c.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult: ActivityResult ->

                activityResult.apply {
                    if (activityResult.resultCode == Activity.RESULT_OK) {
                        if (activityResult.data == null) {
                            listener.onNullAccount("The result code was -1 but data returned was null")
                        } else {
                            getSignInAccountFromIntent(data!!)
                        }
                    } else {
                        listener.onIntentFailure(resultCode)
                    }
                }

            }

        fun getSignInAccountFromIntent(data: Intent) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleResult(task)
        }

        fun handleResult(task: Task<GoogleSignInAccount>) {
            task.addOnSuccessListener {
                googleSignInAccount = it
                listener.onSuccess(it)
            }
            task.addOnFailureListener {
                listener.onTaskFailure(it)
            }
            task.addOnCanceledListener {
                listener.onTaskFailure(Exceptions.UserCanceledException())
            }
        }

        fun getSignedAccount(): GoogleSignInAccount? {
            return googleSignInAccount
        }

        init {
            googleSignInClient = GoogleSignIn.getClient(c, gso)
        }

        fun startSignInFlow() {
            val i = googleSignInClient.signInIntent
            intentLauncher.launch(i)
        }

        fun getIsSignedIn(): Boolean {
            googleSignInAccount = GoogleSignIn.getLastSignedInAccount(c)
            return (googleSignInAccount == null)
        }

        fun signOut() {
            googleSignInClient.signOut()
                .addOnSuccessListener {
                    listener.onSignedOut(googleSignInAccount?.id)
                    googleSignInAccount = null
                }
                .addOnFailureListener {
                    listener.onSignedOutFailed(it)
                }
                .addOnCanceledListener {
                    listener.onSignedOutFailed(Exceptions.UserCanceledException())
                }
        }

    }

    class GoogleSignInNewKit(
        private val c: FragmentActivity,
        private val listener: OnGoogleSignInResult,
        private val serverClientId: String,
    ) {

        private var googleSignInCredential: SignInCredential? = null

        private var intentLauncher: ActivityResultLauncher<IntentSenderRequest> =
            c.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult: ActivityResult ->

                activityResult.apply {
                    if (activityResult.resultCode == Activity.RESULT_OK) {
                        if (activityResult.data == null) {
                            googleSignInCredential = null
                            listener.onNullAccount("The result code was -1 but data returned was null")
                        } else {
                            try {
                                val cred =
                                    Identity.getSignInClient(c)
                                        .getSignInCredentialFromIntent(data!!)
                                googleSignInCredential = cred
                                listener.onSuccess(
                                    cred
                                )
                            } catch (e: Exception) {
                                googleSignInCredential = null
                                listener.onTaskFailure(e)
                            }
                        }
                    } else {
                        listener.onIntentFailure(resultCode)
                    }
                }

            }

        fun startSignInFlow() {
            val request = GetSignInIntentRequest.builder()
                .setServerClientId(serverClientId)
                .build()

            Identity.getSignInClient(c)
                .getSignInIntent(request)
                .addOnSuccessListener {
                    try {
                        intentLauncher.launch(IntentSenderRequest.Builder(it.intentSender).build())
                    } catch (e: Exception) {
                        googleSignInCredential = null
                        listener.onTaskFailure(e)
                    }
                }
                .addOnFailureListener {
                    googleSignInCredential = null
                    listener.onTaskFailure(it)
                }
                .addOnCanceledListener {
                    googleSignInCredential = null
                    listener.onTaskFailure(Exceptions.UserCanceledException())
                }
        }

        fun getSignInCredential(): SignInCredential? {
            return googleSignInCredential
        }

        fun signOut() {
            Identity.getSignInClient(c).signOut()
                .addOnFailureListener {
                    listener.onSignedOutFailed(it)
                }
                .addOnSuccessListener {
                    listener.onSignedOut(googleSignInCredential?.id)
                }
                .addOnCanceledListener {
                    listener.onSignedOutFailed(Exceptions.UserCanceledException())
                }
        }
    }

    class GooglePhoneHintKit(
        private val c: FragmentActivity,
        private val listener: OnGooglePhoneHintResult,
        private val limitAttemptsTo: Int = 1
    ) {

        private var phoneResult: PhoneResult? = null
        private var limitAttemptsToCountDown = limitAttemptsTo

        private val phoneNumberHintIntentResultLauncher =
            c.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                result.data?.let { intent ->
                    try {
                        val number =
                            Identity.getSignInClient(c).getPhoneNumberFromIntent(intent)
                        val reversed = number.reversed()
                        val phone = reversed.slice(0..9).reversed()
                        val prefix = reversed.removeRange(0..9).reversed()
                        phoneResult = PhoneResult(number, phone, prefix)
                        listener.onSuccess(phoneResult!!)
                    } catch (e: Exception) {
                        phoneResult = null
                        listener.onFailure(e)
                    }
                }
            }

        fun requestPhone() {
            if (limitAttemptsToCountDown > 0) {
                val request = GetPhoneNumberHintIntentRequest.builder().build()
                Identity.getSignInClient(c)
                    .getPhoneNumberHintIntent(request)
                    .addOnSuccessListener {
                        try {
                            phoneNumberHintIntentResultLauncher.launch(
                                IntentSenderRequest.Builder(it.intentSender).build()
                            )
                        } catch (e: Exception) {
                            phoneResult = null
                            listener.onFailure(e)
                        }
                    }
                    .addOnFailureListener {
                        phoneResult = null
                        listener.onFailure(it)
                    }
                limitAttemptsToCountDown -= 1
            } else {
                listener.onFailure(MaxAttemptsLimitReachedException())
            }
        }

        inner class MaxAttemptsLimitReachedException :
            Exception("Maximum attempts (${limitAttemptsTo}) to open hint picker has reached")
    }

    interface OnGoogleSignInResult {
        fun onSuccess(account: GoogleSignInAccount)
        fun onSuccess(cred: SignInCredential)
        fun onNullAccount(cause: String)
        fun onIntentFailure(resultCode: Int)
        fun onTaskFailure(exception: Exception)
        fun onSignedOut(previousAccountId: String?)
        fun onSignedOutFailed(exception: Exception)
    }

    data class PhoneResult(
        var completePhoneNumber: String,
        var phone: String,
        var prefix: String
    )

    interface OnGooglePhoneHintResult {
        fun onSuccess(phoneResult: PhoneResult)
        fun onFailure(e: Exception)
    }

    object Verifier : VerifierIntr {

        override fun validateEmailAddress(email: String): Exception? {
            return when {
                isValidEmailAddress(email) -> {
                    null
                }
                email.trim().isEmpty() -> {
                    Exceptions.EmptyStringException()
                }
                else -> {
                    Exceptions.InvalidEmailAddressException()
                }
            }
        }

        override fun validatePhone(phone: String): Exception? {
            return when {
                isValidPhone(phone) -> null
                phone.trim().isEmpty() -> {
                    Exceptions.EmptyStringException()
                }
                else -> {
                    Exceptions.InvalidEmailAddressException()
                }
            }
        }

        override fun validatePassword(password: String, minChars: Int): Exception? {
            return when {
                isValidPassword(password) -> null
                password.trim().isEmpty() -> {
                    Exceptions.EmptyStringException()
                }
                else -> Exceptions.WeakPasswordException(minChars)
            }
        }

        override fun validatePasswords(password: String, validateWithPassword: String): Exception? {
            return if (password.trim()
                    .isNotEmpty() && password.trim() == validateWithPassword.trim()
            ) {
                null
            } else if (password.trim().isEmpty()) {
                Exceptions.EmptyStringException()
            } else Exceptions.PasswordsDontMatchException()
        }

        override fun validateOTP(enteredOTP: String, validOTP: String): Exception? {
            return when {
                isValidOTP(enteredOTP, validOTP) -> {
                    null
                }
                enteredOTP.trim().isEmpty() -> {
                    Exceptions.EmptyStringException()
                }
                else -> {
                    Exceptions.InvalidOTPException()
                }
            }
        }

        override fun isValidEmailAddress(email: String): Boolean {
            return email.trim().isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
        }

        override fun isValidPhone(phone: String): Boolean {
            return phone.trim().isNotEmpty() && Patterns.PHONE.matcher(phone).matches()
        }

        override fun isValidPassword(password: String, minChars: Int): Boolean {
            return password.trim().isNotEmpty() && password.length >= minChars
        }

        override fun isValidOTP(enteredOTP: String, validOTP: String): Boolean {
            return enteredOTP.trim()
                .isNotEmpty() && enteredOTP.length == 6 && TextUtils.isDigitsOnly(
                enteredOTP
            ) && enteredOTP == validOTP
        }
    }

    internal interface VerifierIntr {
        fun validateEmailAddress(email: String): Exception?
        fun validatePhone(phone: String): Exception?
        fun validatePassword(password: String, minChars: Int = 8): Exception?
        fun validatePasswords(password: String, validateWithPassword: String): Exception?
        fun validateOTP(enteredOTP: String, validOTP: String): Exception?
        fun isValidEmailAddress(email: String): Boolean
        fun isValidPhone(phone: String): Boolean
        fun isValidPassword(password: String, minChars: Int = 8): Boolean
        fun isValidOTP(enteredOTP: String, validOTP: String): Boolean
    }

    class Exceptions {
        class UserCanceledException : Exception("User canceled the flow")
        class InvalidPhoneNumberException : Exception("The provided phone number is invalid")
        class WeakPasswordException(minChars: Int) :
            Exception("The provided password is too weak minimum of (${minChars}) characters is required")

        class PasswordsDontMatchException : Exception("The provided passwords donot match")
        class EmptyStringException : Exception("The provided string is empty")
        class InvalidEmailAddressException :
            Exception("The provided email address is badly formatted")

        class InvalidOTPException :
            Exception("The provided OTP is incorrect/invalid")
    }

    class FirebaseSignIn {

    }

}