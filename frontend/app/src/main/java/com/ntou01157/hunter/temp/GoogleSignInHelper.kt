package com.ntou01157.hunter.temp

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object TokenManager {
    var idToken: String? = null
}

object GoogleSignInHelper {

    private val client = OkHttpClient()

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
                        val user = FirebaseAuth.getInstance().currentUser!!

                        // ✅ 取得 Firebase ID Token
                        user.getIdToken(true).addOnCompleteListener { tokenResult ->
                            if (tokenResult.isSuccessful) {
                                val idToken = tokenResult.result?.token
                                if (idToken != null) {
                                    // ⬅️ 存到全域 TokenManager
                                    TokenManager.idToken = idToken

                                    // ✅ 同步傳給後端
                                    createUserInBackend(idToken, {
                                        onSuccess(user)
                                    }, { error ->
                                        onFailure("後端錯誤：$error")
                                    })
                                } else {
                                    onFailure("無法取得 ID Token")
                                }
                            } else {
                                onFailure("取得 ID Token 失敗")
                            }
                        }
                    } else {
                        onFailure("Firebase 驗證失敗")
                    }
                }
        } catch (e: Exception) {
            onFailure("Google 登入失敗: ${e.message}")
        }
    }

    private fun createUserInBackend(
        idToken: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val json = JSONObject()
        json.put("idToken", idToken) // ✅ 傳 Token，不是 email

        val requestBody = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://10.0.2.2:4000/api/auth/google")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(e.message ?: "未知錯誤")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Handler(Looper.getMainLooper()).post { onSuccess() }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        onFailure("後端錯誤代碼: ${response.code}")
                    }
                }
            }
        })
    }
}
