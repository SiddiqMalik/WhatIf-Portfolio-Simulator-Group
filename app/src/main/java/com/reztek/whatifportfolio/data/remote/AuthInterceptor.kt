package com.reztek.whatifportfolio.data.remote

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the signed-in user's Firebase ID token as a Bearer token on every
 * request. Interceptors run on OkHttp's background dispatcher thread (never
 * the main thread), so blocking here with Tasks.await() is safe.
 */
class AuthInterceptor(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val user = auth.currentUser
        val request = if (user != null) {
            val token = Tasks.await(user.getIdToken(false)).token
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
