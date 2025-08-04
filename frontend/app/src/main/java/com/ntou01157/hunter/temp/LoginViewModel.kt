package com.ntou01157.hunter.temp

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseUser
import com.ntou01157.hunter.temp.GoogleSignInHelper

class LoginViewModel : ViewModel() {

    fun getSignInClient(activity: Activity): GoogleSignInClient {
        return GoogleSignInHelper.getClient(activity)
    }

    fun handleSignInResult(
        data: Intent?,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        GoogleSignInHelper.handleResult(
            data,
            onSuccess = { user: FirebaseUser ->
                onSuccess(user.email ?: "未知信箱")
            },
            onFailure = onFailure
        )
    }
}
