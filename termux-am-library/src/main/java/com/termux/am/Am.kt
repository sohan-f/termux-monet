/*
**
** Copyright 2007, The Android Open Source Project
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

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Handler
import java.io.PrintStream
import java.io.PrintWriter
import java.security.InvalidParameterException
import java.util.concurrent.CountDownLatch

class Am : BaseCommand {

    private var mRepeat = 0

    private var mReceiverPermission: String? = null

    private val app: Application

    constructor(out: PrintStream, err: PrintStream, app: Application?) : super(out, err) {
        this.app = app ?: throw InvalidParameterException("app context can't be null")
    }

    override fun onShowUsage(out: PrintStream) {
        val pw = PrintWriter(out)
        pw.println("usage: am [subcommand] [options]\n" + "usage: am start [-D] [-N] [-W] [-P <FILE>] [--start-profiler <FILE>]\n" + "               [--sampling INTERVAL] [-R COUNT] [-S]\n" + "               [--track-allocation] [--user <USER_ID> | current] <INTENT>\n" + "       am startservice [--user <USER_ID> | current] <INTENT>\n" + "       am stopservice [--user <USER_ID> | current] <INTENT>\n" + "       am broadcast [--user <USER_ID> | all | current] <INTENT>\n" + "       am to-uri [INTENT]\n" + "       am to-intent-uri [INTENT]\n" + "       am to-app-uri [INTENT]\n" + "\n" + "am start: start an Activity.  Options are:\n" + "    -D: enable debugging\n" + "    -N: enable native debugging\n" + "    -W: wait for launch to complete\n" + "    --start-profiler <FILE>: start profiler and send results to <FILE>\n" + "    --sampling INTERVAL: use sample profiling with INTERVAL microseconds\n" + "        between samples (use with --start-profiler)\n" + "    -P <FILE>: like above, but profiling stops when app goes idle\n" + "    -R: repeat the activity launch <COUNT> times.  Prior to each repeat,\n" + "        the top activity will be finished.\n" + "    -S: force stop the target app before starting the activity\n" + "    --track-allocation: enable tracking of object allocations\n" + "    --user <USER_ID> | current: Specify which user to run as; if not\n" + "        specified then run as the current user.\n" + "    --stack <STACK_ID>: Specify into which stack should the activity be put." + "\n" + "am startservice: start a Service.  Options are:\n" + "    --user <USER_ID> | current: Specify which user to run as; if not\n" + "        specified then run as the current user.\n" + "\n" + "am stopservice: stop a Service.  Options are:\n" + "    --user <USER_ID> | current: Specify which user to run as; if not\n" + "        specified then run as the current user.\n" + "\n" + "am broadcast: send a broadcast Intent.  Options are:\n" + "    --user <USER_ID> | all | current: Specify which user to send to; if not\n" + "        specified then send to all users.\n" + "    --receiver-permission <PERMISSION>: Require receiver to hold permission.\n" + "\n" + "am to-uri: print the given Intent specification as a URI.\n" + "\n" + "am to-intent-uri: print the given Intent specification as an intent: URI.\n" + "\n" + "am to-app-uri: print the given Intent specification as an android-app: URI.\n" + "\n");
        IntentCmd.printIntentArgsHelp(pw, "")
        pw.flush()
    }

    override fun onRun() {
        when (val op = nextArgRequired()) {
            "start" -> runStart()
            "startservice" -> runStartService()
            "stopservice" -> runStopService()
            "broadcast" -> sendBroadcast()
            "to-uri" -> runToUri(0)
            "to-intent-uri" -> runToUri(Intent.URI_INTENT_SCHEME)
            "to-app-uri" -> runToUri(Intent.URI_ANDROID_APP_SCHEME)
            "printid" -> printAndroidId()
            "printid2" -> printAndroidId2()
            else -> showError("Error: unknown command '$op'")
        }
    }

    private fun printAndroidId() {
        val androidId = android.provider.Settings.Secure.getString(app.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
        out.println("Android ID: $androidId")
    }

    private fun printAndroidId2() {
        val androidId = android.provider.Settings.Secure.getString(app.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
        val pw = PrintWriter(out)
        pw.println("Android ID: $androidId")
        pw.flush()
    }

    private fun makeIntent(): Intent {
        mRepeat = 0
        return IntentCmd.parseCommandArgs(mArgs) { opt, _ ->
            when (opt) {
                "-W", "-P", "--stack", "--sampling", "--start-profiler", "-S" -> true
                "-R" -> {
                    mRepeat = Integer.parseInt(nextArgRequired())
                    true
                }
                "--user" -> {
                    nextArgRequired()
                    true
                }
                "--receiver-permission" -> {
                    mReceiverPermission = nextArgRequired()
                    true
                }
                else -> false
            }
        }
    }

    private fun runStartService() {
        val intent = makeIntent()
        out.println("Starting service: $intent")
        val info: ServiceInfo = try {
            // The Java version passed a possibly-null component straight into
            // PackageManager; !! throws the same NPE at the call site instead.
            app.packageManager.getServiceInfo(intent.component!!, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            err.println("Error: Not found; no service started.")
            return
        }
        try {
            app.startService(intent)
        } catch (e: SecurityException) {
            if (!info.permission.isNullOrEmpty()) {
                err.println("Error: Requires permission " + info.permission)
            } else {
                err.println("Could not start service")
            }
        } catch (e: IllegalArgumentException) {
            err.println("Could not start service")
        }
    }

    private fun runStopService() {
        val intent = makeIntent()
        out.println("Stopping service: $intent")
        val info: ServiceInfo = try {
            // See runStartService for the !! rationale.
            app.packageManager.getServiceInfo(intent.component!!, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            err.println("Error: Not found; Service not stopped.")
            return
        }
        try {
            app.stopService(intent)
        } catch (e: SecurityException) {
            if (!info.permission.isNullOrEmpty()) {
                err.println("Error: Requires permission " + info.permission)
            } else {
                err.println("Error stopping service")
            }
        } catch (e: IllegalArgumentException) {
            err.println("Error stopping service")
        }
    }

    private fun runStart() {
        val intent = makeIntent()
        do {
            out.println("Starting: $intent")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                app.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                out.println("Error: Activity class " + intent.component!!.toShortString() + " does not exist.")
            } catch (e: Exception) {
                err.println("Exception while starting Activity:")
                e.printStackTrace(err)
            }
            // Fixed off-by-one: the Java version decremented in the body and looped
            // while `mRepeat > 1`, so `-R 2` launched once and `-R 3` twice.
            // Post-decrementing in the condition launches exactly COUNT times
            // (still exactly once when -R is absent).
        } while (mRepeat-- > 1)
    }

    private fun sendBroadcast() {
        val intent = makeIntent()
        val receiver = IntentReceiver()
        out.println("Broadcasting: $intent")
        app.sendOrderedBroadcast(intent, mReceiverPermission, receiver, Handler(app.mainLooper), Activity.RESULT_OK, null, null)
        receiver.waitForFinish()
    }

    private fun runToUri(flags: Int) {
        val intent = makeIntent()
        out.println(intent.toUri(flags))
    }

    private inner class IntentReceiver : BroadcastReceiver() {

        // Replaces the hand-rolled wait/notify with a latch: single signal,
        // single waiter, identical behaviour.
        private val finished = CountDownLatch(1)

        override fun onReceive(context: Context?, intent: Intent?) {
            var line = "Broadcast completed: result=$resultCode"
            if (resultData != null)
                line += ", data=\"" + resultData + "\""
            val extras = getResultExtras(false)
            if (extras != null)
                line += ", extras: $extras"
            out.println(line)
            finished.countDown()
        }

        fun waitForFinish() {
            try {
                finished.await()
            } catch (e: InterruptedException) {
                throw IllegalStateException(e)
            }
        }
    }
}
