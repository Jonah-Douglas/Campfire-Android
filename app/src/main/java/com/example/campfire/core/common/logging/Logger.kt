package com.example.campfire.core.common.logging


/**
 * Defines a common interface for logging operations.
 * This allows for different logging implementations to be used interchangeably
 * throughout the application via the [Firelog] wrapper.
 */
interface Logger {
    
    /**
     * Logs a verbose message.
     *
     * @param message The message to log.
     * @param throwable Optional [Throwable] to log with the message.
     */
    fun v(message: String, throwable: Throwable? = null)
    
    /**
     * Logs a verbose message with a specific tag.
     *
     * @param tag The custom tag for this log message.
     * @param message The message to log.
     * @param throwable Optional [Throwable] to log with the message.
     */
    fun v(tag: String, message: String, throwable: Throwable? = null)
    
    /**
     * Logs a debug message.
     *
     * @param message The message to log.
     * @param throwable Optional [Throwable] to log with the message.
     */
    fun d(message: String, throwable: Throwable? = null)
    
    /**
     * Logs a debug message with a specific tag.
     *
     * @param tag The custom tag for this log message.
     * @param message The message to log.
     * @param throwable Optional [Throwable] to log with the message.
     */
    fun d(tag: String, message: String, throwable: Throwable? = null)
    
    /**
     * Logs an info message.
     *
     * @param message The message to log.
     * @param throwable Optional [Throwable] to log with the message.
     */
    fun i(message: String, throwable: Throwable? = null)
    
    /**
     * Logs an info message with a specific tag.
     *
     * @param tag The custom tag for this log message.
     * @param message The message to log.
     * @param throwable Optional [Throwable] to log with the message.
     */
    fun i(tag: String, message: String, throwable: Throwable? = null)
    
    /**
     * Logs a warning message.
     *
     * @param message The message to log.
     * @param throwable Optional [Throwable] to log with the message.
     */
    fun w(message: String, throwable: Throwable? = null)
    
    /**
     * Logs a warning message with a specific tag.
     *
     * @param tag The custom tag for this log message.
     * @param message The message to log.
     * @param throwable Optional [Throwable] to log with the message.
     */
    fun w(tag: String, message: String, throwable: Throwable? = null)
    
    /**
     * Logs an error message.
     *
     * @param message The message to log.
     * @param throwable Optional [Throwable] to log with the message.
     */
    fun e(message: String, throwable: Throwable? = null)
    
    /**
     * Logs an error message with a specific tag.
     *
     * @param tag The custom tag for this log message.
     * @param message The message to log.
     * @param throwable Optional [Throwable] to log with the message.
     */
    fun e(tag: String, message: String, throwable: Throwable? = null)
    
    /**
     * Logs an assertion failure (What a Terrible Failure).
     * Use this for conditions that should never occur.
     *
     * @param message The message to log.
     * @param throwable Optional [Throwable] to log with the message.
     */
    fun wtf(message: String, throwable: Throwable? = null)
    
    /**
     * Logs an assertion failure (What a Terrible Failure) with a specific tag.
     * Use this for conditions that should never occur.
     *
     * @param tag The custom tag for this log message.
     * @param message The message to log.
     * @param throwable Optional [Throwable] to log with the message.
     */
    fun wtf(tag: String, message: String, throwable: Throwable? = null)
}

