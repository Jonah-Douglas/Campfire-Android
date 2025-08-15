package com.example.campfire.core.common.validation


/**
 * A utility object for common validation patterns and functions.
 */
object ValidationPatterns {
    
    /***
     * Regex from Android's [android.util.Patterns.EMAIL_ADDRESS].
     * Useful to avoid UseCase layer dependencies.
     */
    private val EMAIL_ADDRESS_REGEX = Regex(
        // Local part
        "^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]|[\\w-]{2,}))@"
                // Domain part
                + "((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
                + "([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                + "[0-9]{1,2}|25[0-5]|2[0-4][0-9]))|"       // IP Address
                + "([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$"   // Domain name
    )
    
    /**
     * Determines whether or not a given email address is valid.
     *
     * @param email The email address to validate.
     * @return `true` if the email is valid, `false` otherwise.
     */
    fun isValidEmail(email: CharSequence): Boolean {
        return EMAIL_ADDRESS_REGEX.matches(email)
    }
}