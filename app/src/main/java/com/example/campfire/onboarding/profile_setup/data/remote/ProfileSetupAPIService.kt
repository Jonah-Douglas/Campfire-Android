package com.example.campfire.onboarding.profile_setup.data.remote

import com.example.campfire.auth.data.remote.dto.response.UserResponse
import com.example.campfire.core.data.remote.dto.response.APIResponse
import com.example.campfire.onboarding.profile_setup.data.remote.dto.request.CompleteOnboardingProfileRequest
import retrofit2.http.Body
import retrofit2.http.POST


/**
 * Service dedicated to completing the user profile onboarding process.
 */
interface ProfileSetupAPIService {
    
    /**
     * Finishes the base user account setup.
     * On success, returns an ApiResponse containing UserResponse in its data field.
     */
    @POST("/me/complete-profile")
    suspend fun completeOnboardingProfile(@Body request: CompleteOnboardingProfileRequest): APIResponse<UserResponse>
}