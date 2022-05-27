package com.adityaamolbavadekar.fineauthkit.main

import androidx.appcompat.app.AppCompatActivity
import com.adityaamolbavadekar.fineauthkit.FineAuthKit

internal abstract class BaseAuthorizationActivity : AppCompatActivity() {

    abstract fun setOptedAuthorizations(): Array<Authorization>

    enum class Authorization {
        Google,
        FirebasePhoneAuth,
        Apple,
        Microsoft,
        GooglePlay
    }
}
