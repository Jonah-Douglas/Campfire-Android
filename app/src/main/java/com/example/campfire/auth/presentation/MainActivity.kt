package com.example.campfire.auth.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.campfire.core.data.RetrofitInstance
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import retrofit2.HttpException
import java.io.IOException


const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LandingScreen()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val response = try {
                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("username", "jondou")
                        .addFormDataPart("password", "password")
                        .build()

                    // WORKING
                    val token = RetrofitInstance.api.login(requestBody).body()

                    val requestBody2 = MultipartBody.Builder()
                    println(token)
                    println(requestBody2)

                    RetrofitInstance.api.getUsers()
                } catch (e: IOException) {
                    Log.e(TAG, "IOException, you might not have an internet connection")
                    return@repeatOnLifecycle
                } catch (e: HttpException) {
                    Log.e(TAG, "HTTP Exception, unexpected response")
                    return@repeatOnLifecycle
                }

                if (response.isSuccessful && response.body() != null) {
                    println(response.body()!!)
                } else {
                    Log.e(TAG, "Response not successful")
                }
            }
        }
    }
}