package com.example.campfire.core.domain.model

import android.util.Log
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import java.util.Locale


/**
 * Represents a phone number, leveraging libphonenumber for parsing, validation, and formatting.
 *
 * This class aims to store and handle phone numbers in a structured and validated way.
 * The primary internal representation is often the E.164 format for unambiguous storage
 * or the parsed `Phonenumber.PhoneNumber` object from libphonenumber.
 *
 * @property e164Format The phone number in E.164 format (e.g., "+12125551234").
 *                      Null if the input could not be parsed into a valid E.164 number.
 * @property nationalNumber The significant national number part.
 * @property countryCode The numeric country calling code.
 * @property regionCode The ISO 3166-1 alpha-2 country code (e.g., "US", "GB") for this number.
 *                      Null if the region cannot be determined.
 */
data class PhoneNumber(
    val e164Format: String?,
    private val parsedNumber: Phonenumber.PhoneNumber?
) {
    val countryCode: Int?
        get() = parsedNumber?.countryCode
    
    val nationalNumber: Long?
        get() = parsedNumber?.nationalNumber
    
    val regionCode: String?
        get() = parsedNumber?.let { phoneUtil.getRegionCodeForNumber(it) }
    
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
     * Formats the phone number in international format.
     * Example: "+1 212-555-1234".
     * Returns the E.164 format if international formatting isn't possible.
     */
    fun formatInternational(): String {
        return parsedNumber?.let {
            phoneUtil.format(it, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
        } ?: e164Format ?: ""
    }
    
    /**
     * Formats the phone number for out-of-country calling from a specified region.
     * @param callingFromRegionCode The ISO 3166-1 alpha-2 code of the region you are calling from.
     */
    fun formatForOutOfCountryCalling(callingFromRegionCode: String): String {
        return parsedNumber?.let {
            phoneUtil.formatOutOfCountryCallingNumber(it, callingFromRegionCode)
        } ?: e164Format ?: ""
    }
    
    /**
     * Gets the type of the phone number (e.g., MOBILE, FIXED_LINE).
     */
    fun getNumberType(): PhoneNumberUtil.PhoneNumberType? {
        return parsedNumber?.let { phoneUtil.getNumberType(it) }
    }
    
    companion object {
        private const val LOG_TAG = "PhoneNumber"
        private const val LOG_E164_PARSE_FAIL = "Could not parse E.164 number '%s': '%s'"
        
        private val phoneUtil: PhoneNumberUtil by lazy { PhoneNumberUtil.getInstance() }
        
        /**
         * Parses a raw phone number string, attempting to infer the region if not provided.
         * Best effort parsing.
         *
         * @param numberToParse The raw phone number string.
         * @param defaultRegionCode The ISO 3166-1 alpha-2 country code to assume if the number
         *                          doesn't have an international prefix (e.g., "US", "GB").
         *                          Uses the device's default region if null.
         * @return A [PhoneNumber] instance. `isValid` will be false and `e164Format` may be null
         *         if parsing fails or the number is invalid.
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
                PhoneNumber(e164Format = null, parsedNumber = null)
            }
        }
        
        /**
         * Creates a PhoneNumber instance from a known E.164 formatted string.
         * Use this if you are sure the input is already in valid E.164 format.
         *
         * @param e164Number An E.164 formatted number string (e.g., "+12125551234").
         * @return A [PhoneNumber] instance. `isValid` will reflect libphonenumber's validation.
         */
        fun fromE164(e164Number: String): PhoneNumber {
            return try {
                // E164 numbers don't need a default region for parsing.
                val parsed = phoneUtil.parse(e164Number, null)
                PhoneNumber(e164Format = e164Number, parsedNumber = parsed)
            } catch (e: NumberParseException) {
                Log.w(LOG_TAG, String.format(LOG_E164_PARSE_FAIL, e164Number, e.message))
                PhoneNumber(
                    e164Format = e164Number,
                    parsedNumber = null
                ) // Store original even if parse failed
            }
        }
        
        /**
         * Attempts to create a PhoneNumber from separate country code and national number strings.
         *
         * @param countryCodeString The numeric country calling code as a string (e.g., "1", "44").
         * @param nationalNumberString The national part of the number as a string.
         * @return A [PhoneNumber] instance.
         */
        fun fromCountryCodeAndNationalNumber(
            countryCodeString: String,
            nationalNumberString: String
        ): PhoneNumber {
            // Construct a number string that libphonenumber can easily parse with an international prefix.
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
         * Gets an example phone number for a given region.
         * Useful for placeholders or testing.
         *
         * @param regionCode The ISO 3166-1 alpha-2 country code (e.g., "US").
         * @return An example [PhoneNumber] for that region, or an invalid PhoneNumber if not found.
         */
        fun getExampleNumberForRegion(regionCode: String): PhoneNumber {
            val example = phoneUtil.getExampleNumber(regionCode)
            return if (example != null) {
                PhoneNumber(
                    e164Format = phoneUtil.format(example, PhoneNumberUtil.PhoneNumberFormat.E164),
                    parsedNumber = example
                )
            } else {
                PhoneNumber(null, null)
            }
        }
        
        /**
         * Gets the phone dialing code for a given ISO 3166-1 alpha-2 country code.
         * @param regionCode The ISO country code (e.g., "US", "GB").
         * @return The dialing code (e.g., 1, 44), or 0 if not found.
         */
        fun getDialingCodeForRegion(regionCode: String): Int {
            return phoneUtil.getCountryCodeForRegion(regionCode.uppercase(Locale.ROOT))
        }
        
        /**
         * Gets a list of ISO 3166-1 alpha-2 country codes supported by libphonenumber.
         * @return A set of supported region codes.
         */
        fun getSupportedRegions(): Set<String> {
            return phoneUtil.supportedRegions
        }
    }
}