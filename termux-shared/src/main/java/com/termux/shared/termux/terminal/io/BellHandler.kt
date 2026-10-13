package com.termux.shared.termux.terminal.io

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.termux.shared.logger.Logger

class BellHandler private constructor(private val vibrator: Vibrator?) {

    private val handler = Handler(Looper.getMainLooper())

    private var lastBell: Long = 0

    private val bellRunnable: Runnable

    init {
        bellRunnable = Runnable {
            val currentVibrator = vibrator
            if (currentVibrator != null) {
                try {
                    vibrateCompat(currentVibrator)
                } catch (e: Exception) {
                    // Issue on samsung devices on android 8
                    // java.lang.NullPointerException: Attempt to read from field 'android.os.VibrationEffect com.android.server.VibratorService$Vibration.mEffect' on a null object reference
                    Logger.logStackTraceWithMessage(LOG_TAG, "Failed to run vibrator", e)
                }
            }
        }
    }

    @Synchronized
    fun doBell() {
        val now = now()
        val timeSinceLastBell = now - lastBell
        if (timeSinceLastBell < 0) {
            // there is a next bell pending; don't schedule another one
        } else if (timeSinceLastBell < MIN_PAUSE) {
            // there was a bell recently, schedule the next one
            handler.postDelayed(bellRunnable, MIN_PAUSE - timeSinceLastBell)
            lastBell = lastBell + MIN_PAUSE
        } else {
            // the last bell was long ago, do it now
            bellRunnable.run()
            lastBell = now
        }
    }

    private fun now(): Long {
        return SystemClock.uptimeMillis()
    }

    companion object {
        // Fixed: double-checked locking on a non-volatile field could
        // publish a partially constructed instance. Now volatile.
        @Volatile
        private var instance: BellHandler? = null

        private val lock = Any()

        private const val LOG_TAG = "BellHandler"

        @JvmStatic
        @Suppress("DEPRECATION") // VIBRATOR_SERVICE must stay: the pre-S fallback path has no VibratorManager
        fun getInstance(context: Context): BellHandler {
            if (instance == null) {
                synchronized(lock) {
                    if (instance == null) {
                        instance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val vibratorManager = context.applicationContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager?
                            // Null manager threw NPE here in Java as well.
                            BellHandler(vibratorManager!!.defaultVibrator)
                        } else {
                            // A null service stays null (no-op bell), like Java.
                            BellHandler(context.applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?)
                        }
                    }
                }
            }
            return instance!!
        }

        private const val DURATION = 50L

        private const val MIN_PAUSE = 3 * DURATION

        @Suppress("DEPRECATION") // vibrate(long) must stay: the pre-O fallback path
        private fun vibrateCompat(vibrator: Vibrator) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(DURATION, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(DURATION)
            }
        }
    }
}
