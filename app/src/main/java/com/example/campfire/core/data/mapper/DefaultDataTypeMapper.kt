package com.example.campfire.core.data.mapper

import com.example.campfire.core.common.exception.MappingException
import com.example.campfire.core.common.logging.Firelog
import com.example.campfire.core.common.validation.ValidationPatterns
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
 * This mapper handles common formats and scenarios:
 * - **Date Parsing:** Expects ISO-8601 local date format (YYYY-MM-DD).
 * - **DateTime Parsing:** Attempts to parse ISO_OFFSET_DATE_TIME, then ISO_ZONED_DATE_TIME,
 *   and finally ISO_LOCAL_DATE_TIME.
 * - **Phone Number Parsing:** Expects E.164 format for string inputs.
 * - **Email Validation:** Uses [ValidationPatterns.isValidEmail] for validation.
 *
 * **General Principles:**
 * - If the input string/object for a mapping function is `null` (or empty for some string inputs),
 *   the output will typically be `null`.
 * - If the input is non-null but cannot be validly converted (e.g., malformed date string,
 *   invalid email), a [MappingException] is thrown for string-to-object conversions,
 *   or a [DateTimeParseException] (or other relevant parsing exception) might be thrown
 *   directly by the underlying parsing logic if not caught and wrapped.
 *
 * This class is a [Singleton] and designed to be injected where data type
 * mapping is required.
 */
@Singleton
class DefaultDataTypeMapper @Inject constructor() : DataTypeMapper {
    
    /**
     * Formatter for ISO-8601 local date (e.g., "2023-10-27").
     */
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
     *   `null` if the input [date] is `null`.
     */
    override fun mapLocalDateToString(date: LocalDate?): String? {
        return date?.format(isoLocalDateFormatter)
    }
    
    // --- DateTime Mapping ---
    
    /**
     * Maps a string representation of a date-time to a [LocalDateTime] object.
     * This implementation attempts to parse the string using the following standard
     * [DateTimeFormatter]s in order:
     * 1. [DateTimeFormatter.ISO_OFFSET_DATE_TIME] (e.g., "2011-12-03T10:15:30+01:00")
     * 2. [DateTimeFormatter.ISO_ZONED_DATE_TIME] (e.g., "2011-12-03T10:15:30+01:00[Europe/Paris]")
     * 3. [DateTimeFormatter.ISO_LOCAL_DATE_TIME] (e.g., "2011-12-03T10:15:30")
     *
     * The time component is extracted to [LocalDateTime], effectively ignoring offset/zone
     * information for the final result if present in the input string.
     *
     * @param dateTimeString The string representation of the date-time to parse.
     *   If null or empty, returns `null`.
     * @return The parsed [LocalDateTime] object, or `null` if the input [dateTimeString] is null or empty.
     * @throws DateTimeParseException if the [dateTimeString] is non-null/empty and cannot be parsed
     *   by any of the attempted ISO formats. The exception from the final attempt
     *   ([DateTimeFormatter.ISO_LOCAL_DATE_TIME]) will be thrown.
     */
    override fun mapStringToLocalDateTime(dateTimeString: String?): LocalDateTime? {
        if (dateTimeString.isNullOrEmpty()) return null
        
        val input = dateTimeString
        try {
            // Attempt 1: Parse as OffsetDateTime
            try {
                val offsetDateTime =
                    OffsetDateTime.parse(input, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                Firelog.v("Parsed '$input' as ISO_OFFSET_DATE_TIME")
                return offsetDateTime.toLocalDateTime()
            } catch (e: DateTimeParseException) {
                Firelog.v("Failed to parse '$input' as ISO_OFFSET_DATE_TIME, trying ISO_ZONED_DATE_TIME. Error: ${e.message}")
                // Attempt 2: Parse as ZonedDateTime
                try {
                    val zonedDateTime =
                        ZonedDateTime.parse(input, DateTimeFormatter.ISO_ZONED_DATE_TIME)
                    Firelog.v("Parsed '$input' as ISO_ZONED_DATE_TIME")
                    return zonedDateTime.toLocalDateTime()
                } catch (e2: DateTimeParseException) {
                    Firelog.v("Failed to parse '$input' as ISO_ZONED_DATE_TIME, trying ISO_LOCAL_DATE_TIME. Error: ${e2.message}")
                    // Attempt 3: Final fallback to parse as LocalDateTime (no zone/offset)
                    try {
                        val localDateTime =
                            LocalDateTime.parse(input, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        Firelog.v("Parsed '$input' as ISO_LOCAL_DATE_TIME")
                        return localDateTime
                    } catch (e3: DateTimeParseException) {
                        Firelog.w(
                            "DataTypeMapper: Failed to parse string to LocalDateTime after multiple attempts: $input. Final error: ${e3.message}",
                            e3
                        )
                        throw e3 // Re-throw the exception from the last attempt
                    }
                }
            }
        } catch (e: DateTimeParseException) {
            Firelog.w(
                "DataTypeMapper: Unhandled DateTimeParseException for: $input. This path should ideally not be hit if inner catches rethrow.",
                e
            )
            throw e // Re-throw if somehow reached
        }
    }
    
    /**
     * Maps a [LocalDateTime] object to its string representation using [DateTimeFormatter.ISO_LOCAL_DATE_TIME].
     * (e.g., "2011-12-03T10:15:30").
     *
     * @param dateTime The [LocalDateTime] object to format.
     * @return The ISO_LOCAL_DATE_TIME formatted string, or `null` if the input [dateTime] is null.
     */
    override fun mapLocalDateTimeToString(dateTime: LocalDateTime?): String? {
        return dateTime?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
    
    // --- Phone Number Mapping ---
    
    /**
     * Maps an E.164 formatted phone string to a [PhoneNumber] domain object.
     *
     * @param phoneString The E.164 phone string to parse (e.g., "+16502530000").
     * @return A [PhoneNumber] object if the string is valid and non-null,
     *   `null` if the input [phoneString] is `null`.
     * @throws MappingException if the [phoneString] is non-null but not a valid E.164 format
     *   or cannot be parsed into a [PhoneNumber].
     */
    override fun mapStringToPhoneNumber(phoneString: String?): PhoneNumber? {
        return phoneString?.let {
            val nonNullPhoneString = it
            try {
                PhoneNumber.fromE164(nonNullPhoneString)
            } catch (e: Exception) {
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
     *   and has a valid E.164 representation, `null` otherwise (e.g., if [phoneNumber] itself is `null`
     *   or if `e164Format` is `null`).
     */
    override fun mapPhoneNumberToString(phoneNumber: PhoneNumber?): String? {
        return phoneNumber?.e164Format
    }
    
    // --- Email Mapping ---
    
    /**
     * Maps an email string to a validated and trimmed domain email string using
     * Android's [android.util.Patterns.EMAIL_ADDRESS].
     *
     * @param emailString The email string to validate. If null or empty, returns `null`.
     * @return A trimmed, valid email string if the input [emailString] is non-null/empty and
     *   matches [android.util.Patterns.EMAIL_ADDRESS]; `null` if the input is `null` or empty.
     * @throws MappingException if the [emailString] is non-null/empty but determined to be
     *   an invalid email format.
     */
    override fun mapToDomainEmail(emailString: String?): String? {
        return emailString?.let { // If emailString is null, this block is skipped, returns null
            val trimmedEmail = it.trim()
            if (trimmedEmail.isNotEmpty() && ValidationPatterns.isValidEmail(trimmedEmail)) {
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
     * In this default implementation, this is a pass-through operation, returning the
     * original email string unchanged.
     *
     * @param domainEmail The email string from the domain model.
     * @return The email string suitable for DTOs, identical to the input [domainEmail].
     *   Returns `null` if [domainEmail] is `null`.
     */
    override fun mapEmailToDTOFormat(domainEmail: String?): String? {
        return domainEmail
    }
    
    /**
     * Maps a DTO value that is considered mandatory for the domain model.
     * If the transformation performed by the [transform] lambda results in `null`
     * (either because the input [dtoValue] was null and the lambda couldn't produce
     * a default, or the transformation logic itself determined the input was unsuitable
     * for a non-nullable target), this function throws a [MappingException].
     *
     * This utility helps enforce data integrity by ensuring that critical fields
     * are successfully mapped to non-nullable domain types.
     *
     * @param T The type of the DTO field's value (e.g., `String?`).
     * @param R The non-nullable type of the resulting domain field (e.g., `PhoneNumber`).
     * @param dtoValue The value from the DTO (or other source).
     * @param fieldName A descriptive name of the field being mapped. This is included
     *   in the [MappingException] message if an error occurs.
     * @param transform A lambda function that takes the nullable [dtoValue] (`T?`)
     *   and attempts to transform it into the domain type (`R?`).
     *   It should return `null` if the transformation is not possible
     *   or if the input is unsuitable for a non-nullable target.
     * @return The successfully transformed, non-nullable domain value (`R`).
     * @throws MappingException if the [transform] function returns `null`.
     */
    @Throws(MappingException::class)
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
    
    /**
     * Maps a potentially nullable DTO value to a **nullable** domain model field,
     * handling potential transformation errors.
     *
     * - If the [dtoValue] is `null`, this function returns `null` immediately without
     *   attempting the transformation.
     * - If the [dtoValue] is non-null:
     *     - The [transform] lambda is applied to the (now non-null) `dtoValue`.
     *     - If the `transform` is successful, its result (which can be `null` if the
     *       logic within `transform` determines it so) is returned.
     *     - If the `transform` lambda throws any [Exception] (e.g., a parsing error
     *       because a non-null input is malformed), this function catches the exception,
     *       logs a warning, and throws a new [MappingException].
     *
     * This is useful for optional DTO fields that map to optional domain fields,
     * where a failure to transform a non-null DTO value (due to format issues, etc.)
     * should result in a mapping error rather than silently returning `null`.
     *
     * @param T The type of the DTO field's value (e.g., `String?`).
     * @param R The type of the resulting nullable domain model field (e.g., `LocalDate?`).
     * @param dtoValue The potentially nullable value from the DTO.
     * @param fieldName A descriptive name of the field being mapped. This is included
     *                  in logging messages and the [MappingException] if an error occurs.
     * @param transform A lambda function that takes a **non-null** [dtoValue] (type `T`)
     *                  and attempts to transform it into a nullable domain value (type `R?`).
     *                  This transform lambda may throw an exception if the input `T`
     *                  cannot be processed.
     * @return The transformed, nullable domain model value (`R?`), or `null` if the
     *         input [dtoValue] was `null`.
     * @throws MappingException if the [transform] function throws an [Exception] for a
     *                          non-null [dtoValue], indicating a failure to map the field.
     */
    @Throws(MappingException::class)
    override fun <T, R> mapNullableField(
        dtoValue: T?,
        fieldName: String,
        transform: (T) -> R?
    ): R? {
        return dtoValue?.let {
            try {
                transform(it)
            } catch (_: Exception) {
                String.format(ERROR_NULLABLE_FIELD_UNMAPPABLE, fieldName, dtoValue)
                Firelog.w(String.format(LOG_NULLABLE_FIELD_FAILURE, fieldName, dtoValue))
                throw MappingException(
                    message = "Failed to map nullable field '$fieldName'. Input: $it",
                    fieldName = fieldName,
                )
            }
        }
    }
    
    /**
     * Internal object to hold constants for field names used in [MappingException].
     */
    private object FieldName {
        const val PHONE = "phoneNumber"
        const val DATE = "date"
        const val EMAIL = "email"
    }
    
    /**
     * Companion object holding constants for error messages and log messages.
     */
    companion object {
        // Error Messages for MappingException
        private const val ERROR_MANDATORY_FIELD_UNMAPPABLE =
            "Required field '%s' could not be mapped from DTO value: '%s'"
        private const val ERROR_NULLABLE_FIELD_UNMAPPABLE =
            "Nullable field '%s' could not be mapped from DTO value: '%s'"
        private const val ERROR_PHONE_STRING_UNMAPPABLE =
            "Invalid phone string format: '%s'. Expected E.164 format."
        private const val ERROR_DATE_STRING_UNMAPPABLE =
            "Failed to parse date string: '%s'. Expected ISO_LOCAL_DATE (YYYY-MM-DD). Underlying error: '%s'"
        private const val ERROR_EMAIL_STRING_UNMAPPABLE =
            "Invalid email string format: '%s'."
        
        // Log Messages
        private const val LOG_MANDATORY_FIELD_FAILURE =
            "Mandatory field mapping failed for '%s'. DTO value: '%s'. Throwing MappingException."
        private const val LOG_NULLABLE_FIELD_FAILURE =
            "Nullable field mapping failed for '%s'. DTO value: '%s'. Throwing MappingException."
        private const val LOG_PHONE_STRING_FAILURE =
            "Failed to parse phone string: '%s'. Error: '%s'"
        private const val LOG_DATE_STRING_FAILURE =
            "Invalid date string format: '%s'. Expected ISO_LOCAL_DATE (YYYY-MM-DD)."
        private const val LOG_EMAIL_STRING_FAILURE =
            "Invalid email format: '%s' (original: '%s')"
    }
}
