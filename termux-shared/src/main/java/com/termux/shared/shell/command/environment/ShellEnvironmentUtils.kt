package com.termux.shared.shell.command.environment

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.termux.shared.errors.Error
import com.termux.shared.file.FileUtils
import com.termux.shared.logger.Logger
import com.termux.shared.shell.command.environment.UnixShellEnvironment
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap

class ShellEnvironmentUtils {

    companion object {
        private const val LOG_TAG = "ShellEnvironmentUtils"

        /**
         * Convert environment {@link HashMap} to `environ` {@link List<String>}.
         *
         * The items in the environ will have the format `name=value`.
         *
         * Check {@link #isValidEnvironmentVariableName(String)} and {@link #isValidEnvironmentVariableValue(String)}
         * for valid variable names and values.
         *
         * https://manpages.debian.org/testing/manpages/environ.7.en.html
         * https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap08.html
         */
        @NonNull
        @JvmStatic
        fun convertEnvironmentToEnviron(@NonNull environmentMap: HashMap<String, String>): MutableList<String> {
            val environmentList = ArrayList<String>(environmentMap.size)
            for (name in environmentMap.keys) {
                val value = environmentMap[name]
                if (isValidEnvironmentVariableNameValuePair(name, value, true))
                    environmentList.add("$name=$value")
            }
            return environmentList
        }

        /**
         * Convert environment {@link HashMap} to {@link String} where each item equals "key=value".
         */
        @NonNull
        @JvmStatic
        fun convertEnvironmentToDotEnvFile(@NonNull environmentMap: HashMap<String, String>): String {
            return convertEnvironmentToDotEnvFile(convertEnvironmentMapToEnvironmentVariableList(environmentMap))
        }

        /**
         * Convert environment {@link HashMap} to `.env` file {@link String}.
         *
         * The items in the `.env` file have the format `export name="value"`.
         *
         * If the {@link ShellEnvironmentVariable#escaped} is set to {@code true}, then
         * {@link ShellEnvironmentVariable#value} will be considered to be a literal value that has
         * already been escaped by the caller, otherwise all the `"`\$` in the value will be escaped
         * with `a backslash `\`, like `\"`. Note that if `$` is escaped and if its part of variable,
         * then variable expansion will not happen if `.env` file is sourced.
         *
         * The `\` at the end of a value line means line continuation. Value can contain newline characters.
         *
         * Check {@link #isValidEnvironmentVariableName(String)} and {@link #isValidEnvironmentVariableValue(String)}
         * for valid variable names and values.
         *
         * https://github.com/ko1nksm/shdotenv#env-file-syntax
         * https://github.com/ko1nksm/shdotenv/blob/main/docs/specification.md
         */
        @NonNull
        @JvmStatic
        fun convertEnvironmentToDotEnvFile(@NonNull environmentList: MutableList<ShellEnvironmentVariable>): String {
            val environment = StringBuilder()
            Collections.sort(environmentList)
            for (variable in environmentList) {
                val value = variable.value
                if (isValidEnvironmentVariableNameValuePair(variable.name, value, true) && value != null) {
                    // Same regex and replacement as the Java replaceAll call:
                    // pattern matches `"`, backtick, `\` and `$`; replacement
                    // prefixes each with a backslash. $ must be escaped as \$
                    // in Kotlin string templates.
                    environment.append("export ").append(variable.name).append("=\"").append(if (variable.escaped) value else value.replace(Regex("([\"`\\\\\$])"), "\\\\\$1")).append("\"\n")
                }
            }
            return environment.toString()
        }

        /**
         * Convert environment {@link HashMap} to {@link List<ShellEnvironmentVariable>}. Each item
         * will have its {@link ShellEnvironmentVariable#escaped} set to {@code false}.
         */
        @NonNull
        @JvmStatic
        fun convertEnvironmentMapToEnvironmentVariableList(@NonNull environmentMap: HashMap<String, String>): MutableList<ShellEnvironmentVariable> {
            val environmentList = ArrayList<ShellEnvironmentVariable>()
            for (name in environmentMap.keys) {
                environmentList.add(ShellEnvironmentVariable(name, environmentMap[name], false))
            }
            return environmentList
        }

        /**
         * Check if environment variable name and value pair is valid. Errors will be logged if
         * {@code logErrors} is {@code true}.
         *
         * Check {@link #isValidEnvironmentVariableName(String)} and {@link #isValidEnvironmentVariableValue(String)}
         * for valid variable names and values.
         */
        @JvmStatic
        fun isValidEnvironmentVariableNameValuePair(@Nullable name: String?, @Nullable value: String?, logErrors: Boolean): Boolean {
            if (!isValidEnvironmentVariableName(name)) {
                if (logErrors)
                    Logger.logErrorPrivate(LOG_TAG, "Invalid environment variable name. name=`" + name + "`, value=`" + value + "`")
                return false
            }
            if (!isValidEnvironmentVariableValue(value)) {
                if (logErrors)
                    Logger.logErrorPrivate(LOG_TAG, "Invalid environment variable value. name=`" + name + "`, value=`" + value + "`")
                return false
            }
            return true
        }

        /**
         * Check if environment variable name is valid. It must not be {@code null} and must not contain
         * the null byte ('\0') and must only contain alphanumeric and underscore characters and must not
         * start with a digit.
         */
        @JvmStatic
        fun isValidEnvironmentVariableName(@Nullable name: String?): Boolean {
            return name != null && !name.contains("\u0000") && name.matches(Regex("[a-zA-Z_][a-zA-Z0-9_]*"))
        }

        /**
         * Check if environment variable value is valid. It must not be {@code null} and must not contain
         * the null byte ('\0').
         */
        @JvmStatic
        fun isValidEnvironmentVariableValue(@Nullable value: String?): Boolean {
            return value != null && !value.contains("\u0000")
        }

        /**
         * Put value in environment if variable exists in {@link System) environment.
         */
        @JvmStatic
        fun putToEnvIfInSystemEnv(@NonNull environment: HashMap<String, String>, @NonNull name: String) {
            val value = System.getenv(name)
            if (value != null) {
                environment[name] = value
            }
        }

        /**
         * Put {@link String} value in environment if value set.
         */
        @JvmStatic
        fun putToEnvIfSet(@NonNull environment: HashMap<String, String>, @NonNull name: String, @Nullable value: String?) {
            if (value != null) {
                environment[name] = value
            }
        }

        /**
         * Put {@link Boolean} value "true" or "false" in environment if value set.
         */
        @JvmStatic
        fun putToEnvIfSet(@NonNull environment: HashMap<String, String>, @NonNull name: String, @Nullable value: Boolean?) {
            if (value != null) {
                environment[name] = value.toString()
            }
        }

        /**
         * Create HOME directory in environment {@link Map} if set.
         */
        @JvmStatic
        fun createHomeDir(@NonNull environment: HashMap<String, String>) {
            val homeDirectory = environment[UnixShellEnvironment.ENV_HOME]
            if (!homeDirectory.isNullOrEmpty()) {
                val error: Error? = FileUtils.createDirectoryFile("shell home", homeDirectory)
                if (error != null) {
                    Logger.logErrorExtended(LOG_TAG, "Failed to create shell home directory\n" + error.toString())
                }
            }
        }
    }
}
