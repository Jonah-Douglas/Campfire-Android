package com.example.campfire.auth.data.repository

import android.util.Log
import com.example.campfire.auth.data.remote.AuthApiService
import com.example.campfire.auth.data.remote.dto.response.TokenResponse
import com.example.campfire.auth.domain.repository.AuthRepository
import com.example.campfire.auth.domain.repository.LoginResult
import com.example.campfire.core.data.auth.AuthTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject


class AuthRepositoryImpl @Inject constructor(
    private val apiService: AuthApiService,
) : AuthRepository {
    override suspend fun login(email: String, password: String): LoginResult {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.loginUser(
                    username = email,
                    password = password
                )
                
                if (response.isSuccessful && response.body() != null) {
                    val tokenDetails: TokenResponse = response.body()!!
                    LoginResult.Success(
                        AuthTokens(
                            accessToken = tokenDetails.accessToken,
                            refreshToken = tokenDetails.refreshToken
                        )
                    )
                } else {
                    val errorCode = response.code()
                    val errorBody = response.errorBody()?.string()
                    Log.e(
                        "AuthRepositoryImpl",
                        "Login HTTP error - Code: $errorCode, Message: ${response.message()}, Error: $errorBody"
                    )
                    // You might want to parse errorBody to provide more specific error types
                    when (errorCode) {
                        401 -> LoginResult.InvalidCredentialsError
                        // Add other specific error code handling if your API provides them
                        // 400 for inactive user might be another
                        else -> LoginResult.GenericError(
                            code = errorCode,
                            message = "Login failed: $errorBody"
                        )
                    }
                }
            } catch (e: HttpException) {    // For non-2xx HTTP responses
                Log.e("AuthRepositoryImpl", "Login HttpException", e)
                LoginResult.NetworkError("A network error occurred: ${e.message()}")
            } catch (e: IOException) {      // For network issues (no connectivity)
                Log.e("AuthRepositoryImpl", "Login IOException", e)
                LoginResult.NetworkError("Could not connect to the server. Please check your internet connection.")
            } catch (e: Exception) {        // For general unexpected errors (e.g., JSON parsing)
                Log.e("AuthRepositoryImpl", "Login Exception", e)
                LoginResult.GenericError(
                    message = e.localizedMessage ?: "An unexpected error occurred."
                )
            }
        }
    }
    
    // JD TODO: Add other methods for register, verifyEmail, verifyPhone, etc.
}