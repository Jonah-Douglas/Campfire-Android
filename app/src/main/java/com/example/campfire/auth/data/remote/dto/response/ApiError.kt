package com.example.campfire.auth.data.remote.dto.response

import com.google.gson.annotations.SerializedName


/**
 * Represents a generic error structure received from the API.
 *
 * @property code A machine-readable error code string (e.g., "validation_error").
 * @property details An optional human-readable message providing more details about the error.
 * @property fields An optional map of field-specific error messages, where the key is
 *   the field name and the value is the error description for that field.
 */
data class ApiError(
    @SerializedName("error_code")
    val code: String,
    @SerializedName("details")
    val details: String?,
    @SerializedName("fields")
    val fields: Map<String, String>?
)