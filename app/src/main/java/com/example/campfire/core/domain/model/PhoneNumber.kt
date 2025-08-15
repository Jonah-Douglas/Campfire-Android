package com.example.campfire.core.domain.model

import com.example.campfire.core.common.logging.Firelog
import com.example.campfire.core.domain.model.PhoneNumber.Companion.fromE164
import com.example.campfire.core.domain.model.PhoneNumber.Companion.tryParse
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import java.util.Locale


/**
 * Represents a phone number, leveraging Google's libphonenumber library for
 * parsing, validation, and formatting.
 *
 * This class aims to store and handle phone numbers in a structured and validated manner.
 * It encapsulates a `Phonenumber.PhoneNumber` object from libphonenumber and provides
 * convenient access to its properties and formatting utilities.
 *
 * The primary constructor takes an E.164 formatted string and a parsed `Phonenumber.PhoneNumber`
 * object. Factory methods in the companion object ([tryParse], [fromE164], etc.) should
 * typically be used for instantiation.
 *
 * @property e164Format The phone number in E.164 format (e.g., "+12125551234").
 *                      This is `null` if the original input could not be parsed into a valid
 *                      E.164 number by the factory methods, or if the number is invalid.
 * @property parsedNumber The internal `Phonenumber.PhoneNumber` object from libphonenumber.
 *                        This is `null` if parsing failed. This property is private to encapsulate
 *                        libphonenumber details but used by internal logic.
 */
data class PhoneNumber(
    val e164Format: String?,
    private val parsedNumber: Phonenumber.PhoneNumber?
) {
    /**
     * The numeric country calling code for this phone number.
     * Example: `1` for USA, `44` for UK.
     * Returns `null` if the phone number could not be parsed.
     */
    val countryCode: Int?
        get() = parsedNumber?.countryCode
    
    /**
     * The significant national number part of the phone number.
     * This is the number without the country code and any national or international dialing prefixes.
     * Example: For "+1-212-555-1234", this would be `2125551234`.
     * Returns `null` if the phone number could not be parsed.
     */
    val nationalNumber: Long?
        get() = parsedNumber?.nationalNumber
    
    /**
     * The ISO 3166-1 alpha-2 country code (e.g., "US", "GB") for the region
     * associated with this phone number.
     * Returns `null` if the region cannot be determined or if the number is invalid/unparsed.
     */
    val regionCode: String?
        get() = parsedNumber?.let { phoneUtil.getRegionCodeForNumber(it) }
    
    /**
     * Indicates whether this phone number is considered valid by libphonenumber.
     * A number is valid if it's possible to connect to it.
     * Returns `false` if the number is invalid or could not be parsed.
     */
    val isValid: Boolean
        get() = parsedNumber?.let { phoneUtil.isValidNumber(it) } ?: false
    
    /**
     * Formats the phone number in the national format for its region.
     * Example: For a US number, "(212) 555-1234".
     * Returns the E.164 format if national formatting isn't possible or applicable.
     */
    fun formatNational(): String {
        return parsedNumber?.let {
            phoneUtil.format(it, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)
        } ?: e164Format ?: ""
    }
    
    /**
     * Formats the phone number in the national format for its region.
     * Example: For a US number like "+12125551234", this might return "(212) 555-1234".
     * If national formatting isn't possible (e.g., due to an invalid or unparsed number),
     * it falls back to returning the [e164Format] string, or an empty string if that too is null.
     *
     * @return The nationally formatted phone number string, or [e164Format], or an empty string.
     */
    fun formatInternational(): String {
        return parsedNumber?.let {
            phoneUtil.format(it, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
        } ?: e164Format ?: ""
    }
    
    /**
     * Companion object for [PhoneNumber].
     * Provides factory methods for creating [PhoneNumber] instances and utility functions
     * related to phone number metadata.
     */
    companion object {
        private const val LOG_E164_PARSE_FAIL = "Could not parse E.164 number '%s': '%s'"
        
        /**
         * Lazily initialized singleton instance of [PhoneNumberUtil] from libphonenumber.
         * This is the core utility class from the library used for all phone number operations.
         */
        private val phoneUtil: PhoneNumberUtil by lazy { PhoneNumberUtil.getInstance() }
        
        /**
         * Parses a raw phone number string, attempting to infer the region if not provided.
         * This is a best-effort parsing attempt. The resulting [PhoneNumber] should be
         * checked for validity using its [isValid] property.
         *
         * If `defaultRegionCode` is `null`, the device's current default region
         * (from [Locale.getDefault]) will be used as a hint for parsing numbers
         * without an international prefix.
         *
         * @param numberToParse The raw phone number string (e.g., "212-555-1234", "+447911123456").
         * @param defaultRegionCode The ISO 3166-1 alpha-2 country code to assume if the number
         *                          doesn't have an international prefix (e.g., "US", "GB").
         *                          Uses the device's default region if `null`.
         * @return A [PhoneNumber] instance. Check `isValid` and `e164Format` on the returned
         *         instance to determine if parsing was successful and the number is valid.
         *         If parsing fails (e.g., due to [NumberParseException]), a [PhoneNumber]
         *         with `null` [e164Format] and a `null` internal `parsedNumber` is returned,
         *         resulting in `isValid` being `false`.
         */
        fun tryParse(numberToParse: String, defaultRegionCode: String? = null): PhoneNumber {
            val regionToParse = defaultRegionCode ?: Locale.getDefault().country
            return try {
                val parsed = phoneUtil.parse(numberToParse, regionToParse)
                val e164 = if (phoneUtil.isValidNumber(parsed)) {
                    phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
                } else {
                    null
                }
                PhoneNumber(e164Format = e164, parsedNumber = parsed)
            } catch (e: NumberParseException) {
                Firelog.d("NumberParseException for '$numberToParse' in region '$regionToParse': ${e.message}")
                PhoneNumber(e164Format = null, parsedNumber = null)
            }
        }
        
        /**
         * Creates a [PhoneNumber] instance from a known E.164 formatted string.
         * Use this factory method if you are confident the input string is already in
         * valid E.164 format (e.g., "+12125551234").
         *
         * Note: Even if an E.164 string is provided, libphonenumber will still parse it.
         * If the E.164 string is malformed or represents an impossible number,
         * the resulting [PhoneNumber] might have a `null` internal `parsedNumber`
         * (making [isValid] false), although the original `e164Number` string will be retained
         * in [e164Format].
         *
         * @param e164Number An E.164 formatted number string (e.g., "+12125551234").
         * @return A [PhoneNumber] instance. Check `isValid` on the returned instance.
         */
        fun fromE164(e164Number: String): PhoneNumber {
            return try {
                // E.164 numbers are parsed without a default region hint.
                val parsed = phoneUtil.parse(e164Number, null)
                PhoneNumber(e164Format = e164Number, parsedNumber = parsed)
            } catch (e: NumberParseException) {
                Firelog.w(String.format(LOG_E164_PARSE_FAIL, e164Number, e.message))
                PhoneNumber(
                    e164Format = e164Number,
                    parsedNumber = null
                )
            }
        }
        
        /**
         * Attempts to create a [PhoneNumber] instance from separate country code and
         * national number strings.
         *
         * This method constructs a string prefixed with "+" and the country code, then
         * uses [tryParse] for parsing. The validity of the resulting [PhoneNumber]
         * depends on whether libphonenumber can parse this combined string.
         *
         * @param countryCodeString The numeric country calling code as a string (e.g., "1", "44").
         *                          Can optionally include a leading "+".
         * @param nationalNumberString The national (significant) part of the number as a string
         *                             (e.g., "2125551234").
         * @return A [PhoneNumber] instance. Check `isValid` on the returned instance.
         */
        fun fromCountryCodeAndNationalNumber(
            countryCodeString: String,
            nationalNumberString: String
        ): PhoneNumber {
            // Ensure no double '+' if countryCodeString already has it.
            val fullNumberAttempt = if (countryCodeString.startsWith("+")) {
                "${countryCodeString}${nationalNumberString}"
            } else {
                "+${countryCodeString}${nationalNumberString}"
            }
            // Parse as if it's an E.164 or international number.
            return tryParse(fullNumberAttempt, null)
        }
        
        /**
         * Gets the phone dialing code (country code) for a given ISO 3166-1 alpha-2 country code.
         *
         * @param regionCode The ISO 3166-1 alpha-2 country code (e.g., "US", "GB").
         *                   The input is uppercased for compatibility with libphonenumber.
         * @return The numeric dialing code (e.g., 1 for "US", 44 for "GB"),
         *         or `0` if the region code is invalid or not found by libphonenumber.
         */
        fun getDialingCodeForRegion(regionCode: String): Int {
            return phoneUtil.getCountryCodeForRegion(regionCode.uppercase(Locale.ROOT))
        }
        
        /**
         * Gets a set of ISO 3166-1 alpha-2 country codes supported by the underlying
         * libphonenumber library.
         *
         * @return An immutable set of supported region codes (e.g., "US", "GB", "DE").
         */
        fun getSupportedRegions(): Set<String> {
            return phoneUtil.supportedRegions
        }
    }
}