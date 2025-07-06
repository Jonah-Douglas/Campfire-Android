package com.example.campfire.auth.data.repository

import android.util.Log
import com.example.campfire.auth.data.remote.AuthApiService
import com.example.campfire.auth.data.remote.dto.request.RegisterRequest
import com.example.campfire.auth.data.remote.dto.response.RegisterApiResponse
import com.example.campfire.auth.data.remote.dto.response.RegistrationData
import com.example.campfire.auth.data.remote.dto.response.TokenResponse
import com.example.campfire.auth.domain.repository.AuthRepository
import com.example.campfire.auth.domain.repository.LoginResult
import com.example.campfire.auth.domain.repository.LogoutResult
import com.example.campfire.auth.domain.repository.RegisterResult
import com.example.campfire.core.data.auth.AuthTokenStorage
import com.example.campfire.core.data.auth.AuthTokens
import com.example.campfire.core.domain.SessionInvalidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: AuthApiService,
    private val tokenStorage: AuthTokenStorage,
) : AuthRepository, SessionInvalidator {
    
    override suspend fun login(email: String, password: String): LoginResult {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.loginUser(
                    username = email,
                    password = password
                )
                
                if (response.isSuccessful && response.body() != null) {
                    val tokenDetails: TokenResponse = response.body()!!
                    val authTokens = AuthTokens(
                        accessToken = tokenDetails.accessToken,
                        refreshToken = tokenDetails.refreshToken
                    )
                    
                    try {
                        tokenStorage.saveTokens(authTokens)
                    } catch (e: Exception) {
                        Log.e(
                            "AuthRepositoryImpl",
                            "Failed to save tokens after successful login.",
                            e
                        )
                        return@withContext LoginResult.GenericError(
                            message = "Login successful but failed to save session. Please try again."
                        )
                    }
                    
                    // JD TODO: Determine if I need to add this email verification note on login success (probably not)
                    //val requiresVerification = false // Placeholder - GET THIS FROM API RESPONSE
                    
                    LoginResult.Success(
                        tokens = authTokens,
                        //requiresEmailVerification = requiresVerification
                    )
                } else {
                    val errorCode = response.code()
                    val errorBody = response.errorBody()?.string()
                    Log.e(
                        "AuthRepositoryImpl",
                        "Login HTTP error - Code: $errorCode, Message: ${response.message()}, Error: $errorBody"
                    )
                    return@withContext when (errorCode) {
                        401 -> LoginResult.InvalidCredentialsError()
                        else -> LoginResult.GenericError(message = "Login failed: $errorBody")
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
    
    override suspend fun logout(): LogoutResult {
        return withContext(Dispatchers.IO) {
            var serverLogoutAttempted = false
            var serverLogoutSucceeded = false
            var serverErrorCode: Int? = null
            var serverErrorMessage: String? = null
            
            try {
                Log.d("AuthRepositoryImpl", "Attempting server logout...")
                val response = apiService.logoutUser()
                serverLogoutAttempted = true
                if (response.isSuccessful) {
                    serverLogoutSucceeded = true
                    Log.d("AuthRepositoryImpl", "Server logout successful.")
                } else {
                    serverErrorCode = response.code()
                    serverErrorMessage = response.errorBody()?.string() ?: response.message()
                    Log.e(
                        "AuthRepositoryImpl",
                        "Server logout failed - Code: $serverErrorCode, Message: $serverErrorMessage"
                    )
                }
            } catch (e: IOException) {
                Log.w(
                    "AuthRepositoryImpl",
                    "Network error during server logout attempt: ${e.message}",
                    e
                )
            } catch (e: Exception) {
                Log.e(
                    "AuthRepositoryImpl",
                    "Exception during server logout attempt: ${e.message}",
                    e
                )
                serverErrorMessage = e.message
            }
            
            // Always clear local tokens after attempting server logout.
            // This ensures the user is logged out of the app.
            Log.d("AuthRepositoryImpl", "Clearing local authentication tokens.")
            tokenStorage.clearTokens()
            
            // Determine the final LogoutResult based on server interaction
            if (serverLogoutSucceeded) {
                LogoutResult.Success
            } else if (!serverLogoutAttempted && serverErrorMessage == null) {
                // This case implies an IOException occurred before even attempting the call,
                // and it was caught and logged.
                LogoutResult.NetworkError("Could not connect to the server for logout. Local data cleared.")
            } else {
                // Server logout failed or wasn't confirmed successful.
                // If it was a 401, the user is effectively logged out on the server too.
                if (serverErrorCode == 401) {
                    LogoutResult.GenericError(
                        code = serverErrorCode,
                        // Even though server call failed, the local state is clean.
                        // Message can reflect that tokens were invalid.
                        message = "Session expired or invalid. You have been logged out."
                    )
                } else if (serverErrorCode != null) {
                    LogoutResult.GenericError(
                        code = serverErrorCode,
                        message = "Server logout failed: $serverErrorMessage. Local data cleared."
                    )
                } else if (serverErrorMessage != null) { // Exception during API call but not an HTTP one
                    LogoutResult.GenericError(
                        message = "Error during server logout: $serverErrorMessage. Local data cleared."
                    )
                } else { // Should ideally be covered by IOException if serverLogoutAttempted is false
                    LogoutResult.NetworkError("Could not connect to server for logout. Local data cleared.")
                }
            }
        }
    }
    
    override suspend fun invalidateSessionAndTriggerLogout() {
        withContext(Dispatchers.IO) { // Ensure execution on IO dispatcher
            Log.w(
                "AuthRepositoryImpl",
                "Invalidating session: Clearing local tokens and notifying app."
            )
            tokenStorage.clearTokens()
            // TODO: Notify other parts of the app that user has been logged out.
            // This could be via a SharedFlow in the repository or a dedicated SessionManager.
            // For example: _userSessionStateFlow.emit(UserLoggedOutState)
            Log.i("AuthRepositoryImpl", "Session invalidated and local tokens cleared.")
        }
    }
    
    override suspend fun registerUser(registerRequest: RegisterRequest): RegisterResult {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.registerUser(registerRequest)
                
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse: RegisterApiResponse = response.body()!! // Top-level DTO
                    val apiData: RegistrationData? = apiResponse.data     // Nested data DTO
                    
                    if (apiData != null) {
                        // Now this access should work if 'requiresVerification' is defined in RegistrationDataDto
                        val requiresVerification =
                            apiData.requiresVerification != false // Default if null
                        
                        RegisterResult.Success(
                            // You can choose which message to use, or combine them
                            message = apiData.specificDataMessage ?: apiResponse.message
                            ?: "Registration successful",
                            requiresEmailVerification = requiresVerification
                        )
                    } else {
                        // Data object is null, but response was successful (unusual, but handle it)
                        RegisterResult.GenericError(
                            message = apiResponse.message
                                ?: "Registration succeeded but no data returned."
                        )
                    }
                } else {
                    // Handle API errors (response.isSuccessful is false)
                    val errorCode = response.code()
                    val errorBody = response.errorBody()?.string()
                    Log.w("AuthRepositoryImpl", "Registration API error: $errorCode - $errorBody")
                    // ... (your existing error handling logic) ...
                    RegisterResult.GenericError(
                        errorCode,
                        errorBody ?: "Registration failed with an unknown server error."
                    )
                }
            } catch (e: HttpException) {
                // Non-2xx HTTP responses
                val errorBody = e.response()?.errorBody()?.string()
                Log.e(
                    "AuthRepositoryImpl",
                    "RegisterUser HttpException: ${e.code()} - $errorBody",
                    e
                )
                RegisterResult.NetworkError("A network error occurred: ${e.code()}. Please try again.")
            } catch (e: IOException) {
                // Network errors (no internet, server unreachable)
                Log.e("AuthRepositoryImpl", "RegisterUser IOException", e)
                RegisterResult.NetworkError("Could not connect to the server. Please check your internet connection.")
            } catch (e: Exception) {
                // Other unexpected errors
                Log.e("AuthRepositoryImpl", "RegisterUser Exception", e)
                RegisterResult.GenericError(message = "An unexpected error occurred during registration: ${e.message}")
            }
        }
    }
    
    // JD TODO: Add other methods for verifyEmail, verifyPhone, etc.
}