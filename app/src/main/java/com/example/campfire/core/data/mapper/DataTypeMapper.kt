package com.example.campfire.core.data.mapper

import com.example.campfire.core.common.exception.MappingException
import com.example.campfire.core.domain.model.PhoneNumber
import java.time.LocalDate
import java.time.LocalDateTime


interface DataTypeMapper {
    
    // --- Date Mapping ---
    fun mapStringToLocalDate(dateString: String?): LocalDate?
    fun mapLocalDateToString(date: LocalDate?): String?
    
    // --- DateTime Mapping ---
    /**
     * Maps a string representation of a date-time to a LocalDateTime object.
     * Implementations should handle common ISO-8601 formats.
     *
     * @param dateTimeString The string to parse. Can be null.
     * @return The parsed LocalDateTime object, or null if the input string is null.
     * @throws java.time.format.DateTimeParseException if the string is non-null and cannot be parsed.
     */
    fun mapStringToLocalDateTime(dateTimeString: String?): LocalDateTime?
    
    /**
     * Maps a LocalDateTime object to its string representation.
     * Implementations should typically use a standard ISO-8601 format.
     *
     * @param dateTime The LocalDateTime object to format. Can be null.
     * @return The string representation of the date-time, or null if the input is null.
     */
    fun mapLocalDateTimeToString(dateTime: LocalDateTime?): String?
    
    // --- Phone Number Mapping ---
    fun mapStringToPhoneNumber(phoneString: String?): PhoneNumber?
    fun mapPhoneNumberToString(phoneNumber: PhoneNumber?): String?
    
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