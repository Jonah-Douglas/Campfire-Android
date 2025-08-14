package com.example.campfire.core.common.logging


/**
 * Singleton wrapper for application-wide logging.
 * This object delegates logging calls to a configured [Logger] implementation.
 * It allows for easy swapping of the underlying logging framework and provides
 * a basic fallback mechanism if no specific logger is initialized.
 *
 * Call [Firelog.initialize] in your Application class to set up the primary logger.
 */
object Firelog : Logger {
    
    private var logger: Logger = AndroidLogger()
    
    /**
     * Initializes Firelog with a specific [Logger] implementation.
     * This is useful for testing or if you want to switch logging behavior dynamically
     * (though typically it's set once at application startup).
     *
     * @param loggerImpl The [Logger] implementation to use.
     */
    fun initialize(loggerImpl: Logger) {
        logger = loggerImpl
    }
    
    override fun v(message: String, throwable: Throwable?) {
        logger.v(message, throwable)
    }
    
    override fun v(tag: String, message: String, throwable: Throwable?) {
        logger.v(tag, message, throwable)
    }
    
    override fun d(message: String, throwable: Throwable?) {
        logger.d(message, throwable)
    }
    
    override fun d(tag: String, message: String, throwable: Throwable?) {
        logger.d(tag, message, throwable)
    }
    
    override fun i(message: String, throwable: Throwable?) {
        logger.i(message, throwable)
    }
    
    override fun i(tag: String, message: String, throwable: Throwable?) {
        logger.i(tag, message, throwable)
    }
    
    override fun w(message: String, throwable: Throwable?) {
        logger.w(message, throwable)
    }
    
    override fun w(tag: String, message: String, throwable: Throwable?) {
        logger.w(tag, message, throwable)
    }
    
    override fun e(message: String, throwable: Throwable?) {
        logger.e(message, throwable)
    }
    
    override fun e(tag: String, message: String, throwable: Throwable?) {
        logger.e(tag, message, throwable)
    }
    
    override fun wtf(message: String, throwable: Throwable?) {
        logger.wtf(message, throwable)
    }
    
    override fun wtf(tag: String, message: String, throwable: Throwable?) {
        logger.wtf(tag, message, throwable)
    }
}
