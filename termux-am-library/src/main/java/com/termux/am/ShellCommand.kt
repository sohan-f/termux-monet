/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.termux.am

/**
 * Copied from android-7.0.0_r1 frameworks/base/core/java/android/os
 */
class ShellCommand {

    companion object {
        private const val TAG = "ShellCommand"

        private const val DEBUG = false
    }

    private lateinit var mArgs: Array<String>

    private var mArgPos: Int = 0

    private var mCurArgData: String? = null

    fun init(args: Array<String>, firstArgPos: Int) {
        mArgs = args
        mArgPos = firstArgPos
        mCurArgData = null
    }

    /**
     * Return the next option on the command line -- that is an argument that
     * starts with '-'.  If the next argument is not an option, null is returned.
     */
    fun getNextOption(): String? {
        if (mCurArgData != null) {
            val prev = mArgs[mArgPos - 1]
            throw IllegalArgumentException("No argument expected after \"$prev\"")
        }
        if (mArgPos >= mArgs.size) {
            return null
        }
        val arg = mArgs[mArgPos]
        if (!arg.startsWith("-")) {
            return null
        }
        mArgPos++
        if (arg == "--") {
            return null
        }
        if (arg.length > 1 && arg[1] != '-') {
            if (arg.length > 2) {
                mCurArgData = arg.substring(2)
                return arg.substring(0, 2)
            } else {
                mCurArgData = null
                return arg
            }
        }
        mCurArgData = null
        return arg
    }

    /**
     * Return the next argument on the command line, whatever it is; if there are
     * no arguments left, return null.
     */
    fun getNextArg(): String? {
        if (mCurArgData != null) {
            val arg = mCurArgData
            mCurArgData = null
            return arg
        } else if (mArgPos < mArgs.size) {
            return mArgs[mArgPos++]
        } else {
            return null
        }
    }

    fun peekNextArg(): String? {
        if (mCurArgData != null) {
            return mCurArgData
        } else if (mArgPos < mArgs.size) {
            return mArgs[mArgPos]
        } else {
            return null
        }
    }

    /**
     * Return the next argument on the command line, whatever it is; if there are
     * no arguments left, throws an IllegalArgumentException to report this to the user.
     */
    fun getNextArgRequired(): String {
        val arg = getNextArg()
        if (arg == null) {
            val prev = mArgs[mArgPos - 1]
            throw IllegalArgumentException("Argument expected after \"$prev\"")
        }
        return arg
    }
}
