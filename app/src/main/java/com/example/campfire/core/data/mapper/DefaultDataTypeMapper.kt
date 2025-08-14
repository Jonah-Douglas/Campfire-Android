package com.example.campfire.core.data.mapper

import android.util.Patterns
import com.example.campfire.core.common.exception.MappingException
import com.example.campfire.core.common.logging.Firelog
import com.example.campfire.core.domain.model.PhoneNumber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Default implementation of [DataTypeMapper] providing common data type conversions
 * between DTO string representations and domain model types.
 *
 * This mapper generally follows the principle:
 * - If the input is `null`, the output is `null`.
 * - If the input is non-null but cannot be validly converted, a [MappingException] is thrown.
 */
@Singleton
class DefaultDataTypeMapper @Inject constructor() : DataTypeMapper {
    
    private val isoLocalDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    
    /**
     * Maps an ISO-8601 date string to a [LocalDate] object.
     *
     * @param dateString The date string to parse (e.g., "2023-10-27").
     * @return A [LocalDate] object if the string is valid and non-null,
     *         `null` if the input [dateString] is `null`.
     * @throws MappingException if the [dateString] is non-null but not a valid ISO-8601 date format.
     */
    override fun mapStringToLocalDate(dateString: String?): LocalDate? {
        return dateString?.let { // If dateString is null, this block is skipped, returns null
            val nonNullDateString = it
            try {
                LocalDate.parse(nonNullDateString, isoLocalDateFormatter)
            } catch (e: DateTimeParseException) {
                Firelog.w(String.format(LOG_DATE_STRING_FAILURE, nonNullDateString))
                throw MappingException(
                    message = String.format(
                        ERROR_DATE_STRING_UNMAPPABLE,
                        nonNullDateString,
                        e.message
                    ),
                    cause = e,
                    fieldName = FieldName.DATE
                )
            }
        }
    }
    
    /**
     * Maps a [LocalDate] object to an ISO-8601 formatted date string.
     *
     * @param date The [LocalDate] object to format.
     * @return An ISO-8601 date string (e.g., "2023-10-27") if the input [date] is non-null,
     *         `null` if the input [date] is `null`.
     */
    override fun mapLocalDateToString(date: LocalDate?): String? {
        return date?.format(isoLocalDateFormatter)
    }
    
    override fun mapStringToLocalDateTime(dateTimeString: String?): LocalDateTime? {
        return dateTimeString?.let {
            try {
                try {
                    val offsetDateTime =
                        OffsetDateTime.parse(it, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    return offsetDateTime.toLocalDateTime()
                } catch (e: DateTimeParseException) {
                    // If no offset, try parsing directly as LocalDateTime (e.g., "2023-11-20T10:30:00")
                    try {
                        val zonedDateTime =
                            ZonedDateTime.parse(it, DateTimeFormatter.ISO_ZONED_DATE_TIME)
                        return zonedDateTime.toLocalDateTime()
                    } catch (e2: DateTimeParseException) {
                        // Final fallback for local date-time without zone/offset
                        try {
                            return LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        } catch (e3: DateTimeParseException) {
                            Firelog.w(
                                "DataTypeMapper: Failed to parse string to LocalDateTime after multiple attempts: $it",
                                e3
                            )
                            throw e3
                        }
                    }
                }
            } catch (e: DateTimeParseException) {
                Firelog.w("DataTypeMapper: Failed to parse string to LocalDateTime: $it", e)
                throw e
            }
        }
    }
    
    override fun mapLocalDateTimeToString(dateTime: LocalDateTime?): String? {
        return dateTime?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
    
    /**
     * Maps an E.164 formatted phone string to a [PhoneNumber] domain object.
     *
     * @param phoneString The E.164 phone string to parse (e.g., "+16502530000").
     * @return A [PhoneNumber] object if the string is valid and non-null,
     *         `null` if the input [phoneString] is `null`.
     * @throws MappingException if the [phoneString] is non-null but not a valid E.164 format
     *                          or cannot be parsed into a [PhoneNumber].
     */
    override fun mapStringToPhoneNumber(phoneString: String?): PhoneNumber? {
        return phoneString?.let {
            val nonNullPhoneString = it
            try {
                PhoneNumber.fromE164(nonNullPhoneString)
            } catch (e: Exception) { // Catch a broader exception if PhoneNumber.fromE164 throws various types
                Firelog.w(String.format(LOG_PHONE_STRING_FAILURE, nonNullPhoneString, e.message))
                throw MappingException(
                    message = String.format(ERROR_PHONE_STRING_UNMAPPABLE, nonNullPhoneString),
                    cause = e,
                    fieldName = FieldName.PHONE
                )
            }
        }
    }
    
    /**
     * Maps a [PhoneNumber] domain object to its E.164 string representation.
     *
     * @param phoneNumber The [PhoneNumber] object.
     * @return The E.164 formatted phone string (e.g., "+16502530000") if [phoneNumber] is non-null
     *         and has a valid E.164 representation, `null` otherwise (e.g., if [phoneNumber] itself is `null`
     *         or if `e164Format` is `null`).
     */
    override fun mapPhoneNumberToString(phoneNumber: PhoneNumber?): String? {
        return phoneNumber?.e164Format
    }
    
    /**
     * Maps an email string to a validated and trimmed domain email string.
     *
     * @param emailString The email string to validate.
     * @return A trimmed, valid email string if the input [emailString] is non-null and valid,
     *         `null` if the input [emailString] is `null`.
     * @throws MappingException if the [emailString] is non-null but determined to be an invalid email format.
     */
    override fun mapToDomainEmail(emailString: String?): String? {
        return emailString?.let { // If emailString is null, this block is skipped, returns null
            val trimmedEmail = it.trim()
            if (trimmedEmail.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(trimmedEmail)
                    .matches()
            ) {
                trimmedEmail
            } else {
                Firelog.w(String.format(LOG_EMAIL_STRING_FAILURE, trimmedEmail, it))
                throw MappingException(
                    message = String.format(ERROR_EMAIL_STRING_UNMAPPABLE, trimmedEmail),
                    fieldName = FieldName.EMAIL
                )
            }
        }
    }
    
    /**
     * Maps a domain email string to a DTO-compatible email string.
     * Typically, this involves no transformation unless specific DTO formatting is required.
     *
     * @param domainEmail The email string from the domain model.
     * @return The email string suitable for DTOs, usually identical to the input.
     *         Returns `null` if [domainEmail] is `null`.
     */
    override fun mapEmailToDTOFormat(domainEmail: String?): String? {
        return domainEmail // Usually no change unless DTO needs specific casing etc.
    }
    
    /**
     * Maps a DTO value that is considered mandatory for the domain model.
     * If the transformation results in null (either because the input was null
     * or the transformation failed for a non-nullable target), it throws a MappingException.
     *
     * @param T The type of the DTO field's value (e.g., String?).
     * @param R The non-nullable type of the resulting domain field (e.g., PhoneNumber).
     * @param dtoValue The value from the DTO.
     * @param fieldName A descriptive name of the field being mapped (for error messages).
     * @param transform A lambda function that takes the DTO value and attempts to transform it
     *                  into the domain type. It should return null if the transformation is not possible
     *                  or if the input is unsuitable for a non-nullable target.
     * @return The successfully transformed, non-nullable domain value.
     * @throws MappingException if the transformation returns null.
     */
    override fun <T, R : Any> mapMandatoryField(
        dtoValue: T?,
        fieldName: String,
        transform: (T?) -> R?
    ): R {
        return transform(dtoValue) ?: run {
            val errorMessage =
                String.format(ERROR_MANDATORY_FIELD_UNMAPPABLE, fieldName, dtoValue ?: "null")
            Firelog.e(String.format(LOG_MANDATORY_FIELD_FAILURE, fieldName, dtoValue ?: "null"))
            throw MappingException(
                message = errorMessage,
                fieldName = fieldName
            )
        }
    }
    
    private object FieldName {
        const val PHONE = "phoneNumber"
        const val DATE = "date"
        const val EMAIL = "email"
    }
    
    companion object {
        private const val ERROR_MANDATORY_FIELD_UNMAPPABLE =
            "Required field '%s' could not be mapped from DTO value: '%s'"
        private const val ERROR_PHONE_STRING_UNMAPPABLE =
            "Invalid phone string format: '%s'. Expected E.164 format."
        private const val ERROR_DATE_STRING_UNMAPPABLE =
            "Failed to parse date string: '%s'. Expected ISO_LOCAL_DATE (YYYY-MM-DD). Underlying error: '%s'"
        private const val ERROR_EMAIL_STRING_UNMAPPABLE =
            "Invalid email string format: '%s'."
        
        // Logging
        private const val LOG_MANDATORY_FIELD_FAILURE =
            "Mandatory field mapping failed for '%s'. DTO value: '%s'. Throwing MappingException."
        private const val LOG_PHONE_STRING_FAILURE =
            "Failed to parse phone string: '%s'. Error: '%s'"
        private const val LOG_DATE_STRING_FAILURE =
            "Invalid date string format: '%s'. Expected ISO_LOCAL_DATE (YYYY-MM-DD)."
        private const val LOG_EMAIL_STRING_FAILURE =
            "Invalid email format: '%s' (original: '%s')"
    }
}
