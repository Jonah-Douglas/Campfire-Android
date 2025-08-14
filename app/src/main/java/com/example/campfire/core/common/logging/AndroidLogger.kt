package com.example.campfire.core.common.logging

import android.annotation.SuppressLint
import android.util.Log


/**
 * A basic [Logger] implementation that uses Android's built-in [android.util.Log].
 * This serves as a fallback or a simple logger when Timber is not desired/available.
 *
 * It uses a [defaultTag] if no specific tag is provided in the log call.
 * Unlike Timber, it does not automatically infer tags from class names.
 *
 * @param defaultTag The tag to use for log messages when no specific tag is provided.
 *                   Defaults to "CampfireApp".
 */
@SuppressLint("LogNotTimber")
class AndroidLogger(private val defaultTag: String = "CampfireApp") : Logger {
    private fun getTag(explicitTag: String?) = explicitTag ?: defaultTag
    
    override fun v(message: String, throwable: Throwable?) {
        Log.v(defaultTag, message, throwable)
    }
    
    override fun v(tag: String, message: String, throwable: Throwable?) {
        Log.v(getTag(tag), message, throwable)
    }
    
    override fun d(message: String, throwable: Throwable?) {
        Log.d(defaultTag, message, throwable)
    }
    
    override fun d(tag: String, message: String, throwable: Throwable?) {
        Log.d(getTag(tag), message, throwable)
    }
    
    override fun i(message: String, throwable: Throwable?) {
        Log.i(defaultTag, message, throwable)
    }
    
    override fun i(tag: String, message: String, throwable: Throwable?) {
        Log.i(getTag(tag), message, throwable)
    }
    
    override fun w(message: String, throwable: Throwable?) {
        Log.w(defaultTag, message, throwable)
    }
    
    override fun w(tag: String, message: String, throwable: Throwable?) {
        Log.w(getTag(tag), message, throwable)
    }
    
    override fun e(message: String, throwable: Throwable?) {
        Log.e(defaultTag, message, throwable)
    }
    
    override fun e(tag: String, message: String, throwable: Throwable?) {
        Log.e(getTag(tag), message, throwable)
    }
    
    override fun wtf(message: String, throwable: Throwable?) {
        Log.wtf(defaultTag, message, throwable)
    }
    
    override fun wtf(tag: String, message: String, throwable: Throwable?) {
        Log.wtf(getTag(tag), message, throwable)
    }
}
