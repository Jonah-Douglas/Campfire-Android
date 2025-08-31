package com.example.campfire.onboarding.profile_setup.domain.usecase

import com.example.campfire.core.common.validation.ValidationPatterns
import com.example.campfire.onboarding.profile_setup.domain.model.CompleteProfileSetupResult
import com.example.campfire.onboarding.profile_setup.domain.model.ProfileSetupField
import com.example.campfire.onboarding.profile_setup.domain.repository.ProfileSetupRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Use case for completing a user's profile after initial phone verification.
 * Validates user details and calls the repository to update the profile.
 */
class CompleteProfileSetupUseCase @Inject constructor(
    private val profileSetupRepository: ProfileSetupRepository
) {
    
    suspend operator fun invoke(
        firstName: String,
        lastName: String,
        email: String,
        dateOfBirth: LocalDate?,
        enableNotifications: Boolean
    ): CompleteProfileSetupResult {
        
        val validationErrors = mutableMapOf<ProfileSetupField, String>()
        
        // 1. Validate First Name
        if (firstName.isBlank()) {
            validationErrors[ProfileSetupField.FIRST_NAME] = ERROR_FIRST_NAME_EMPTY
        }
        
        // 2. Validate Last Name
        if (lastName.isBlank()) {
            validationErrors[ProfileSetupField.LAST_NAME] = ERROR_LAST_NAME_EMPTY
        }
        
        // 3. Validate Email
        if (email.isBlank()) {
            validationErrors[ProfileSetupField.EMAIL] = ERROR_EMAIL_EMPTY
        } else if (!ValidationPatterns.isValidEmail(email.trim())) {
            validationErrors[ProfileSetupField.EMAIL] = ERROR_INVALID_EMAIL
        }
        
        // 4. Validate Date of Birth
        if (dateOfBirth == null) {
            validationErrors[ProfileSetupField.DATE_OF_BIRTH] = ERROR_DATE_OF_BIRTH_EMPTY
        } else {
            val today = LocalDate.now()
            
            if (dateOfBirth.isAfter(today.minusYears(MIN_AGE))) {
                validationErrors[ProfileSetupField.DATE_OF_BIRTH] =
                    String.format(ERROR_BELOW_MIN_AGE, MIN_AGE)
            } else if (dateOfBirth.isBefore(today.minusYears(MAX_AGE))) {
                validationErrors[ProfileSetupField.DATE_OF_BIRTH] =
                    String.format(ERROR_OVER_MAX_AGE, MAX_AGE)
            }
        }
        
        // If there are any validation errors, return them
        if (validationErrors.isNotEmpty()) {
            return CompleteProfileSetupResult.Validation(validationErrors)
        }
        
        // Call the repository to complete the profile
        val result = profileSetupRepository.completeProfileSetup(
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            email = email.trim(),
            dateOfBirth = dateOfBirth!!,
            enableNotifications = enableNotifications
        )
        
        return result
    }
    
    companion object {
        private const val MIN_AGE: Long = 18
        private const val MAX_AGE: Long = 150
        
        // Errors
        private const val ERROR_FIRST_NAME_EMPTY =
            "First name cannot be empty."
        private const val ERROR_LAST_NAME_EMPTY =
            "Last name cannot be empty."
        private const val ERROR_EMAIL_EMPTY =
            "Email cannot be empty."
        private const val ERROR_DATE_OF_BIRTH_EMPTY =
            "Date of birth is required."
        private const val ERROR_INVALID_EMAIL =
            "Please enter a valid email address."
        private const val ERROR_BELOW_MIN_AGE =
            "You must be at least '%s' years old."
        private const val ERROR_OVER_MAX_AGE =
            "Date of birth seems incorrect (older than '%s' years)."
    }
}