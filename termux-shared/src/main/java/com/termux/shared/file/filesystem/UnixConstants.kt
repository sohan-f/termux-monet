/*
 * Copyright (c) 2008, 2009, Oracle and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */
// AUTOMATICALLY GENERATED FILE - DO NOT EDIT
package com.termux.shared.file.filesystem

// BEGIN Android-changed: Use constants from android.system.OsConstants. http://b/32203242
// Those constants are initialized by native code to ensure correctness on different architectures.
// AT_SYMLINK_NOFOLLOW (used by fstatat) and AT_REMOVEDIR (used by unlinkat) as of July 2018 do not
// have equivalents in android.system.OsConstants so left unchanged.
import android.system.OsConstants

/**
 * https://cs.android.com/android/platform/superproject/+/android-11.0.0_r3:libcore/ojluni/src/main/java/sun/nio/fs/UnixConstants.java
 *
 * Note: Java fields were package-private and only used by [FileAttributes] in
 * the same package. Kotlin has no package-private, so they are widened to
 * public static fields. External callers gain read access but behaviour is unchanged.
 */
class UnixConstants private constructor() {

    companion object {
        @JvmField
        val O_RDONLY: Int = OsConstants.O_RDONLY

        @JvmField
        val O_WRONLY: Int = OsConstants.O_WRONLY

        @JvmField
        val O_RDWR: Int = OsConstants.O_RDWR

        @JvmField
        val O_APPEND: Int = OsConstants.O_APPEND

        @JvmField
        val O_CREAT: Int = OsConstants.O_CREAT

        @JvmField
        val O_EXCL: Int = OsConstants.O_EXCL

        @JvmField
        val O_TRUNC: Int = OsConstants.O_TRUNC

        @JvmField
        val O_SYNC: Int = OsConstants.O_SYNC

        // Crash on Android 5.
        // No static field O_DSYNC of type I in class Landroid/system/OsConstants; or its superclasses
        // (declaration of 'android.system.OsConstants' appears in /system/framework/core-libart.jar)
        //@RequiresApi(Build.VERSION_CODES.O_MR1)
        //static final int O_DSYNC = OsConstants.O_DSYNC;
        @JvmField
        val O_NOFOLLOW: Int = OsConstants.O_NOFOLLOW

        @JvmField
        val S_IAMB: Int = get_S_IAMB()

        @JvmField
        val S_IRUSR: Int = OsConstants.S_IRUSR

        @JvmField
        val S_IWUSR: Int = OsConstants.S_IWUSR

        @JvmField
        val S_IXUSR: Int = OsConstants.S_IXUSR

        @JvmField
        val S_IRGRP: Int = OsConstants.S_IRGRP

        @JvmField
        val S_IWGRP: Int = OsConstants.S_IWGRP

        @JvmField
        val S_IXGRP: Int = OsConstants.S_IXGRP

        @JvmField
        val S_IROTH: Int = OsConstants.S_IROTH

        @JvmField
        val S_IWOTH: Int = OsConstants.S_IWOTH

        @JvmField
        val S_IXOTH: Int = OsConstants.S_IXOTH

        @JvmField
        val S_IFMT: Int = OsConstants.S_IFMT

        @JvmField
        val S_IFREG: Int = OsConstants.S_IFREG

        @JvmField
        val S_IFDIR: Int = OsConstants.S_IFDIR

        @JvmField
        val S_IFLNK: Int = OsConstants.S_IFLNK

        @JvmField
        val S_IFSOCK: Int = OsConstants.S_IFSOCK

        @JvmField
        val S_IFCHR: Int = OsConstants.S_IFCHR

        @JvmField
        val S_IFBLK: Int = OsConstants.S_IFBLK

        @JvmField
        val S_IFIFO: Int = OsConstants.S_IFIFO

        @JvmField
        val R_OK: Int = OsConstants.R_OK

        @JvmField
        val W_OK: Int = OsConstants.W_OK

        @JvmField
        val X_OK: Int = OsConstants.X_OK

        @JvmField
        val F_OK: Int = OsConstants.F_OK

        @JvmField
        val ENOENT: Int = OsConstants.ENOENT

        @JvmField
        val EACCES: Int = OsConstants.EACCES

        @JvmField
        val EEXIST: Int = OsConstants.EEXIST

        @JvmField
        val ENOTDIR: Int = OsConstants.ENOTDIR

        @JvmField
        val EINVAL: Int = OsConstants.EINVAL

        @JvmField
        val EXDEV: Int = OsConstants.EXDEV

        @JvmField
        val EISDIR: Int = OsConstants.EISDIR

        @JvmField
        val ENOTEMPTY: Int = OsConstants.ENOTEMPTY

        @JvmField
        val ENOSPC: Int = OsConstants.ENOSPC

        @JvmField
        val EAGAIN: Int = OsConstants.EAGAIN

        @JvmField
        val ENOSYS: Int = OsConstants.ENOSYS

        @JvmField
        val ELOOP: Int = OsConstants.ELOOP

        @JvmField
        val EROFS: Int = OsConstants.EROFS

        @JvmField
        val ENODATA: Int = OsConstants.ENODATA

        @JvmField
        val ERANGE: Int = OsConstants.ERANGE

        @JvmField
        val EMFILE: Int = OsConstants.EMFILE

        // S_IAMB are access mode bits, therefore, calculated by taking OR of all the read, write and
        // execute permissions bits for owner, group and other.
        @JvmStatic
        private fun get_S_IAMB(): Int {
            return OsConstants.S_IRUSR or OsConstants.S_IWUSR or OsConstants.S_IXUSR or OsConstants.S_IRGRP or OsConstants.S_IWGRP or OsConstants.S_IXGRP or OsConstants.S_IROTH or OsConstants.S_IWOTH or OsConstants.S_IXOTH
        }

        // END Android-changed: Use constants from android.system.OsConstants. http://b/32203242
        @JvmField
        val AT_SYMLINK_NOFOLLOW: Int = 0x100

        @JvmField
        val AT_REMOVEDIR: Int = 0x200
    }
}
