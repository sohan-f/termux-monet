/*
**
** Copyright 2013, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/
package com.termux.am

import java.io.PrintStream

/**
 * Copied from android-7.0.0_r1 frameworks/base/core/java/com/android/internal/os
 */
abstract class BaseCommand {

    @JvmField
    protected val mArgs: ShellCommand = ShellCommand()

    @JvmField
    protected var out: PrintStream

    @JvmField
    protected var err: PrintStream

    constructor(out: PrintStream, err: PrintStream) {
        this.out = out
        this.err = err
    }

    /**
     * Call to run the command.
     */
    fun run(args: Array<String>): Int {
        if (args.isEmpty()) {
            onShowUsage(out)
            return 1
        }
        mArgs.init(args, 0)
        try {
            onRun()
        } catch (e: IllegalArgumentException) {
            // Matches AOSP: a usage error still returns 0, only unexpected
            // exceptions return 1.
            onShowUsage(err)
            err.println()
            err.println("Error: " + e.message)
        } catch (e: Exception) {
            e.printStackTrace(err)
            return 1
        }
        return 0
    }

    /**
     * Convenience to show usage information to error output.
     */
    fun showUsage() {
        onShowUsage(err)
    }

    /**
     * Convenience to show usage information to error output along
     * with an error message.
     */
    fun showError(message: String?) {
        onShowUsage(err)
        err.println()
        err.println(message)
    }

    /**
     * Implement the command.
     */
    abstract fun onRun()

    /**
     * Print help text for the command.
     */
    abstract fun onShowUsage(out: PrintStream)

    /**
     * Return the next option on the command line -- that is an argument that
     * starts with '-'.  If the next argument is not an option, null is returned.
     */
    fun nextOption(): String? {
        return mArgs.getNextOption()
    }

    /**
     * Return the next argument on the command line, whatever it is; if there are
     * no arguments left, return null.
     */
    fun nextArg(): String? {
        return mArgs.getNextArg()
    }

    /**
     * Return the next argument on the command line, whatever it is; if there are
     * no arguments left, throws an IllegalArgumentException to report this to the user.
     */
    fun nextArgRequired(): String {
        return mArgs.getNextArgRequired()
    }

    companion object {
        // These are magic strings understood by the Eclipse plugin.
        const val FATAL_ERROR_CODE = "Error type 1"

        const val NO_SYSTEM_ERROR_CODE = "Error type 2"

        const val NO_CLASS_ERROR_CODE = "Error type 3"
    }
}
