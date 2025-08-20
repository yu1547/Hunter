package com.ntou01157.hunter.temp

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.*
import com.google.firebase.auth.*
import com.google.android.gms.common.api.ApiException
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import android.os.Handler
import android.os.Looper



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
                        // ✅ 呼叫後端 API 建立 MongoDB 使用者
                        createUserInBackend(user.email ?: "", {
                            onSuccess(user)
                        }, { error ->
                            onFailure("後端錯誤：$error")
                        })
                    } else {
                        onFailure("Firebase 驗證失敗")
                    }
                }
        } catch (e: Exception) {
            onFailure("Google 登入失敗: ${e.message}")
        }
    }

    private fun createUserInBackend(
        email: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        Log.d("GoogleSignInHelper", "準備傳送到後端 email: $email") // <-- 新增這行
        val json = JSONObject()
        json.put("email", email)

        val requestBody = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://10.0.2.2:4000/api/auth/google")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("GoogleSignInHelper", "後端請求失敗: ${e.message}")
                onFailure(e.message ?: "未知錯誤")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Handler(Looper.getMainLooper()).post {
                        onSuccess() // ⬅️ 這個 callback 裡面才呼叫 navController.navigate()
                    }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        onFailure("後端錯誤代碼: ${response.code}")
                    }
                }
            }

        })
    }
}
