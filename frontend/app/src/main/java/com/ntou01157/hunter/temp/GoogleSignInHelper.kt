package com.ntou01157.hunter.temp

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.*
import com.google.firebase.auth.*
import com.google.android.gms.common.api.ApiException

object GoogleSignInHelper {

    fun getClient(activity: Activity): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(com.ntou01157.hunter.R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(activity, gso)
    }

    fun handleResult(
        data: Intent?,
        onSuccess: (FirebaseUser) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener { authResult ->
                    if (authResult.isSuccessful) {
                        onSuccess(FirebaseAuth.getInstance().currentUser!!)
                    } else {
                        onFailure("Firebase 驗證失敗")
                    }
                }
        } catch (e: Exception) {
            onFailure("Google 登入失敗: ${e.message}")
        }
    }
}
