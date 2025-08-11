package com.example.campfire.core.data.mapper

import com.example.campfire.core.common.exception.MappingException
import com.example.campfire.core.domain.model.PhoneNumber
import java.time.LocalDate


interface DataTypeMapper {
    
    // --- Date Mapping ---
    fun mapStringToLocalDate(dateString: String?): LocalDate?
    fun mapLocalDateToString(date: LocalDate?): String?
    
    // --- Phone Number Mapping ---
    fun mapStringToPhoneNumber(phoneString: String?): PhoneNumber?
    fun mapPhoneNumberToString(phoneNumber: PhoneNumber?): String? // Typically E.164
    
    // --- Email Mapping ---
    fun mapToDomainEmail(emailString: String?): String? // Returns a validated/normalized email or throws
    fun mapEmailToDTOFormat(domainEmail: String?): String?
    
    @Throws(MappingException::class)
    fun <T, R : Any> mapMandatoryField(
        dtoValue: T?,
        fieldName: String,
        transform: (T?) -> R?
    ): R
}