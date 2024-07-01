package com.example.campfire

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.campfire.ui.theme.CampfireTheme
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.HttpException
import java.io.IOException


const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CampfireTheme {
                Scaffold( modifier = Modifier.fillMaxSize() ) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val response = try {
                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("username", "jondou")
                        .addFormDataPart("password", "1234")
                        .build()

                    // WORKING
                    val token = RetrofitInstance.api.login(requestBody).body()

                    val requestBody2 = MultipartBody.Builder()

                    RetrofitInstance.api.getUsers()
                } catch (e: IOException) {
                    Log.e(TAG, "IOException, you might not have internet connection")
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

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CampfireTheme {
        Greeting("Android")
    }
}