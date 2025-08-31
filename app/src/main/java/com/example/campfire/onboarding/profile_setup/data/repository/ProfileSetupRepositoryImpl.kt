package com.example.campfire.onboarding.profile_setup.data.repository

import com.example.campfire.core.common.exception.MappingException
import com.example.campfire.core.common.logging.Firelog
import com.example.campfire.core.data.mapper.UserMapper
import com.example.campfire.core.data.remote.dto.response.APIResponse
import com.example.campfire.onboarding.profile_setup.data.remote.ProfileSetupAPIService
import com.example.campfire.onboarding.profile_setup.data.remote.dto.request.CompleteOnboardingProfileRequest
import com.example.campfire.onboarding.profile_setup.domain.model.CompleteProfileSetupResult
import com.example.campfire.onboarding.profile_setup.domain.model.ProfileSetupField
import com.example.campfire.onboarding.profile_setup.domain.repository.ProfileSetupRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ProfileSetupRepositoryImpl @Inject constructor(
    private val profileSetupAPIService: ProfileSetupAPIService,
    private val userMapper: UserMapper
) : ProfileSetupRepository {
    
    override suspend fun completeProfileSetup(
        firstName: String,
        lastName: String,
        email: String,
        dateOfBirth: LocalDate,
        enableNotifications: Boolean
    ): CompleteProfileSetupResult {
        return withContext(Dispatchers.IO) {
            try {
                val request = CompleteOnboardingProfileRequest(
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    dateOfBirth = dateOfBirth.toString(),
                    enableNotifications = enableNotifications
                )
                
                val apiResponse = profileSetupAPIService.completeOnboardingProfile(request)
                
                if (apiResponse.success && apiResponse.data != null) {
                    val userResponseDTO = apiResponse.data
                    try {
                        val user = userMapper.mapToDomain(userResponseDTO)
                        CompleteProfileSetupResult.Success(user)
                    } catch (e: MappingException) {
                        Firelog.e(String.format(LOG_ONBOARDING_MAPPING_ERROR, e.message), e)
                        CompleteProfileSetupResult.Generic(
                            message = String.format(
                                ERROR_ONBOARDING_MAPPING,
                                e.message
                            )
                        )
                    } catch (e: Exception) {
                        Firelog.e(LOG_ONBOARDING_UNEXPECTED_MAPPING_ERROR, e)
                        CompleteProfileSetupResult.Generic(message = ERROR_ONBOARDING_UNEXPECTED_MAPPING)
                    }
                } else {
                    val generalMessage = apiResponse.message
                    val apiErrorDetails = apiResponse.error
                    
                    Firelog.e(
                        String.format(
                            LOG_ONBOARDING_API_NOT_SUCCESSFUL,
                            apiResponse.success,
                            generalMessage,
                            apiErrorDetails?.let { "Error Code: ${it.code}, Details: ${it.details}, Fields: ${it.fields}" }
                                ?: LOG_NO_API_ERROR_DETAILS
                        )
                    )
                    
                    if (apiErrorDetails == null) {
                        CompleteProfileSetupResult.Generic(
                            message = generalMessage ?: ERROR_ONBOARDING_UNKNOWN_API
                        )
                    } else {
                        val effectiveErrorMessage = apiErrorDetails.details ?: generalMessage
                        ?: String.format(
                            ERROR_ONBOARDING_WITH_CODE,
                            apiErrorDetails.code
                        )
                        
                        when (apiErrorDetails.code) {
                            API_ERROR_CODE_EMAIL_ALREADY_EXISTS -> {
                                CompleteProfileSetupResult.EmailAlreadyExists
                            }
                            
                            API_ERROR_CODE_VALIDATION_ERROR -> {
                                val validationErrors =
                                    apiErrorDetails.fields?.mapNotNull { (key, value) ->
                                        val fieldEnum = try {
                                            ProfileSetupField.valueOf(key.uppercase())
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
                                    CompleteProfileSetupResult.Validation(errors = validationErrors)
                                } else {
                                    CompleteProfileSetupResult.Generic(
                                        message = String.format(
                                            ERROR_VALIDATION_FAILED_NO_SPECIFIC_FIELDS,
                                            effectiveErrorMessage
                                        )
                                    )
                                }
                            }
                            // Add other onboarding specific API error codes here
                            else -> {
                                CompleteProfileSetupResult.Generic(
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
                        val errorResponseType = object : TypeToken<APIResponse<Any>>() {}.type
                        val errorResponse =
                            gson.fromJson<APIResponse<Any>>(errorBody, errorResponseType)
                        backendDrivenHttpErrorMessage =
                            errorResponse?.error?.details ?: errorResponse?.message
                    } catch (parseException: Exception) {
                        Firelog.w(
                            LOG_ERROR_BODY_PARSE_FAILURE_HTTP_EXCEPTION_ONBOARDING,
                            parseException
                        )
                    }
                }
                
                val httpErrorMessage = backendDrivenHttpErrorMessage ?: String.format(
                    ERROR_HTTP_GENERIC_ONBOARDING,
                    errorCode,
                    errorBody ?: e.message()
                )
                Firelog.e(String.format(LOG_ONBOARDING_HTTP_EXCEPTION, httpErrorMessage), e)
                
                // Handle HTTP status codes appropriately for onboarding
                when (errorCode) {
                    400 -> {
                        if (backendDrivenHttpErrorMessage?.contains(
                                "email",
                                ignoreCase = true
                            ) == true &&
                            backendDrivenHttpErrorMessage.contains("exists", ignoreCase = true)
                        ) {
                            CompleteProfileSetupResult.EmailAlreadyExists
                        } else {
                            CompleteProfileSetupResult.Generic(message = httpErrorMessage)
                        }
                    }
                    
                    401, 403 -> {
                        CompleteProfileSetupResult.Network(message = "Authentication error: $httpErrorMessage")
                    }
                    
                    409 -> {
                        CompleteProfileSetupResult.EmailAlreadyExists
                    }
                    
                    else -> CompleteProfileSetupResult.Network(message = httpErrorMessage)
                }
            } catch (e: IOException) {
                Firelog.e(LOG_ONBOARDING_IO_EXCEPTION, e)
                CompleteProfileSetupResult.Network(message = ERROR_IO_EXCEPTION_ONBOARDING)
            } catch (e: Exception) {
                Firelog.e(LOG_ONBOARDING_EXCEPTION, e)
                CompleteProfileSetupResult.Generic(
                    message = e.localizedMessage ?: ERROR_UNEXPECTED_ONBOARDING
                )
            }
        }
    }
    
    companion object {
        // --- Onboarding Specific Log Messages ---
        private const val LOG_ONBOARDING_MAPPING_ERROR =
            "Error mapping UserResponseDTO to User domain model for onboarding: %s"
        private const val LOG_ONBOARDING_UNEXPECTED_MAPPING_ERROR =
            "Unexpected error during user data mapping stage for onboarding."
        private const val LOG_ONBOARDING_API_NOT_SUCCESSFUL =
            "Complete Onboarding API call not successful. Success: %s, Message: %s, %s"
        private const val LOG_NO_API_ERROR_DETAILS = "No API error_details object provided."
        private const val LOG_UNKNOWN_VALIDATION_FIELD =
            "Unknown validation field from backend for onboarding: %s"
        private const val LOG_ERROR_BODY_PARSE_FAILURE_HTTP_EXCEPTION_ONBOARDING =
            "Could not parse error body as ApiResponse for Onboarding HttpException"
        private const val LOG_ONBOARDING_HTTP_EXCEPTION = "Onboarding HttpException - %s"
        private const val LOG_ONBOARDING_IO_EXCEPTION = "Onboarding IOException"
        private const val LOG_ONBOARDING_EXCEPTION = "Onboarding Exception"
        
        // --- Onboarding Specific Error Messages ---
        private const val ERROR_ONBOARDING_MAPPING =
            "Error processing your profile data from server. Details: %s"
        private const val ERROR_ONBOARDING_UNEXPECTED_MAPPING =
            "An unexpected error occurred while processing your profile data."
        private const val ERROR_ONBOARDING_UNKNOWN_API =
            "Failed to complete profile due to an unknown API error."
        private const val ERROR_ONBOARDING_WITH_CODE =
            "Failed to complete profile. Error code: %s"
        private const val ERROR_VALIDATION_FAILED_NO_SPECIFIC_FIELDS =
            "Validation failed. Server says: %s"
        private const val ERROR_HTTP_GENERIC_ONBOARDING = "HTTP %s: %s"
        private const val ERROR_IO_EXCEPTION_ONBOARDING =
            "Could not connect to the server to complete your profile. Please check your internet connection."
        private const val ERROR_UNEXPECTED_ONBOARDING =
            "An unexpected error occurred while completing your profile."
        
        // --- API Error Codes ---
        const val API_ERROR_CODE_EMAIL_ALREADY_EXISTS = "EMAIL_ALREADY_EXISTS"
        const val API_ERROR_CODE_VALIDATION_ERROR = "VALIDATION_ERROR"
    }
}
