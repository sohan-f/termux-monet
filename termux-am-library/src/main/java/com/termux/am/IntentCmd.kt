package com.termux.am

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import java.io.PrintWriter
import java.net.URISyntaxException
import java.util.ArrayList
import java.util.HashSet

/**
 * Copied from android-7.0.0_r1 frameworks/base/core/java/android/content/Intent.java
 */
class IntentCmd {

    /**
     * @hide
     */
    fun interface CommandOptionHandler {

        fun handleOption(opt: String, cmd: ShellCommand): Boolean
    }

    companion object {
        /**
         * @hide
         */
        @JvmStatic
        @Throws(URISyntaxException::class)
        fun parseCommandArgs(cmd: ShellCommand, optionHandler: CommandOptionHandler?): Intent {
            var intent = Intent()
            var baseIntent: Intent? = intent
            var hasIntentInfo = false
            var data: Uri? = null
            var type: String? = null
            while (true) {
                val opt = cmd.getNextOption() ?: break
                when (opt) {
                    "-a" -> {
                        intent.setAction(cmd.getNextArgRequired())
                        if (intent === baseIntent) {
                            hasIntentInfo = true
                        }
                    }
                    "-d" -> {
                        data = Uri.parse(cmd.getNextArgRequired())
                        if (intent === baseIntent) {
                            hasIntentInfo = true
                        }
                    }
                    "-t" -> {
                        type = cmd.getNextArgRequired()
                        if (intent === baseIntent) {
                            hasIntentInfo = true
                        }
                    }
                    "-c" -> {
                        intent.addCategory(cmd.getNextArgRequired())
                        if (intent === baseIntent) {
                            hasIntentInfo = true
                        }
                    }
                    "-e", "--es" -> {
                        val key = cmd.getNextArgRequired()
                        val value = cmd.getNextArgRequired()
                        intent.putExtra(key, value)
                    }
                    "--esn" -> {
                        val key = cmd.getNextArgRequired()
                        intent.putExtra(key, null as String?)
                    }
                    "--ei" -> {
                        val key = cmd.getNextArgRequired()
                        val value = cmd.getNextArgRequired()
                        // javac resolves this to putExtra(String, Serializable);
                        // the cast pins the same overload in Kotlin.
                        intent.putExtra(key, Integer.decode(value) as java.io.Serializable)
                    }
                    "--eu" -> {
                        val key = cmd.getNextArgRequired()
                        val value = cmd.getNextArgRequired()
                        intent.putExtra(key, Uri.parse(value))
                    }
                    "--ecn" -> {
                        val key = cmd.getNextArgRequired()
                        val value = cmd.getNextArgRequired()
                        val cn = ComponentName.unflattenFromString(value)
                            ?: throw IllegalArgumentException("Bad component name: $value")
                        intent.putExtra(key, cn)
                    }
                    "--eia" -> {
                        val key = cmd.getNextArgRequired()
                        val value = cmd.getNextArgRequired()
                        val strings = value.split(",")
                        val list = IntArray(strings.size)
                        for (i in strings.indices) {
                            list[i] = Integer.decode(strings[i])
                        }
                        intent.putExtra(key, list)
                    }
                    "--eial" -> {
                        val key = cmd.getNextArgRequired()
                        val value = cmd.getNextArgRequired()
                        val strings = value.split(",")
                        val list = ArrayList<Int>(strings.size)
                        for (i in strings.indices) {
                            list.add(Integer.decode(strings[i]))
                        }
                        intent.putExtra(key, list)
                    }
                    "--el" -> {
                        val key = cmd.getNextArgRequired()
                        val value = cmd.getNextArgRequired()
                        // javac resolves this to putExtra(String, Serializable);
                        // the cast pins the same overload in Kotlin.
                        intent.putExtra(key, java.lang.Long.valueOf(value) as java.io.Serializable)
                    }
                    "--ela" -> {
                        val key = cmd.getNextArgRequired()
                        val value = cmd.getNextArgRequired()
                        val strings = value.split(",")
                        val list = LongArray(strings.size)
                        for (i in strings.indices) {
                            list[i] = java.lang.Long.valueOf(strings[i])
                        }
                        intent.putExtra(key, list)
                        hasIntentInfo = true
                    }
                    "--elal" -> {
                        val key = cmd.getNextArgRequired()
                        val value = cmd.getNextArgRequired()
                        val strings = value.split(",")
                        val list = ArrayList<Long>(strings.size)
                        for (i in strings.indices) {
                            list.add(java.lang.Long.valueOf(strings[i]))
                        }
                        intent.putExtra(key, list)
                        hasIntentInfo = true
                    }
                    "--ef" -> {
                        val key = cmd.getNextArgRequired()
                        val value = cmd.getNextArgRequired()
                        // javac resolves this to putExtra(String, Serializable);
                        // the cast pins the same overload in Kotlin.
                        intent.putExtra(key, java.lang.Float.valueOf(value) as java.io.Serializable)
                    }
                    "--efa" -> {
                        val key = cmd.getNextArgRequired()
                        val value = cmd.getNextArgRequired()
                        val strings = value.split(",")
                        val list = FloatArray(strings.size)
                        for (i in strings.indices) {
                            list[i] = java.lang.Float.valueOf(strings[i])
                        }
                        intent.putExtra(key, list)
                        hasIntentInfo = true
                    }
                    "--efal" -> {
                        val key = cmd.getNextArgRequired()
                        val value = cmd.getNextArgRequired()
                        val strings = value.split(",")
                        val list = ArrayList<Float>(strings.size)
                        for (i in strings.indices) {
                            list.add(java.lang.Float.valueOf(strings[i]))
                        }
                        intent.putExtra(key, list)
                        hasIntentInfo = true
                    }
                    "--esa" -> {
                        val key = cmd.getNextArgRequired()
                        val value = cmd.getNextArgRequired()
                        // Split on commas unless they are preceeded by an escape.
                        // The escape character must be escaped for the string and
                        // again for the regex, thus four escape characters become one.
                        // Explicit Regex: Kotlin's literal split(String) must not
                        // be used here. toTypedArray keeps the mutable String[]
                        // the Java version worked with.
                        val strings = value.split(Regex("(?<!\\\\),")).toTypedArray()
                        for (i in strings.indices) {
                            strings[i] = strings[i].replace("\\,", ",")
                        }
                        intent.putExtra(key, strings)
                        hasIntentInfo = true
                    }
                    "--esal" -> {
                        val key = cmd.getNextArgRequired()
                        val value = cmd.getNextArgRequired()
                        // Split on commas unless they are preceeded by an escape.
                        // The escape character must be escaped for the string and
                        // again for the regex, thus four escape characters become one.
                        val strings = value.split(Regex("(?<!\\\\),")).toTypedArray()
                        val list = ArrayList<String>(strings.size)
                        for (i in strings.indices) {
                            list.add(strings[i].replace("\\,", ","))
                        }
                        intent.putExtra(key, list)
                        hasIntentInfo = true
                    }
                    "--ez" -> {
                        val key = cmd.getNextArgRequired()
                        val value = cmd.getNextArgRequired().lowercase()
                        // Boolean.valueOf() results in false for anything that is not "true", which is
                        // error-prone in shell commands
                        val arg: Boolean
                        if ("true" == value || "t" == value) {
                            arg = true
                        } else if ("false" == value || "f" == value) {
                            arg = false
                        } else {
                            try {
                                arg = Integer.decode(value) != 0
                            } catch (ex: NumberFormatException) {
                                throw IllegalArgumentException("Invalid boolean value: $value")
                            }
                        }
                        intent.putExtra(key, arg)
                    }
                    "-n" -> {
                        val str = cmd.getNextArgRequired()
                        val cn = ComponentName.unflattenFromString(str)
                            ?: throw IllegalArgumentException("Bad component name: $str")
                        intent.setComponent(cn)
                        if (intent === baseIntent) {
                            hasIntentInfo = true
                        }
                    }
                    "-p" -> {
                        val str = cmd.getNextArgRequired()
                        intent.setPackage(str)
                        if (intent === baseIntent) {
                            hasIntentInfo = true
                        }
                    }
                    "-f" -> {
                        val str = cmd.getNextArgRequired()
                        intent.setFlags(Integer.decode(str))
                    }
                    "--grant-read-uri-permission" -> intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    "--grant-write-uri-permission" -> intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    "--grant-persistable-uri-permission" -> intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                    "--grant-prefix-uri-permission" -> intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                    "--exclude-stopped-packages" -> intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES)
                    "--include-stopped-packages" -> intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    "--debug-log-resolution" -> intent.addFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION)
                    "--activity-brought-to-front" -> intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                    "--activity-clear-top" -> intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    "--activity-clear-when-task-reset" -> intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                    "--activity-exclude-from-recents" -> intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    "--activity-launched-from-history" -> intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
                    "--activity-multiple-task" -> intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    "--activity-no-animation" -> intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    "--activity-no-history" -> intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    "--activity-no-user-action" -> intent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                    "--activity-previous-is-top" -> intent.addFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP)
                    "--activity-reorder-to-front" -> intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    "--activity-reset-task-if-needed" -> intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    "--activity-single-top" -> intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    "--activity-clear-task" -> intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    "--activity-task-on-home" -> intent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME)
                    "--receiver-registered-only" -> intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY)
                    "--receiver-replace-pending" -> intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING)
                    "--receiver-foreground" -> intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                    "--receiver-no-abort" -> intent.addFlags(Intent.FLAG_RECEIVER_NO_ABORT)
                    /*
                    "--receiver-include-background" ->
                        intent.addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND)
                    */
                    "--selector" -> {
                        intent.setDataAndType(data, type)
                        intent = Intent()
                    }
                    else -> {
                        if (optionHandler != null && optionHandler.handleOption(opt, cmd)) {
                            // Okay, caller handled this option.
                        } else {
                            throw IllegalArgumentException("Unknown option: $opt")
                        }
                    }
                }
            }
            intent.setDataAndType(data, type)
            val hasSelector = intent !== baseIntent
            if (hasSelector) {
                // A selector was specified; fix up.
                baseIntent?.setSelector(intent)
                intent = baseIntent!!
            }
            val arg = cmd.getNextArg()
            baseIntent = null
            if (arg == null) {
                if (hasSelector) {
                    // If a selector has been specified, and no arguments
                    // have been supplied for the main Intent, then we can
                    // assume it is ACTION_MAIN CATEGORY_LAUNCHER; we don't
                    // need to have a component name specified yet, the
                    // selector will take care of that.
                    baseIntent = Intent(Intent.ACTION_MAIN)
                    baseIntent?.addCategory(Intent.CATEGORY_LAUNCHER)
                }
            } else if (arg.indexOf(':') >= 0) {
                // The argument is a URI.  Fully parse it, and use that result
                // to fill in any data not specified so far.
                baseIntent = Intent.parseUri(arg, Intent.URI_INTENT_SCHEME or Intent.URI_ANDROID_APP_SCHEME or Intent.URI_ALLOW_UNSAFE)
            } else if (arg.indexOf('/') >= 0) {
                // The argument is a component name.  Build an Intent to launch
                // it.
                baseIntent = Intent(Intent.ACTION_MAIN)
                baseIntent?.addCategory(Intent.CATEGORY_LAUNCHER)
                baseIntent?.setComponent(ComponentName.unflattenFromString(arg))
            } else {
                // Assume the argument is a package name.
                baseIntent = Intent(Intent.ACTION_MAIN)
                baseIntent?.addCategory(Intent.CATEGORY_LAUNCHER)
                baseIntent?.setPackage(arg)
            }
            val base = baseIntent
            if (base != null) {
                var extras: Bundle? = intent.extras
                intent.replaceExtras(null as Bundle?)
                val uriExtras: Bundle? = base.extras
                base.replaceExtras(null as Bundle?)
                if (intent.action != null && base.categories != null) {
                    val cats = HashSet(base.categories)
                    for (c in cats) {
                        base.removeCategory(c)
                    }
                }
                intent.fillIn(base, Intent.FILL_IN_COMPONENT or Intent.FILL_IN_SELECTOR)
                if (extras == null) {
                    extras = uriExtras
                } else if (uriExtras != null) {
                    uriExtras.putAll(extras)
                    extras = uriExtras
                }
                intent.replaceExtras(extras)
                hasIntentInfo = true
            }
            if (!hasIntentInfo)
                throw IllegalArgumentException("No intent supplied")
            return intent
        }

        /**
         * @hide
         */
        @JvmStatic
        fun printIntentArgsHelp(pw: PrintWriter, prefix: String?) {
        val lines = arrayOf("<INTENT> specifications include these flags and arguments:", "    [-a <ACTION>] [-d <DATA_URI>] [-t <MIME_TYPE>]", "    [-c <CATEGORY> [-c <CATEGORY>] ...]", "    [-e|--es <EXTRA_KEY> <EXTRA_STRING_VALUE> ...]", "    [--esn <EXTRA_KEY> ...]", "    [--ez <EXTRA_KEY> <EXTRA_BOOLEAN_VALUE> ...]", "    [--ei <EXTRA_KEY> <EXTRA_INT_VALUE> ...]", "    [--el <EXTRA_KEY> <EXTRA_LONG_VALUE> ...]", "    [--ef <EXTRA_KEY> <EXTRA_FLOAT_VALUE> ...]", "    [--eu <EXTRA_KEY> <EXTRA_URI_VALUE> ...]", "    [--ecn <EXTRA_KEY> <EXTRA_COMPONENT_NAME_VALUE>]", "    [--eia <EXTRA_KEY> <EXTRA_INT_VALUE>[,<EXTRA_INT_VALUE...]]", "        (mutiple extras passed as Integer[])", "    [--eial <EXTRA_KEY> <EXTRA_INT_VALUE>[,<EXTRA_INT_VALUE...]]", "        (mutiple extras passed as List<Integer>)", "    [--ela <EXTRA_KEY> <EXTRA_LONG_VALUE>[,<EXTRA_LONG_VALUE...]]", "        (mutiple extras passed as Long[])", "    [--elal <EXTRA_KEY> <EXTRA_LONG_VALUE>[,<EXTRA_LONG_VALUE...]]", "        (mutiple extras passed as List<Long>)", "    [--efa <EXTRA_KEY> <EXTRA_FLOAT_VALUE>[,<EXTRA_FLOAT_VALUE...]]", "        (mutiple extras passed as Float[])", "    [--efal <EXTRA_KEY> <EXTRA_FLOAT_VALUE>[,<EXTRA_FLOAT_VALUE...]]", "        (mutiple extras passed as List<Float>)", "    [--esa <EXTRA_KEY> <EXTRA_STRING_VALUE>[,<EXTRA_STRING_VALUE...]]", "        (mutiple extras passed as String[]; to embed a comma into a string,", "         escape it using \"\\,\")", "    [--esal <EXTRA_KEY> <EXTRA_STRING_VALUE>[,<EXTRA_STRING_VALUE...]]", "        (mutiple extras passed as List<String>; to embed a comma into a string,", "         escape it using \"\\,\")", "    [-f <FLAG>]", "    [--grant-read-uri-permission] [--grant-write-uri-permission]", "    [--grant-persistable-uri-permission] [--grant-prefix-uri-permission]", "    [--debug-log-resolution] [--exclude-stopped-packages]", "    [--include-stopped-packages]", "    [--activity-brought-to-front] [--activity-clear-top]", "    [--activity-clear-when-task-reset] [--activity-exclude-from-recents]", "    [--activity-launched-from-history] [--activity-multiple-task]", "    [--activity-no-animation] [--activity-no-history]", "    [--activity-no-user-action] [--activity-previous-is-top]", "    [--activity-reorder-to-front] [--activity-reset-task-if-needed]", "    [--activity-single-top] [--activity-clear-task]", "    [--activity-task-on-home]", "    [--receiver-registered-only] [--receiver-replace-pending]", "    [--receiver-foreground] [--receiver-no-abort]", "    [--receiver-include-background]", "    [--selector]", "    [<URI> | <PACKAGE> | <COMPONENT>]");
            for (line in lines) {
                pw.print(prefix)
                pw.println(line)
            }
        }
    }
}
