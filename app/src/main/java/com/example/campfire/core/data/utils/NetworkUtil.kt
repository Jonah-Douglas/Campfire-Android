package com.example.campfire.core.data.utils

import com.example.campfire.core.data.RetrofitInstance
import com.example.campfire.auth.data.Token
import okhttp3.Authenticator
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit

class NetworkUtil {
    val client: OkHttpClient.Builder = OkHttpClient.Builder()

    // JD TODO: remove 3 attempt max in prod
    // JD TODO: Update the token being used by this client (should just need the token), if the token is no longer valid make new call to login and use new token
    init {
        client.authenticator(Authenticator { _, response ->
            if (responseCount(response) >= 5) {
                return@Authenticator null
            }
//            val credential = basic("name", "password")
//            response.request.newBuilder().header("Authorization", credential).build()
//            val requestBody = MultipartBody.Builder()
//                .setType(MultipartBody.FORM)
//                .addFormDataPart("username", "jondou")
//                .addFormDataPart("password", "1234")
//                .build()
            val newAccessToken = Token("jd", "bearer")//callAPILogin();

            newAccessToken?.let {
                return@Authenticator response.request.newBuilder()
                    .header(newAccessToken.token_type, newAccessToken.access_token).build()
            }
        })

        client.connectTimeout(10, TimeUnit.SECONDS)
        client.writeTimeout(10, TimeUnit.SECONDS)
        client.readTimeout(30, TimeUnit.SECONDS)
    }

    private fun responseCount(response: Response): Int {
        var response = response
        var result = 1

        while (response.priorResponse != null) {
            response = response.priorResponse!!
            result++
        }

        return result
    }

    private suspend fun callAPILogin(): Token? {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.Companion.FORM)
            .addFormDataPart("username", "jondou")
            .addFormDataPart("password", "password")
            .build()

        return RetrofitInstance.api.login(requestBody).body()
    }
}