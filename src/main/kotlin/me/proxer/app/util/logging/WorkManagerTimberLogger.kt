package me.proxer.app.util.logging

import android.annotation.SuppressLint
import android.util.Log
import androidx.work.Logger
import timber.log.Timber

/**
 * @author Ruben Gees
 */
@SuppressLint("RestrictedApi")
class WorkManagerTimberLogger(loggingLevel: Int = Log.INFO) : Logger(loggingLevel) {

    override fun verbose(tag: String, message: String) {
        log(Log.VERBOSE, message)
    }

    override fun verbose(tag: String, message: String, throwable: Throwable) {
        log(Log.VERBOSE, message, throwable)
    }

    override fun debug(tag: String, message: String) {
        log(Log.DEBUG, message)
    }

    override fun debug(tag: String, message: String, throwable: Throwable) {
        log(Log.DEBUG, message, throwable)
    }

    override fun info(tag: String, message: String) {
        log(Log.INFO, message)
    }

    override fun info(tag: String, message: String, throwable: Throwable) {
        log(Log.INFO, message, throwable)
    }

    override fun warning(tag: String, message: String) {
        log(Log.WARN, message)
    }

    override fun warning(tag: String, message: String, throwable: Throwable) {
        log(Log.WARN, message, throwable)
    }

    override fun error(tag: String, message: String) {
        log(Log.ERROR, message)
    }

    override fun error(tag: String, message: String, throwable: Throwable) {
        log(Log.ERROR, message, throwable)
    }

    private fun log(priority: Int, message: String, throwable: Throwable? = null) {
        if (throwable == null) {
            Timber.log(priority, message)
        } else {
            Timber.log(priority, message, throwable)
        }
    }
}
