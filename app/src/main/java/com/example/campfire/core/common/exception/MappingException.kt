package com.example.campfire.core.common.exception


/**
 * Custom exception to indicate an error during data mapping operations
 * (e.g., DTO to Domain model, or Domain model to Request DTO).
 *
 * @param message A descriptive message about the mapping failure.
 * @param cause The underlying exception that caused this mapping failure, if any.
 * @param fieldName The name of the field that was being mapped when the error occurred (optional).
 * @param fieldValue The value of the field that caused the error (optional, be mindful of PII).
 */
class MappingException(
    override val message: String,
    override val cause: Throwable? = null,
    val fieldName: String? = null,
    val fieldValue: Any? = null
) : RuntimeException(message, cause) {
    
    constructor(
        message: String,
        fieldName: String? = null,
        fieldValue: Any? = null
    ) : this(message, null, fieldName, fieldValue)
    
    override fun toString(): String {
        return buildString {
            if (fieldName != null) {
                append(", Field: '$fieldName'")
            }
            if (fieldValue != null) {
                // JD TODO: this would be storing any request value directly, shouldn't keep this
                append(", Value: '$fieldValue'")
            }
        }
    }
}

