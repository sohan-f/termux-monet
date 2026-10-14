package com.termux.shared.shell

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.termux.shared.file.FileUtils
import com.termux.terminal.TerminalSession
import java.lang.reflect.Field
import java.util.ArrayList
import java.util.Collections

class ShellUtils {

    companion object {
        /**
         * Get process id of {@link Process}.
         */
        @JvmStatic
        fun getPid(p: Process): Int {
            try {
                val f: Field = p.javaClass.getDeclaredField("pid")
                f.isAccessible = true
                try {
                    return f.getInt(p)
                } finally {
                    f.isAccessible = false
                }
            } catch (e: Throwable) {
                return -1
            }
        }

        /**
         * Setup shell command arguments for the execute.
         */
        @NonNull
        @JvmStatic
        fun setupShellCommandArguments(@NonNull executable: String, @Nullable arguments: Array<String?>?): Array<String> {
            val result = ArrayList<String?>()
            result.add(executable)
            if (arguments != null)
                result.addAll(arguments.asList())
            // The list may contain nulls exactly like the Java version (platform
            // array elements); the unchecked cast preserves the String[] shape.
            @Suppress("UNCHECKED_CAST")
            return result.toArray(arrayOfNulls<String>(result.size)) as Array<String>
        }

        /**
         * Get basename for executable.
         */
        @Nullable
        @JvmStatic
        fun getExecutableBasename(@Nullable executable: String?): String? {
            return FileUtils.getFileBasename(executable)
        }

        /**
         * Get transcript for {@link TerminalSession}.
         */
        @JvmStatic
        fun getTerminalSessionTranscriptText(terminalSession: TerminalSession?, linesJoined: Boolean, trim: Boolean): String? {
            if (terminalSession == null)
                return null
            val terminalEmulator = terminalSession.getEmulator() ?: return null
            val terminalBuffer = terminalEmulator.screen
            var transcriptText = if (linesJoined)
                terminalBuffer.getTranscriptTextWithFullLinesJoined()
            else
                terminalBuffer.getTranscriptTextWithoutJoinedLines()
            if (trim)
                transcriptText = transcriptText.trim()
            return transcriptText
        }
    }
}
