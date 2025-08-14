package com.example.campfire.core.common.logging

import timber.log.Timber


/**
 * A [Logger] implementation that delegates to [Timber].
 * This implementation handles the optional tag. If a tag is not provided,
 * Timber's default behavior (automatic tagging based on class name) will be used.
 */
class TimberLogger : Logger {
    
    override fun v(message: String, throwable: Throwable?) {
        Timber.v(throwable, message)
    }
    
    override fun v(tag: String, message: String, throwable: Throwable?) {
        Timber.tag(tag).v(throwable, message)
    }
    
    override fun d(message: String, throwable: Throwable?) {
        Timber.d(throwable, message)
    }
    
    override fun d(tag: String, message: String, throwable: Throwable?) {
        Timber.tag(tag).d(throwable, message)
    }
    
    override fun i(message: String, throwable: Throwable?) {
        Timber.i(throwable, message)
    }
    
    override fun i(tag: String, message: String, throwable: Throwable?) {
        Timber.tag(tag).i(throwable, message)
    }
    
    override fun w(message: String, throwable: Throwable?) {
        Timber.w(throwable, message)
    }
    
    override fun w(tag: String, message: String, throwable: Throwable?) {
        Timber.tag(tag).w(throwable, message)
    }
    
    override fun e(message: String, throwable: Throwable?) {
        Timber.e(throwable, message)
    }
    
    override fun e(tag: String, message: String, throwable: Throwable?) {
        Timber.tag(tag).e(throwable, message)
    }
    
    override fun wtf(message: String, throwable: Throwable?) {
        Timber.wtf(throwable, message)
    }
    
    override fun wtf(tag: String, message: String, throwable: Throwable?) {
        Timber.tag(tag).wtf(throwable, message)
    }
}
