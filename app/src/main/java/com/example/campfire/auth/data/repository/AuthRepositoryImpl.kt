package com.example.campfire.auth.data.repository

import com.example.campfire.auth.data.local.AuthTokenStorage
import com.example.campfire.auth.data.local.AuthTokens
import com.example.campfire.auth.data.mapper.UserMapper
import com.example.campfire.auth.data.remote.AuthApiService
import com.example.campfire.auth.data.remote.dto.request.CompleteProfileRequest
import com.example.campfire.auth.data.remote.dto.request.SendOTPRequest
import com.example.campfire.auth.data.remote.dto.request.VerifyOTPRequest
import com.example.campfire.auth.data.remote.dto.response.ApiResponse
import com.example.campfire.auth.data.remote.dto.response.OTPResponse
import com.example.campfire.auth.data.remote.dto.response.TokenResponse
import com.example.campfire.auth.domain.repository.AuthRepository
import com.example.campfire.auth.domain.repository.CompleteProfileResult
import com.example.campfire.auth.domain.repository.Field
import com.example.campfire.auth.domain.repository.LogoutResult
import com.example.campfire.auth.domain.repository.SendOTPResult
import com.example.campfire.auth.domain.repository.VerifyOTPResult
import com.example.campfire.auth.presentation.navigation.AuthAction
import com.example.campfire.core.common.exception.MappingException
import com.example.campfire.core.common.logging.Firelog
import com.example.campfire.core.domain.SessionInvalidator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
    private val userMapper: UserMapper
) : AuthRepository, SessionInvalidator {
    
    override suspend fun sendOTP(phoneNumber: String, authAction: AuthAction): SendOTPResult {
        return withContext(Dispatchers.IO) {
            try {
                val requestDTO = SendOTPRequest(phoneNumber = phoneNumber)
                val apiResponse = apiService.sendOtp(requestDTO)
                
                if (apiResponse.success) {
                    val otpData: OTPResponse? = apiResponse.data
                    if (otpData?.debugOTP != null) {
                        Firelog.d("DEV MODE (from API Response): OTP for $phoneNumber is ${otpData.debugOTP}")
                    }
                    SendOTPResult.Success
                } else {
                    val generalMessage = apiResponse.message
                    val apiErrorDetails = apiResponse.error
                    
                    Firelog.e(
                        String.format(
                            LOG_SEND_OTP_API_NOT_SUCCESSFUL,
                            false, // apiResponse.success is known to be false here
                            generalMessage,
                            apiErrorDetails?.code,
                            apiErrorDetails?.details
                        )
                    )
                    
                    if (apiErrorDetails == null) {
                        SendOTPResult.Generic(
                            message = generalMessage ?: ERROR_API_FAILED_NO_DETAILS
                        )
                    } else {
                        val effectiveErrorMessage = apiErrorDetails.details ?: generalMessage
                        ?: String.format(ERROR_API_UNKNOWN_WITH_CODE, apiErrorDetails.code)
                        
                        when (apiErrorDetails.code) {
                            API_ERROR_CODE_USER_ALREADY_EXISTS -> {
                                if (authAction == AuthAction.REGISTER) {
                                    SendOTPResult.UserAlreadyExists(message = effectiveErrorMessage)
                                } else {
                                    SendOTPResult.Generic(
                                        message = String.format(
                                            ERROR_SEND_OTP_USER_EXISTS_WRONG_ACTION,
                                            apiErrorDetails.code,
                                            effectiveErrorMessage
                                        )
                                    )
                                }
                            }
                            
                            API_ERROR_CODE_USER_NOT_FOUND -> {
                                if (authAction == AuthAction.LOGIN) {
                                    SendOTPResult.UserNotFound(message = effectiveErrorMessage)
                                } else {
                                    SendOTPResult.Generic(
                                        message = String.format(
                                            ERROR_SEND_OTP_USER_NOT_FOUND_WRONG_ACTION,
                                            apiErrorDetails.code,
                                            effectiveErrorMessage
                                        )
                                    )
                                }
                            }
                            
                            API_ERROR_CODE_RATE_LIMITED -> SendOTPResult.RateLimited(
                                message = effectiveErrorMessage
                            )
                            
                            API_ERROR_CODE_INVALID_PHONE_NUMBER -> SendOTPResult.InvalidPhoneNumber(
                                message = effectiveErrorMessage
                            )
                            
                            else -> {
                                SendOTPResult.Generic(
                                    message = effectiveErrorMessage
                                )
                            }
                        }
                    }
                }
            } catch (e: HttpException) {
                val errorCode = e.code()
                val errorBody = e.response()?.errorBody()?.string()
                var backendDrivenHttpErrorMessage: String? = null
                
                if (errorBody != null) {
                    try {
                        val gson = Gson()
                        val errorResponse = gson.fromJson<ApiResponse<Any>>(
                            errorBody,
                            object : TypeToken<ApiResponse<Any>>() {}.type
                        )
                        backendDrivenHttpErrorMessage =
                            errorResponse?.error?.details ?: errorResponse?.message
                    } catch (parseException: Exception) {
                        Firelog.w(LOG_ERROR_BODY_PARSE_FAILURE_HTTP_EXCEPTION, parseException)
                    }
                }
                
                val httpErrorMessage = backendDrivenHttpErrorMessage ?: String.format(
                    ERROR_HTTP_GENERIC,
                    errorCode,
                    errorBody ?: e.message()
                )
                Firelog.e(String.format(LOG_SEND_OTP_HTTP_EXCEPTION, httpErrorMessage), e)
                
                when (errorCode) {
                    400 -> SendOTPResult.InvalidPhoneNumber(message = httpErrorMessage)
                    409 -> {
                        if (authAction == AuthAction.REGISTER) {
                            SendOTPResult.UserAlreadyExists(message = httpErrorMessage)
                        } else { // Assuming LOGIN or other action implies user should exist
                            SendOTPResult.UserNotFound(message = httpErrorMessage)
                        }
                    }
                    
                    429 -> SendOTPResult.RateLimited(message = httpErrorMessage)
                    else -> SendOTPResult.Network(message = httpErrorMessage)
                }
            } catch (e: IOException) {
                val networkErrorMessage =
                    String.format(ERROR_IO_EXCEPTION_SEND_OTP, e.message)
                Firelog.e(String.format(LOG_SEND_OTP_IO_EXCEPTION, e.message), e)
                SendOTPResult.Network(message = networkErrorMessage)
            } catch (e: Exception) {
                val genericErrorMessage = String.format(ERROR_UNEXPECTED, e.message)
                Firelog.e(String.format(LOG_SEND_OTP_EXCEPTION, e.message), e)
                SendOTPResult.Generic(message = genericErrorMessage)
            }
        }
    }
    
    override suspend fun verifyOTP(
        phoneNumber: String,
        otpCode: String,
        authAction: AuthAction
    ): VerifyOTPResult {
        return withContext(Dispatchers.IO) {
            try {
                val request = VerifyOTPRequest(
                    phoneNumber = phoneNumber,
                    verificationCode = otpCode
                )
                val apiResponse = apiService.verifyOTP(request)
                
                if (apiResponse.success && apiResponse.data != null) {
                    val tokenDetails: TokenResponse = apiResponse.data
                    val authTokens = AuthTokens(
                        accessToken = tokenDetails.accessToken,
                        refreshToken = tokenDetails.refreshToken
                    )
                    
                    try {
                        tokenStorage.saveTokens(authTokens)
                    } catch (e: Exception) {
                        Firelog.e(LOG_VERIFY_OTP_TOKEN_SAVE_FAILURE, e)
                        return@withContext VerifyOTPResult.Generic(
                            message = ERROR_VERIFY_OTP_TOKEN_SAVE_FAILURE
                        )
                    }
                    
                    when (authAction) {
                        AuthAction.LOGIN -> VerifyOTPResult.SuccessLogin(authTokens)
                        AuthAction.REGISTER -> {
                            if (tokenDetails.isNewUser) {
                                VerifyOTPResult.SuccessRegistration(authTokens)
                            } else {
                                // Was a registration attempt, OTP valid, but user already existed
                                VerifyOTPResult.SuccessButUserExistedDuringRegistration(authTokens)
                            }
                        }
                    }
                } else {
                    val generalMessage = apiResponse.message
                    val apiErrorDetails = apiResponse.error
                    
                    Firelog.e(
                        String.format(
                            LOG_VERIFY_OTP_BUSINESS_ERROR,
                            generalMessage ?: apiErrorDetails.toString()
                        )
                    )
                    
                    val resultFromApiError: VerifyOTPResult = if (apiErrorDetails == null) {
                        VerifyOTPResult.Generic(
                            message = generalMessage ?: ERROR_VERIFY_OTP_FAILED_NO_DETAILS
                        )
                    } else {
                        val effectiveErrorMessage = apiErrorDetails.details ?: generalMessage
                        ?: String.format(ERROR_API_UNKNOWN_WITH_CODE, apiErrorDetails.code)
                        when (apiErrorDetails.code) {
                            API_ERROR_CODE_OTP_INCORRECT, API_ERROR_CODE_INVALID_OTP -> VerifyOTPResult.OTPIncorrect(
                                message = effectiveErrorMessage
                            )
                            
                            API_ERROR_CODE_OTP_EXPIRED -> VerifyOTPResult.OTPExpired(
                                message = effectiveErrorMessage
                            )
                            
                            API_ERROR_CODE_RATE_LIMITED -> {
                                val retrySecondsString =
                                    apiErrorDetails.fields?.get(API_FIELD_RETRY_AFTER_SECONDS)
                                        ?: apiErrorDetails.details?.let { details ->
                                            val match =
                                                Regex(REGEX_RETRY_AFTER_SECONDS).find(details)
                                            match?.groupValues?.getOrNull(1)
                                        }
                                VerifyOTPResult.RateLimited(retryAfterSeconds = retrySecondsString?.toIntOrNull())
                            }
                            
                            API_ERROR_CODE_USER_NOT_FOUND -> {
                                VerifyOTPResult.Generic(
                                    message = String.format(
                                        ERROR_VERIFY_OTP_USER_NOT_FOUND,
                                        effectiveErrorMessage
                                    )
                                )
                            }
                            
                            else -> {
                                VerifyOTPResult.Generic(
                                    message = effectiveErrorMessage
                                )
                            }
                        }
                    }
                    return@withContext resultFromApiError
                }
            } catch (e: HttpException) {
                val errorCode = e.code()
                val errorBody = e.response()?.errorBody()?.string()
                var backendDrivenHttpErrorMessage: String? = null
                
                if (errorBody != null) {
                    try {
                        val gson = Gson()
                        val errorResponse = gson.fromJson<ApiResponse<Any>>(
                            errorBody,
                            object : TypeToken<ApiResponse<Any>>() {}.type
                        )
                        backendDrivenHttpErrorMessage =
                            errorResponse?.error?.details ?: errorResponse?.message
                    } catch (parseException: Exception) {
                        Firelog.w(
                            LOG_ERROR_BODY_PARSE_FAILURE_VERIFY_OTP_HTTP_EXCEPTION,
                            parseException
                        )
                    }
                }
                val httpErrorMessage = backendDrivenHttpErrorMessage ?: String.format(
                    ERROR_HTTP_GENERIC,
                    errorCode,
                    errorBody ?: e.message()
                )
                Firelog.e(String.format(LOG_VERIFY_OTP_HTTP_EXCEPTION, httpErrorMessage), e)
                
                when (errorCode) {
                    400 -> VerifyOTPResult.OTPIncorrect(
                        message = String.format(ERROR_VERIFY_OTP_HTTP_400, httpErrorMessage)
                    )
                    
                    401, 403 -> VerifyOTPResult.OTPIncorrect(
                        message = String.format(ERROR_VERIFY_OTP_HTTP_401_403, httpErrorMessage)
                    )
                    
                    404 -> VerifyOTPResult.Generic(
                        code = errorCode,
                        message = String.format(ERROR_VERIFY_OTP_HTTP_404, httpErrorMessage)
                    )
                    
                    410 -> VerifyOTPResult.OTPExpired(
                        message = String.format(ERROR_VERIFY_OTP_HTTP_410, httpErrorMessage)
                    )
                    
                    429 -> {
                        val retryAfterHeader = e.response()?.headers()?.get(HEADER_RETRY_AFTER)
                        VerifyOTPResult.RateLimited(retryAfterSeconds = retryAfterHeader?.toIntOrNull())
                    }
                    
                    else -> VerifyOTPResult.Network(
                        message = String.format(ERROR_VERIFY_OTP_NETWORK_HTTP, httpErrorMessage)
                    )
                }
            } catch (e: IOException) {
                Firelog.e(LOG_VERIFY_OTP_IO_EXCEPTION, e)
                VerifyOTPResult.Network(message = ERROR_IO_EXCEPTION_VERIFY_OTP)
            } catch (e: Exception) {
                Firelog.e(LOG_VERIFY_OTP_EXCEPTION, e)
                VerifyOTPResult.Generic(
                    message = e.localizedMessage ?: ERROR_UNEXPECTED_VERIFY_OTP
                )
            }
        }
    }
    
    override suspend fun completeUserProfile(request: CompleteProfileRequest): CompleteProfileResult {
        return withContext(Dispatchers.IO) {
            try {
                val apiResponse = apiService.completeUserProfile(request)
                
                if (apiResponse.success && apiResponse.data != null) {
                    val userResponseDTO = apiResponse.data
                    try {
                        val user = userMapper.mapToDomain(userResponseDTO)
                        CompleteProfileResult.Success(user)
                    } catch (e: MappingException) {
                        Firelog.e(String.format(LOG_COMPLETE_PROFILE_MAPPING_ERROR, e.message), e)
                        CompleteProfileResult.Generic(
                            message = String.format(
                                ERROR_COMPLETE_PROFILE_MAPPING,
                                e.message
                            )
                        )
                    } catch (e: Exception) {
                        Firelog.e(LOG_COMPLETE_PROFILE_UNEXPECTED_MAPPING_ERROR, e)
                        CompleteProfileResult.Generic(message = ERROR_COMPLETE_PROFILE_UNEXPECTED_MAPPING)
                    }
                } else {
                    val generalMessage = apiResponse.message
                    val apiErrorDetails = apiResponse.error
                    
                    Firelog.e(
                        String.format(
                            LOG_COMPLETE_PROFILE_API_NOT_SUCCESSFUL,
                            apiResponse.success,
                            generalMessage,
                            apiErrorDetails?.let { "Error Code: ${it.code}, Details: ${it.details}, Fields: ${it.fields}" }
                                ?: LOG_NO_API_ERROR_DETAILS
                        )
                    )
                    
                    if (apiErrorDetails == null) {
                        CompleteProfileResult.Generic(
                            message = generalMessage ?: ERROR_COMPLETE_PROFILE_UNKNOWN_API
                        )
                    } else {
                        val effectiveErrorMessage = apiErrorDetails.details ?: generalMessage
                        ?: String.format(
                            ERROR_COMPLETE_PROFILE_WITH_CODE,
                            apiErrorDetails.code
                        )
                        
                        when (apiErrorDetails.code) {
                            API_ERROR_CODE_EMAIL_ALREADY_EXISTS -> {
                                CompleteProfileResult.EmailAlreadyExists
                            }
                            
                            API_ERROR_CODE_VALIDATION_ERROR -> {
                                val validationErrors =
                                    apiErrorDetails.fields?.mapNotNull { (key, value) ->
                                        val fieldEnum = try {
                                            Field.valueOf(key.uppercase())
                                        } catch (_: IllegalArgumentException) {
                                            Firelog.w(
                                                String.format(
                                                    LOG_UNKNOWN_VALIDATION_FIELD,
                                                    key
                                                )
                                            )
                                            null
                                        }
                                        fieldEnum?.let { it to value }
                                    }?.toMap() ?: emptyMap()
                                
                                if (validationErrors.isNotEmpty()) {
                                    CompleteProfileResult.Validation(errors = validationErrors)
                                } else {
                                    CompleteProfileResult.Generic(
                                        message = String.format(
                                            ERROR_VALIDATION_FAILED_NO_SPECIFIC_FIELDS,
                                            effectiveErrorMessage
                                        )
                                    )
                                }
                            }
                            
                            else -> {
                                CompleteProfileResult.Generic(
                                    message = effectiveErrorMessage
                                )
                            }
                        }
                    }
                }
            } catch (e: HttpException) {
                Firelog.e(LOG_COMPLETE_PROFILE_HTTP_EXCEPTION, e)
                CompleteProfileResult.Network(
                    String.format(
                        ERROR_NETWORK_GENERIC_HTTP,
                        e.message()
                    )
                )
            } catch (e: IOException) {
                Firelog.e(LOG_COMPLETE_PROFILE_IO_EXCEPTION, e)
                CompleteProfileResult.Network(message = ERROR_IO_EXCEPTION_GENERIC)
            } catch (e: Exception) {
                Firelog.e(LOG_COMPLETE_PROFILE_EXCEPTION, e)
                CompleteProfileResult.Generic(
                    message = e.localizedMessage ?: ERROR_UNEXPECTED
                )
            }
        }
    }
    
    override suspend fun logout(): LogoutResult {
        return withContext(Dispatchers.IO) {
            var serverLogoutSucceeded = false
            var serverErrorMessage: String? = null
            var exceptionMessage: String? = null
            
            // 1. Attempt server-side logout
            try {
                Firelog.d(LOG_LOGOUT_ATTEMPT_SERVER)
                val apiResponse = apiService.logoutUser()
                
                if (apiResponse.success) {
                    serverLogoutSucceeded = true
                    Firelog.i(LOG_LOGOUT_SERVER_SUCCESS)
                } else {
                    val apiErrorDetails = apiResponse.error
                    val generalApiMessage = apiResponse.message
                    
                    serverErrorMessage = if (apiErrorDetails != null) {
                        String.format(
                            ERROR_LOGOUT_SERVER_ERROR_DETAILS,
                            apiErrorDetails.details ?: generalApiMessage ?: String.format(
                                ERROR_LOGOUT_UNKNOWN_SERVER_ERROR_CODE,
                                apiErrorDetails.code
                            )
                        )
                    } else {
                        generalApiMessage ?: ERROR_LOGOUT_SERVER_UNSPECIFIED_FAILURE
                    }
                    Firelog.e(String.format(LOG_LOGOUT_SERVER_FAILED, serverErrorMessage))
                }
            } catch (e: HttpException) {
                Firelog.e(
                    String.format(
                        LOG_LOGOUT_NETWORK_ERROR_HTTP,
                        e.code()
                    ),
                    e
                )
                exceptionMessage = String.format(ERROR_LOGOUT_NETWORK_HTTP, e.code())
            } catch (e: IOException) {
                Firelog.e(LOG_LOGOUT_CONNECTION_ERROR, e)
                exceptionMessage = ERROR_LOGOUT_CONNECTION
            } catch (e: Exception) {
                Firelog.e(LOG_LOGOUT_UNEXPECTED_ERROR_SERVER, e)
                exceptionMessage =
                    String.format(ERROR_LOGOUT_UNEXPECTED_SERVER, e.localizedMessage)
            }
            
            // 2. Always attempt to clear local tokens and invalidate session
            var localCleanupError: String? = null
            try {
                Firelog.d(LOG_LOGOUT_CLEARING_LOCAL_DATA)
                tokenStorage.clearTokens()
                Firelog.i(LOG_LOGOUT_LOCAL_DATA_CLEARED)
            } catch (e: Exception) {
                Firelog.e(LOG_LOGOUT_CRITICAL_CLEAR_FAILURE, e)
                localCleanupError = ERROR_LOGOUT_LOCAL_CLEANUP_FAILURE
            }
            
            // 3. Determine the final LogoutResult
            if (serverLogoutSucceeded && exceptionMessage == null && localCleanupError == null) {
                LogoutResult.Success
            } else {
                val combinedErrorMessage = listOfNotNull(
                    if (!serverLogoutSucceeded) serverErrorMessage
                        ?: ERROR_LOGOUT_SERVER_NOT_CONFIRMED
                    else null,
                    exceptionMessage,
                    localCleanupError
                ).joinToString(separator = " | ")
                
                val finalMessage =
                    combinedErrorMessage.ifEmpty { ERROR_LOGOUT_UNSPECIFIED_ISSUE }
                
                if (exceptionMessage != null && (exceptionMessage.contains(KEYWORD_NETWORK_ERROR) || exceptionMessage.contains(
                        KEYWORD_COULD_NOT_CONNECT
                    ))
                ) {
                    LogoutResult.Network(message = finalMessage)
                } else {
                    LogoutResult.Generic(message = finalMessage)
                }
            }
        }
    }
    
    
    override suspend fun invalidateSessionAndTriggerLogout() {
        withContext(Dispatchers.IO) {
            Firelog.w(LOG_INVALIDATE_SESSION)
            tokenStorage.clearTokens()
            // JD TODO: Notify other parts of the app that user has been logged out.
            Firelog.i(LOG_SESSION_INVALIDATED_TOKENS_CLEARED)
        }
    }
    
    // JD TODO: Add other methods for verifyEmail, etc.
    
    companion object {
        // --- General Error Messages ---
        private const val ERROR_API_FAILED_NO_DETAILS =
            "API operation failed without specific error details."
        private const val ERROR_API_UNKNOWN_WITH_CODE =
            "An unknown API error occurred. Code: %s"
        private const val ERROR_UNEXPECTED = "An unexpected error occurred: %s"
        private const val ERROR_IO_EXCEPTION_GENERIC =
            "Could not connect to the server. Please check your internet connection."
        private const val ERROR_NETWORK_GENERIC_HTTP = "A network error occurred: %s"
        
        // --- SendOTP: Log Messages ---
        private const val LOG_SEND_OTP_API_NOT_SUCCESSFUL =
            "sendOTP API call not successful. Success: %s, Message: %s, Error Code: %s, Error Details: %s"
        private const val LOG_ERROR_BODY_PARSE_FAILURE_HTTP_EXCEPTION =
            "Could not parse error body as ApiResponse for SendOTP HttpException"
        private const val LOG_SEND_OTP_HTTP_EXCEPTION = "sendOTP HttpException - %s"
        private const val LOG_SEND_OTP_IO_EXCEPTION = "sendOTP IOException: %s"
        private const val LOG_SEND_OTP_EXCEPTION = "sendOTP Exception: %s"
        
        // --- SendOTP: Error Messages ---
        private const val ERROR_SEND_OTP_USER_EXISTS_WRONG_ACTION =
            "User found (code: %s), but login action expected different state. Message: %s"
        private const val ERROR_SEND_OTP_USER_NOT_FOUND_WRONG_ACTION =
            "User not found (code: %s), but register action expected different state. Message: %s"
        private const val ERROR_HTTP_GENERIC = "HTTP %s: %s"
        private const val ERROR_IO_EXCEPTION_SEND_OTP =
            "Could not connect. Please check your internet connection: %s"
        
        
        // --- VerifyOTP: Log Messages ---
        private const val LOG_VERIFY_OTP_TOKEN_SAVE_FAILURE =
            "Failed to save tokens after successful OTP verification."
        private const val LOG_VERIFY_OTP_BUSINESS_ERROR = "Verify OTP business error: %s"
        private const val LOG_ERROR_BODY_PARSE_FAILURE_VERIFY_OTP_HTTP_EXCEPTION =
            "Could not parse error body as ApiResponse for VerifyOTP HttpException"
        private const val LOG_VERIFY_OTP_HTTP_EXCEPTION = "VerifyOTP HttpException - %s"
        private const val LOG_VERIFY_OTP_IO_EXCEPTION = "VerifyOTP IOException"
        private const val LOG_VERIFY_OTP_EXCEPTION = "VerifyOTP Exception"
        
        
        // --- VerifyOTP: Error Messages ---
        private const val ERROR_VERIFY_OTP_TOKEN_SAVE_FAILURE =
            "OTP verification successful but failed to save session."
        private const val ERROR_VERIFY_OTP_FAILED_NO_DETAILS =
            "OTP verification failed without specific error details."
        private const val ERROR_VERIFY_OTP_USER_NOT_FOUND =
            "User not found for OTP verification. Server message: %s"
        private const val ERROR_VERIFY_OTP_HTTP_400 = "Invalid OTP format or request. %s"
        private const val ERROR_VERIFY_OTP_HTTP_401_403 =
            "OTP incorrect or session invalid. %s"
        private const val ERROR_VERIFY_OTP_HTTP_404 = "User or OTP session not found. %s"
        private const val ERROR_VERIFY_OTP_HTTP_410 = "OTP has expired. %s"
        private const val ERROR_VERIFY_OTP_NETWORK_HTTP =
            "Network error during OTP verification: %s"
        private const val ERROR_IO_EXCEPTION_VERIFY_OTP =
            "Could not connect to verify OTP. Please check your internet connection."
        private const val ERROR_UNEXPECTED_VERIFY_OTP =
            "An unexpected error occurred during OTP verification."
        
        
        // --- CompleteProfile: Log Messages ---
        private const val LOG_COMPLETE_PROFILE_MAPPING_ERROR =
            "Error mapping UserResponse to User domain model: %s"
        private const val LOG_COMPLETE_PROFILE_UNEXPECTED_MAPPING_ERROR =
            "Unexpected error during user data mapping stage."
        private const val LOG_COMPLETE_PROFILE_API_NOT_SUCCESSFUL =
            "Complete Profile API call not successful. Success: %s, Message: %s, %s"
        private const val LOG_NO_API_ERROR_DETAILS = "No error details object."
        private const val LOG_UNKNOWN_VALIDATION_FIELD = "Unknown validation field from backend: %s"
        private const val LOG_COMPLETE_PROFILE_HTTP_EXCEPTION = "CompleteProfile HttpException"
        private const val LOG_COMPLETE_PROFILE_IO_EXCEPTION = "CompleteProfile IOException"
        private const val LOG_COMPLETE_PROFILE_EXCEPTION = "CompleteProfile Exception"
        
        // --- CompleteProfile: Error Messages ---
        private const val ERROR_COMPLETE_PROFILE_MAPPING =
            "Error processing user data from server. Details: %s"
        private const val ERROR_COMPLETE_PROFILE_UNEXPECTED_MAPPING =
            "An unexpected error occurred while processing user data."
        private const val ERROR_COMPLETE_PROFILE_UNKNOWN_API =
            "Failed to complete profile due to an unknown API error."
        private const val ERROR_COMPLETE_PROFILE_WITH_CODE =
            "Failed to complete profile. Error code: %s"
        private const val ERROR_VALIDATION_FAILED_NO_SPECIFIC_FIELDS =
            "Validation failed with no specific fields. Server says: %s"
        
        
        // --- Logout: Log Messages ---
        private const val LOG_LOGOUT_ATTEMPT_SERVER = "Attempting server logout..."
        private const val LOG_LOGOUT_SERVER_SUCCESS = "Server logout successful."
        private const val LOG_LOGOUT_SERVER_FAILED = "Server logout failed: %s"
        private const val LOG_LOGOUT_NETWORK_ERROR_HTTP =
            "Network error during server logout (HTTP %s)"
        private const val LOG_LOGOUT_CONNECTION_ERROR = "Connection error during server logout"
        private const val LOG_LOGOUT_UNEXPECTED_ERROR_SERVER =
            "Unexpected error during server logout"
        private const val LOG_LOGOUT_CLEARING_LOCAL_DATA =
            "Clearing local tokens and session data..."
        private const val LOG_LOGOUT_LOCAL_DATA_CLEARED =
            "Local tokens and session data cleared."
        private const val LOG_LOGOUT_CRITICAL_CLEAR_FAILURE =
            "Critical: Failed to clear local tokens/session during logout."
        
        
        // --- Logout: Error Messages ---
        private const val ERROR_LOGOUT_SERVER_ERROR_DETAILS = "Server error: %s"
        private const val ERROR_LOGOUT_UNKNOWN_SERVER_ERROR_CODE = "Unknown server error (Code: %s)"
        private const val ERROR_LOGOUT_SERVER_UNSPECIFIED_FAILURE =
            "Server logout reported an unspecified failure."
        private const val ERROR_LOGOUT_NETWORK_HTTP =
            "Logout failed due to a network issue (Error %s). Please check your connection."
        private const val ERROR_LOGOUT_CONNECTION =
            "Logout failed. Could not connect to the server. Please check your internet connection."
        private const val ERROR_LOGOUT_UNEXPECTED_SERVER =
            "An unexpected error occurred during server logout: %s"
        private const val ERROR_LOGOUT_LOCAL_CLEANUP_FAILURE =
            "Logout partially completed, but failed to fully clear local session data. Please restart the app or clear app data if issues persist."
        private const val ERROR_LOGOUT_SERVER_NOT_CONFIRMED =
            "Server logout did not confirm success."
        private const val ERROR_LOGOUT_UNSPECIFIED_ISSUE =
            "Logout completed with an unspecified issue."
        
        // --- Session Invalidation: Log Messages ---
        private const val LOG_INVALIDATE_SESSION =
            "Invalidating session: Clearing local tokens and notifying app."
        private const val LOG_SESSION_INVALIDATED_TOKENS_CLEARED =
            "Session invalidated and local tokens cleared."
        
        // --- API Error Codes (as strings) ---
        private const val API_ERROR_CODE_USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS"
        private const val API_ERROR_CODE_USER_NOT_FOUND = "USER_NOT_FOUND"
        private const val API_ERROR_CODE_RATE_LIMITED = "RATE_LIMITED"
        private const val API_ERROR_CODE_INVALID_PHONE_NUMBER = "INVALID_PHONE_NUMBER"
        private const val API_ERROR_CODE_OTP_INCORRECT = "OTP_INCORRECT"
        private const val API_ERROR_CODE_INVALID_OTP = "INVALID_OTP"
        private const val API_ERROR_CODE_OTP_EXPIRED = "OTP_EXPIRED"
        private const val API_ERROR_CODE_EMAIL_ALREADY_EXISTS = "EMAIL_ALREADY_EXISTS"
        private const val API_ERROR_CODE_VALIDATION_ERROR = "VALIDATION_ERROR"
        
        
        // --- API/Headers/Regex ---
        private const val API_FIELD_RETRY_AFTER_SECONDS = "retry_after_seconds"
        private const val REGEX_RETRY_AFTER_SECONDS = "Retry after (\\d+) seconds"
        private const val HEADER_RETRY_AFTER = "Retry-After"
        
        // --- Keywords for error message parsing ---
        private const val KEYWORD_NETWORK_ERROR = "Network error"
        private const val KEYWORD_COULD_NOT_CONNECT = "Could not connect"
    }
}