package com.example.campfire.core.common.exception


/**
 * Custom exception to indicate an error during data mapping operations
 * (e.g., DTO to Domain model, or Domain model to Request DTO).
 *
 * @param message A descriptive message about the mapping failure.
 * @param cause The underlying exception that caused this mapping failure, if any.
 * @param fieldName The name of the field that was being mapped when the error occurred (optional).
 */
class MappingException(
    override val message: String,
    override val cause: Throwable? = null,
    val fieldName: String? = null,
) : RuntimeException(message, cause) {
    
    constructor(
        message: String,
        fieldName: String? = null,
    ) : this(message, null, fieldName)
    
    override fun toString(): String {
        return buildString {
            if (fieldName != null) {
                append(", Field: '$fieldName'")
            }
        }
    }
}