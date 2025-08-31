package com.example.campfire.core.data.mapper

import com.example.campfire.auth.data.remote.dto.response.UserResponse
import com.example.campfire.core.common.exception.MappingException
import com.example.campfire.core.common.logging.Firelog
import com.example.campfire.core.domain.model.User
import javax.inject.Inject


class UserMapper @Inject constructor(
    private val dataTypeMapper: DataTypeMapper
) {
    
    /**
     * Maps a [com.example.campfire.auth.data.remote.dto.response.UserResponse] DTO to a [com.example.campfire.core.domain.model.User] domain model.
     *
     * @param dto The [com.example.campfire.auth.data.remote.dto.response.UserResponse] object received from the data source.
     * @return The corresponding [com.example.campfire.core.domain.model.User] domain model.
     * @throws com.example.campfire.core.common.exception.MappingException if mandatory fields (like phone, dateOfBirth, email)
     *   cannot be mapped from the DTO due to invalid format or missing data,
     *   as handled by [DataTypeMapper.mapMandatoryField].
     */
    @Throws(MappingException::class)
    fun mapToDomain(dto: UserResponse): User {
        Firelog.v("Attempting to map UserResponse to User for id: ${dto.id}")
        
        try {
            val phoneNumber = dataTypeMapper.mapMandatoryField(
                dtoValue = dto.phone,
                fieldName = FieldName.PHONE,
                transform = dataTypeMapper::mapStringToPhoneNumber
            )
            
            val dob = dataTypeMapper.mapMandatoryField(
                dtoValue = dto.dateOfBirth,
                fieldName = FieldName.DATE_OF_BIRTH,
                transform = dataTypeMapper::mapStringToLocalDate
            )
            
            val email = dataTypeMapper.mapMandatoryField(
                dtoValue = dto.email,
                fieldName = FieldName.EMAIL,
                transform = dataTypeMapper::mapToDomainEmail
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
            
            val user = User(
                id = dto.id,
                phone = phoneNumber,
                firstName = dto.firstName,
                lastName = dto.lastName,
                email = email,
                dateOfBirth = dob,
                enableNotifications = dto.enableNotifications,
                isProfileComplete = dto.isProfileComplete,
                isActive = dto.isActive,
                isSuperuser = dto.isSuperuser,
                createdAt = createdAtDateTime,
                updatedAt = updatedAtDateTime
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
        const val PHONE = "phone"
        const val DATE_OF_BIRTH = "dateOfBirth"
        const val EMAIL = "email"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
        const val FIELD_NAME_OVERALL = "UserResponse_Overall"
    }
}