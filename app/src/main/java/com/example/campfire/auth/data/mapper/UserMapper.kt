package com.example.campfire.auth.data.mapper

import com.example.campfire.auth.data.remote.dto.response.UserResponse
import com.example.campfire.auth.domain.model.User
import com.example.campfire.core.common.exception.MappingException
import com.example.campfire.core.common.logging.Firelog
import com.example.campfire.core.data.mapper.DataTypeMapper
import javax.inject.Inject


class UserMapper @Inject constructor(
    private val dataTypeMapper: DataTypeMapper
) {
    
    /**
     * Maps a [UserResponse] DTO to a [User] domain model.
     *
     * @param dto The [UserResponse] object received from the data source.
     * @return The corresponding [User] domain model.
     * @throws MappingException if mandatory fields (like phone, dateOfBirth, email)
     *   cannot be mapped from the DTO due to invalid format or missing data,
     *   as handled by [DataTypeMapper.mapMandatoryField].
     */
    @Throws(MappingException::class)
    fun mapToDomain(dto: UserResponse): User {
        Firelog.v("Mapping UserResponse to User")
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
        
        val createdAtDate = dataTypeMapper.mapMandatoryField(
            dtoValue = dto.createdAt,
            fieldName = FieldName.CREATED_AT,
            transform = dataTypeMapper::mapStringToLocalDate
        )
        
        val updatedAtDate = dataTypeMapper.mapMandatoryField(
            dtoValue = dto.updatedAt,
            fieldName = FieldName.UPDATED_AT,
            transform = dataTypeMapper::mapStringToLocalDate
        )
        
        return User(
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
            createdAt = createdAtDate,
            updatedAt = updatedAtDate
        )
    }
    
    private object FieldName {
        const val PHONE = "phone"
        const val DATE_OF_BIRTH = "dateOfBirth"
        const val EMAIL = "email"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
    }
}

