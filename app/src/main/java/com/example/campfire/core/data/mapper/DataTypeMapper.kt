package com.example.campfire.core.data.mapper

import com.example.campfire.core.common.exception.MappingException
import com.example.campfire.core.domain.model.PhoneNumber
import java.time.LocalDate
import java.time.LocalDateTime


/**
 * Defines a contract for mapping various data types commonly encountered
 * when transforming data between layers (e.g., DTOs to Domain models,
 * Domain models to DTOs, or parsing string inputs).
 *
 * Implementations of this interface provide concrete logic for these conversions.
 */
interface DataTypeMapper {
    
    // --- Date Mapping ---
    
    /**
     * Maps a string representation of a date to a [LocalDate] object.
     * Implementations should define the expected date format(s) they can parse.
     *
     * @param dateString The string representation of the date. Can be null.
     * @return The parsed [LocalDate] object, or null if the input string is null or empty.
     * @throws java.time.format.DateTimeParseException if the [dateString] is non-null, non-empty, and cannot be parsed
     *                                according to the implementation's expected format.
     */
    fun mapStringToLocalDate(dateString: String?): LocalDate?
    
    
    /**
     * Maps a [LocalDate] object to its string representation.
     * Implementations should define the output date format.
     *
     * @param date The [LocalDate] object to format. Can be null.
     * @return The string representation of the date, or null if the input [date] is null.
     */
    fun mapLocalDateToString(date: LocalDate?): String?
    
    // --- DateTime Mapping ---
    
    /**
     * Maps a string representation of a date-time to a [LocalDateTime] object.
     * Implementations should handle common ISO-8601 formats but may support others.
     * It's recommended that implementations clearly document the primary format they expect.
     *
     * @param dateTimeString The string representation of the date-time to parse. Can be null.
     * @return The parsed [LocalDateTime] object, or null if the input [dateTimeString] is null or empty.
     * @throws java.time.format.DateTimeParseException if the [dateTimeString] is non-null, non-empty,
     * and cannot be parsed.
     */
    fun mapStringToLocalDateTime(dateTimeString: String?): LocalDateTime?
    
    /**
     * Maps a [LocalDateTime] object to its string representation.
     * Implementations should typically use a standard ISO-8601 format.
     *
     * @param dateTime The [LocalDateTime] object to format. Can be null.
     * @return The string representation of the date-time, or null if the input [dateTime] is null.
     */
    fun mapLocalDateTimeToString(dateTime: LocalDateTime?): String?
    
    // --- Phone Number Mapping ---
    
    /**
     * Maps a string representation of a phone number to a [PhoneNumber] domain model.
     * Implementations should handle various common phone number formats if possible,
     * or clearly document the expected input format (e.g., E.164).
     *
     * @param phoneString The string representation of the phone number. Can be null.
     * @return A [PhoneNumber] object if parsing is successful and the number is considered
     *         valid by the implementation, or null if the input [phoneString] is null, empty,
     *         or represents an invalid/unparseable phone number.
     */
    fun mapStringToPhoneNumber(phoneString: String?): PhoneNumber?
    
    /**
     * Maps a [PhoneNumber] domain model to its string representation.
     * Implementations should define the output format (e.g., E.164, national format).
     *
     * @param phoneNumber The [PhoneNumber] object to format. Can be null.
     * @return The string representation of the phone number, or null if the input [phoneNumber] is null.
     */
    fun mapPhoneNumberToString(phoneNumber: PhoneNumber?): String?
    
    // --- Email Mapping ---
    
    /**
     * Maps an email string (potentially from a DTO or raw input) to a validated
     * domain representation of an email.
     * This might involve validation or minor canonicalization (e.g., lowercasing).
     *
     * @param emailString The email string to process. Can be null.
     * @return A string representing the domain email if the input is valid and non-null/empty,
     *         otherwise null. Implementations might return the original string if valid
     *         or a canonicalized version.
     */
    fun mapToDomainEmail(emailString: String?): String?
    
    /**
     * Maps a domain email string to a format suitable for DTOs or network requests.
     * This might involve ensuring a specific format or simply passing it through if no
     * transformation is needed from the domain model's representation.
     *
     * @param domainEmail The domain email string. Can be null.
     * @return The email string formatted for DTO/network use, or null if the input is null.
     */
    fun mapEmailToDTOFormat(domainEmail: String?): String?
    
    /**
     * A utility function to map a potentially nullable DTO value to a non-nullable
     * domain model field. If the transformation results in null (either because the
     * input was null and couldn't be transformed to a default, or the transform
     * logic itself yields null for a non-null input that's considered invalid for
     * a mandatory field), this function throws a [MappingException].
     *
     * This is useful for enforcing that certain fields in a domain model are always present
     * if their corresponding DTO field (or the source of data) was expected to provide them.
     *
     * @throws MappingException if the transformation results in null for a mandatory field.
     */
    @Throws(MappingException::class)
    fun <T, R : Any> mapMandatoryField(
        dtoValue: T?,
        fieldName: String,
        transform: (T?) -> R?
    ): R
    
    /**
     * Maps a potentially nullable DTO (Data Transfer Object) value to a potentially
     * nullable domain model field.
     *
     * Implementations should handle the transformation of the [dtoValue] using the
     * provided [transform] lambda. If the [dtoValue] is `null`, implementations
     * should typically return `null` without invoking the [transform].
     *
     * If the [dtoValue] is non-null, the [transform] function is applied.
     * Implementations must define how errors during this transformation are handled:
     *  - They might return `null` if the transformation logic within the [transform]
     *    lambda itself results in `null` (e.g., for an unparseable but optional value).
     *  - They might throw a [com.example.campfire.core.common.exception.MappingException]
     *    (or other relevant exception) if a non-null [dtoValue] cannot be transformed
     *    due to being malformed or invalid, and this is considered an error condition
     *    even for a nullable field.
     *
     * @param T The type of the source DTO field's value.
     * @param R The type of the resulting nullable domain model field.
     * @param dtoValue The potentially nullable value from the DTO (or other source data).
     * @param fieldName A descriptive name of the field being mapped. This can be used
     *                  for logging or providing context in error messages if the
     *                  transformation fails.
     * @param transform A lambda function that takes a **non-null** [dtoValue] of type [T]
     *                  and attempts to transform it into a nullable domain model value of type [R]?.
     *                  This lambda may produce `null` if the transformation is not possible or appropriate,
     *                  or it might throw an exception if the input [T] is invalid.
     * @return The transformed, nullable domain model value (`R?`), or `null` if the
     *         input [dtoValue] was `null` or if the transformation resulted in `null`.
     * @throws com.example.campfire.core.common.exception.MappingException (or other)
     *         Implementations may throw this if the [transform] function fails for a
     *         non-null [dtoValue] and this failure is considered an error.
     */
    fun <T, R> mapNullableField(
        dtoValue: T?,
        fieldName: String,
        transform: (T) -> R?
    ): R?
}