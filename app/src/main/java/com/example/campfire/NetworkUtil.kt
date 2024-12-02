package com.example.campfire

import okhttp3.Authenticator
import okhttp3.Credentials.basic
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

            response.request.newBuilder().header("Authorization", "Bearer 1234567890").build()
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
}