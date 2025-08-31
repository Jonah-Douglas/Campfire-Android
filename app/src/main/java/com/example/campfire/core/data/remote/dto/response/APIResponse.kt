package com.example.campfire.core.data.remote.dto.response

import com.google.gson.annotations.SerializedName

/**
 * A generic wrapper for all API responses, standardizing the structure
 * for both successful and error outcomes.
 *
 * @param T The type of the data payload for successful responses.
 * @property success Indicates whether the API operation was successful.
 * @property data The actual data payload of type [T] if the operation was successful. Typically null on failure.
 * @property message An optional message from the API, can be present for success or failure.
 * @property error An [APIErrorResponse] object containing error details if the operation failed. Typically null on success.
 */
data class APIResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: T? = null,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("error")
    val error: APIErrorResponse? = null
)