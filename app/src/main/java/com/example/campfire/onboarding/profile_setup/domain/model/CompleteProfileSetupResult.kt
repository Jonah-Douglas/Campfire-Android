package com.example.campfire.onboarding.profile_setup.domain.model

import com.example.campfire.core.domain.model.User


sealed interface CompleteProfileSetupResult {
    data class Success(val user: User) : CompleteProfileSetupResult
    data class Validation(val errors: Map<ProfileSetupField, String>) : CompleteProfileSetupResult
    data object EmailAlreadyExists : CompleteProfileSetupResult
    data class Network(val message: String? = null) : CompleteProfileSetupResult
    data class Generic(val code: Int? = null, val message: String? = null) :
        CompleteProfileSetupResult
}