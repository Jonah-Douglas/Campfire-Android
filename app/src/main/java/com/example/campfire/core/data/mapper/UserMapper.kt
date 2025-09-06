package com.example.campfire.core.data.mapper

import com.example.campfire.core.common.exception.MappingException
import com.example.campfire.core.common.logging.Firelog
import com.example.campfire.core.domain.model.User
import com.example.campfire.onboarding.profile_setup.data.remote.dto.response.UserProfileResponse
import javax.inject.Inject


class UserMapper @Inject constructor(
    private val dataTypeMapper: DataTypeMapper
) {
    
    /**
     * Maps a [UserProfileResponse] DTO to a [User] domain model.
     *
     * @param dto The [UserProfileResponse] object received from the data source.
     * @return The corresponding [User] domain model.
     * @throws MappingException if mandatory fields (like id, phone_number, created_at, updated_at)
     *   cannot be mapped from the DTO due to missing data or invalid format, or if nullable
     *   field transformations fail.
     */
    @Throws(MappingException::class)
    fun mapToDomain(dto: UserProfileResponse): User {
        Firelog.v("Attempting to map UserProfileResponse to User for id: ${dto.id}")
        
        try {
            // --- Mandatory DTO fields mapping to Non-Nullable Domain fields ---
            val phoneNumber = dataTypeMapper.mapMandatoryField(
                dtoValue = dto.phoneNumber,
                fieldName = FieldName.PHONE_NUMBER,
                transform = dataTypeMapper::mapStringToPhoneNumber
            )
            
            val createdAtDateTime = dataTypeMapper.mapMandatoryField(
                dtoValue = dto.createdAt,
                fieldName = FieldName.CREATED_AT,
                transform = dataTypeMapper::mapStringToLocalDateTime
            )
            
            val updatedAtDateTime = dataTypeMapper.mapMandatoryField(
                dtoValue = dto.updatedAt,
                fieldName = FieldName.UPDATED_AT,
                transform = dataTypeMapper::mapStringToLocalDateTime
            )
            
            // --- Nullable DTO fields mapping to Nullable Domain fields ---
            val domainEmail = dto.email?.let { emailString ->
                dataTypeMapper.mapNullableField(
                    dtoValue = emailString,
                    fieldName = FieldName.EMAIL,
                    transform = { nonNullEmail -> dataTypeMapper.mapToDomainEmail(nonNullEmail) }
                )
            }
            
            val domainDOB = dto.dateOfBirth?.let { dobString ->
                dataTypeMapper.mapNullableField(
                    dtoValue = dobString,
                    fieldName = FieldName.DATE_OF_BIRTH,
                    transform = dataTypeMapper::mapStringToLocalDate
                )
            }
            
            val domainLastLoginAt = dto.lastLoginAt?.let { lastLoginString ->
                dataTypeMapper.mapNullableField(
                    dtoValue = lastLoginString,
                    fieldName = FieldName.LAST_LOGIN_AT,
                    transform = dataTypeMapper::mapStringToLocalDateTime
                )
            }
            
            val user = User(
                id = dto.id,
                phone = phoneNumber,
                firstName = dto.firstName,
                lastName = dto.lastName,
                email = domainEmail,
                dateOfBirth = domainDOB,
                enableNotifications = dto.isEnableNotifications,
                isProfileComplete = dto.isProfileComplete,
                isAppSetupComplete = dto.isAppSetupComplete,
                isActive = dto.isActive,
                lastLoginAt = domainLastLoginAt,
                updatedAt = updatedAtDateTime,
                createdAt = createdAtDateTime,
            )
            
            Firelog.d("Successfully mapped UserResponse to User for id: ${user.id}")
            return user
        } catch (e: MappingException) {
            Firelog.e("Failed to map UserResponse to User for id: ${dto.id}", e)
            throw e
        } catch (e: Exception) {
            Firelog.e("Unexpected error during UserResponse mapping for id: ${dto.id}", e)
            throw MappingException(
                message = "Unexpected error during UserResponse mapping",
                fieldName = FieldName.FIELD_NAME_OVERALL,
                cause = e
            )
        }
    }
    
    private object FieldName {
        const val PHONE_NUMBER = "phone_number"
        const val EMAIL = "email"
        const val DATE_OF_BIRTH = "date_of_birth"
        const val LAST_LOGIN_AT = "last_login_at"
        const val CREATED_AT = "created_at"
        const val UPDATED_AT = "updated_at"
        const val FIELD_NAME_OVERALL = "UserProfileResponse_Overall"
    }
}