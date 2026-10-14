package com.termux.terminal

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.Stack

/**
 * Renders text into a screen. Contains all the terminal-specific knowledge and state. Emulates a subset of the X Window
 * System xterm terminal, which in turn is an emulator for a subset of the Digital Equipment Corporation vt100 terminal.
 * <p>
 * See also 7-bit Code Table defined at https://vt100.net/docs/vt220-rm/chapter2.html#S2.3.1
 * <p>
 * References:
 * <ul>
 * <li>http://invisible-island.net/xterm/ctlseqs/ctlseqs.html</li>
 * <li>http://en.wikipedia.org/wiki/ANSI_escape_code</li>
 * <li>http://man.he.net/man4/console_codes</li>
 * <li>http://bazaar.launchpad.net/~leonerd/libvterm/trunk/view/head:/src/state.c</li>
 * <li>http://www.columbia.edu/~kermit/k95manual/iso2022.html</li>
 * <li>http://www.vt100.net/docs/vt510-rm/chapter4</li>
 * <li>http://en.wikipedia.org/wiki/ISO/IEC_2022 - for 7-bit and 8-bit GL GR explanation</li>
 * <li>http://bjh21.me.uk/all-escapes/all-escapes.txt - extensive!</li>
 * <li>http://woldlab.caltech.edu/~diane/kde4.10/workingdir/kubuntu/konsole/doc/developer/old-documents/VT100/techref.
 * html - document for konsole - accessible!</li>
 * </ul>
 */
class TerminalEmulator(session: TerminalOutput, columns: Int, rows: Int, cellWidthPixels: Int, cellHeightPixels: Int, transcriptRows: Int?, client: TerminalSessionClient?) {

    companion object {
        /** Log unknown or unimplemented escape sequences received from the shell process. */
        private const val LOG_ESCAPE_SEQUENCES = false

        const val MOUSE_LEFT_BUTTON = 0

        /** Mouse moving while having left mouse button pressed. */
        const val MOUSE_LEFT_BUTTON_MOVED = 32
        const val MOUSE_WHEELUP_BUTTON = 64
        const val MOUSE_WHEELDOWN_BUTTON = 65

        /** Used for invalid data - http://en.wikipedia.org/wiki/Replacement_character#Replacement_character */
        const val UNICODE_REPLACEMENT_CHAR = 0xFFFD

        /*
         * Escape sequences starting with an ESC character.
         *
         * - https://vt100.net/docs/vt220-rm/chapter2.html#S2.5.1
         * - https://invisible-island.net/xterm/ctlseqs/ctlseqs.html#h3-Controls-beginning-with-ESC
         * - https://invisible-island.net/xterm/ctlseqs/ctlseqs.html#h3-C1-lparen-8-Bit-rparen-Control-Characters
         * - https://en.wikipedia.org/wiki/C0_and_C1_control_codes
         */

        /** Escape processing: Not currently in an escape sequence. */
        private const val ESC_NONE = 0
        /** Escape processing: Have seen an ESC character - proceed to {@link #doEsc(int)} */
        private const val ESC = 1
        /** Escape processing: Have seen ESC POUND */
        private const val ESC_POUND = 2
        /** Escape processing: Have seen ESC and a character-set-select ( char */
        private const val ESC_SELECT_LEFT_PAREN = 3
        /** Escape processing: Have seen ESC and a character-set-select ) char */
        private const val ESC_SELECT_RIGHT_PAREN = 4
        /** Escape processing: "ESC [" or CSI (Control Sequence Introducer). */
        private const val ESC_CSI = 6
        /** Escape processing: ESC [ ? */
        private const val ESC_CSI_QUESTIONMARK = 7
        /** Escape processing: ESC [ $ */
        private const val ESC_CSI_DOLLAR = 8
        /** Escape processing: ESC % */
        private const val ESC_PERCENT = 9
        /**
         * Escape processing: `ESC ]` for Operating System Command (OSC)
         * <p>
         * `OSC` commands may be in one of the following formats:
         * - `OSC Ps ; Pt BEL` where `BEL` is the bell control passed as `\a`.
         * - `OSC Ps ; Pt ST` where `ST` is the string terminator passed as `ESC \`.
         * `ST` is the preferred standard for modern terminals.
         * <p>
         * If an `OSC` escape sequence is received, then {@link #mEscapeState} is set to {@link #ESC_OSC}
         * and {@link #receiveOsc(int)} is called by {@link #processCodePoint(int)}.
         * - By default it will add bytes received after `OSC` escape sequence to {@link #mTerminalControlArgs}.
         * - If a `BEL` is received, then {@link #doOsc(String)} is called to process
         *   the OSC command.
         * - If an `ESC` is received, then {@link #mEscapeState} is set to {@link #ESC_OSC__ESC} and
         *   {@link #receiveOscEsc(int)} is called for the next code point.
         *   - If the next code point is a `\` for `ST`, then {@link #doOsc(String)} is
         *     called to process the OSC command.
         *   - If the next code point is not a `\`, then {@link #mEscapeState} is set back to
         *     {@link #ESC_OSC} as `ESC` may be part of command data as so it is added to
         *     {@link #mTerminalControlArgs}, and for later code points {@link #receiveOsc(int)} is called
         *     instead.
         * <p>
         * While an `OSC` is being received, {@link #mOscType} may be set to the command type when it
         * has been fully received by {@link #setOscTypeVariables()}.
         * <p>
         * See also {@link #mIsFastPathOsc} for enabling fast path for specific `OSC` commands if required
         * via {@link #setOscTypeVariables()}.
         * <p>
         * See also {@link #mIgnoreCrLfForOsc} to prevent printing of CR/LF characters for specific
         * `OSC` commands if required via {@link #setOscTypeVariables()}.
         * <p>
         * - https://invisible-island.net/xterm/ctlseqs/ctlseqs.html#h3-Operating-System-Commands
         */
        private const val ESC_OSC = 10
        /** Escape processing: `ESC` received while receiving a {@link #ESC_OSC} command.  */
        private const val ESC_OSC__ESC = 11
        /** Escape processing: ESC [ > */
        private const val ESC_CSI_BIGGERTHAN = 12
        /**
         * Escape procession: `ESC P` for Device Control String (DCS)
         * <p>
         * `DCS` commands are in the format `DCS data ST` where `ST` is the string terminator passed as `ESC \`.
         * `data` is application defined raw data without any specific standards.
         * <p>
         * If an `DCS` escape sequence is received, then {@link #mEscapeState} is set to {@link #ESC_DCS}
         * and {@link #doDcs(int)} is called by {@link #processCodePoint(int)}.
         * - By default it will add bytes received after `DCS` escape sequence to {@link #mTerminalControlArgs}.
         * - If an `ESC` is received by {@link #processCodePoint(int)}, then {@link #ESC_DCS__ESC} set
         *   to `true`.
         * - If the next code point is a `\` for `ST`, then {@link #doDcs(int)} processes the DCS command.
         * - If the next code point is not a `\`, then {@link #ESC_DCS__ESC} is set back to `false` as
         *   `ESC` may be part of command data as so it is added to {@link #mTerminalControlArgs}.
         *  - For certain commands like sixel commands, {@link #doDcs(int)} alters the default behaviour.
         * <p>
         * See also {@link #mIsFastPathDcs} for enabling fast path for specific `DCS` commands if required.
         * <p>
         * <p>
         * - https://vt100.net/docs/vt220-rm/chapter2.html#S2.5.3
         */
        private const val ESC_DCS = 13
        /** Escape processing: CSI > */
        private const val ESC_CSI_QUESTIONMARK_ARG_DOLLAR = 14
        /** Escape processing: CSI $ARGS ' ' */
        private const val ESC_CSI_ARGS_SPACE = 15
        /** Escape processing: CSI $ARGS '*' */
        private const val ESC_CSI_ARGS_ASTERIX = 16
        /** Escape processing: CSI " */
        private const val ESC_CSI_DOUBLE_QUOTE = 17
        /** Escape processing: CSI ' */
        private const val ESC_CSI_SINGLE_QUOTE = 18
        /** Escape processing: CSI ! */
        private const val ESC_CSI_EXCLAMATION = 19
        /**
         * Escape processing: `ESC _` for Application Program Command (APC).
         * <p>
         * `APC` commands are in the format `APC data ST` where `ST` is the string terminator passed as `ESC \`.
         * `data` is application defined raw data without any specific standards.
         * <p>
         * If an `APC` escape sequence is received, then {@link #mEscapeState} is set to {@link #ESC_APC}
         * and {@link #receiveApc(int)} is called by {@link #processCodePoint(int)}.
         * - By default it will add bytes received after `APC` escape sequence to {@link #mTerminalControlArgs}.
         * - If an `ESC` is received, then {@link #mEscapeState} is set to {@link #ESC_APC__ESC} and
         *   {@link #receiveApcEsc(int)} is called for the next code point.
         *   - If the next code point is a `\` for `ST`, then {@link #doApc()} is called to
         *     process the APC command.
         *   - If the next code point is not a `\`, then {@link #mEscapeState} is set back to
         *     {@link #ESC_APC} as `ESC` may be part of command data as so it is added to
         *     {@link #mTerminalControlArgs}, and for later code points {@link #receiveApc(int)} is called
         *     instead.
         * <p>
         * Currently, APC commands are only parsed, but ignored as none are supported.
         * <p>
         * - https://invisible-island.net/xterm/ctlseqs/ctlseqs.html#h3-Application-Program-Command-functions
         */
        private const val ESC_APC = 20
        /** Escape processing: `ESC` received while receiving a {@link #ESC_APC} command.  */
        private const val ESC_APC__ESC = 21
        /** Escape processing: ESC [ <parameter bytes> */
        private const val ESC_CSI_UNSUPPORTED_PARAMETER_BYTE = 22
        /** Escape processing: ESC [ <parameter bytes> <intermediate bytes> */
        private const val ESC_CSI_UNSUPPORTED_INTERMEDIATE_BYTE = 23

        /** The number of parameter arguments including colon separated sub-parameters. */
        private const val MAX_ESCAPE_PARAMETERS = 32

        /** DECSET 1 - application cursor keys. */
        private const val DECSET_BIT_APPLICATION_CURSOR_KEYS = 1
        private const val DECSET_BIT_REVERSE_VIDEO = 1 shl 1
        /**
         * http://www.vt100.net/docs/vt510-rm/DECOM: "When DECOM is set, the home cursor position is at the upper-left
         * corner of the screen, within the margins. The starting point for line numbers depends on the current top margin
         * setting. The cursor cannot move outside of the margins. When DECOM is reset, the home cursor position is at the
         * upper-left corner of the screen. The starting point for line numbers is independent of the margins. The cursor
         * can move outside of the margins."
         */
        private const val DECSET_BIT_ORIGIN_MODE = 1 shl 2
        /**
         * http://www.vt100.net/docs/vt510-rm/DECAWM: "If the DECAWM function is set, then graphic characters received when
         * the cursor is at the right border of the page appear at the beginning of the next line. Any text on the page
         * scrolls up if the cursor is at the end of the scrolling region. If the DECAWM function is reset, then graphic
         * characters received when the cursor is at the right border of the page replace characters already on the page."
         */
        private const val DECSET_BIT_AUTOWRAP = 1 shl 3
        /** DECSET 25 - if the cursor should be enabled, {@link #isCursorEnabled()}. */
        private const val DECSET_BIT_CURSOR_ENABLED = 1 shl 4
        private const val DECSET_BIT_APPLICATION_KEYPAD = 1 shl 5
        /** DECSET 1000 - if to report mouse press&release events. */
        private const val DECSET_BIT_MOUSE_TRACKING_PRESS_RELEASE = 1 shl 6
        /** DECSET 1002 - like 1000, but report moving mouse while pressed. */
        private const val DECSET_BIT_MOUSE_TRACKING_BUTTON_EVENT = 1 shl 7
        /** DECSET 1004 - NOT implemented. */
        private const val DECSET_BIT_SEND_FOCUS_EVENTS = 1 shl 8
        /** DECSET 1006 - SGR-like mouse protocol (the modern sane choice). */
        private const val DECSET_BIT_MOUSE_PROTOCOL_SGR = 1 shl 9
        /** DECSET 2004 - see {@link #paste(String)} */
        private const val DECSET_BIT_BRACKETED_PASTE_MODE = 1 shl 10
        /** Toggled with DECLRMM - http://www.vt100.net/docs/vt510-rm/DECLRMM */
        private const val DECSET_BIT_LEFTRIGHT_MARGIN_MODE = 1 shl 11
        /** Not really DECSET bit... - http://www.vt100.net/docs/vt510-rm/DECSACE */
        private const val DECSET_BIT_RECTANGULAR_CHANGEATTRIBUTE = 1 shl 12

        /**
         * The initial capacity for {@link #mTerminalControlArgs}.
         */
        private const val TERMINAL_CONTROL_ARGS__INITIAL_CAPACITY = 16

        /**
         * The default max length for {@link #mTerminalControlArgs}.
         * Needs to be large enough to contain reasonable OSC 52 pastes, sixel and iterm images data.
         */
        private const val TERMINAL_CONTROL_ARGS__DEFAULT_MAX_LENGTH = 16384

        /**
         * The initial capacity for sixel args stored in {@link #mTerminalControlArgs}.
         */
        private const val SIXEL_ARGS__INITIAL_CAPACITY = 256

        private const val LOG_TAG = "TerminalEmulator"

        @JvmStatic
        fun mapDecSetBitToInternalBit(decsetBit: Int): Int {
            when (decsetBit) {
                1 ->
                    return DECSET_BIT_APPLICATION_CURSOR_KEYS
                5 ->
                    return DECSET_BIT_REVERSE_VIDEO
                6 ->
                    return DECSET_BIT_ORIGIN_MODE
                7 ->
                    return DECSET_BIT_AUTOWRAP
                25 ->
                    return DECSET_BIT_CURSOR_ENABLED
                66 ->
                    return DECSET_BIT_APPLICATION_KEYPAD
                69 ->
                    return DECSET_BIT_LEFTRIGHT_MARGIN_MODE
                1000 ->
                    return DECSET_BIT_MOUSE_TRACKING_PRESS_RELEASE
                1002 ->
                    return DECSET_BIT_MOUSE_TRACKING_BUTTON_EVENT
                1004 ->
                    return DECSET_BIT_SEND_FOCUS_EVENTS
                1006 ->
                    return DECSET_BIT_MOUSE_PROTOCOL_SGR
                2004 ->
                    return DECSET_BIT_BRACKETED_PASTE_MODE
                else ->
                    return -1
                // throw new IllegalArgumentException("Unsupported decset: " + decsetBit);
            }
        }

        /** The number of terminal transcript rows that can be scrolled back to. */
        const val TERMINAL_TRANSCRIPT_ROWS_MIN = 100
        const val TERMINAL_TRANSCRIPT_ROWS_MAX = 50000
        const val DEFAULT_TERMINAL_TRANSCRIPT_ROWS = 2000


        /* The supported terminal cursor styles. */

        const val TERMINAL_CURSOR_STYLE_BLOCK = 0
        const val TERMINAL_CURSOR_STYLE_UNDERLINE = 1
        const val TERMINAL_CURSOR_STYLE_BAR = 2
        const val DEFAULT_TERMINAL_CURSOR_STYLE = TERMINAL_CURSOR_STYLE_BLOCK

        @JvmField
        val TERMINAL_CURSOR_STYLES_LIST = arrayOf(TERMINAL_CURSOR_STYLE_BLOCK, TERMINAL_CURSOR_STYLE_UNDERLINE, TERMINAL_CURSOR_STYLE_BAR)
    }

    private var mTitle: String? = null
    private val mTitleStack = Stack<String?>()

    /** The cursor position. Between (0,0) and (mRows-1, mColumns-1). */
    private var mCursorRow = 0
    private var mCursorCol = 0

    /** The number of character rows and columns in the terminal screen. */
    @JvmField
    var mRows: Int = rows

    @JvmField
    var mColumns: Int = columns

    /** Size of a terminal cell in pixels. */
    private var mCellWidthPixels: Int = cellWidthPixels
    private var mCellHeightPixels: Int = cellHeightPixels

    /** The terminal cursor styles. */
    private var mCursorStyle: Int = DEFAULT_TERMINAL_CURSOR_STYLE

    /** The normal screen buffer. Stores the characters that appear on the screen of the emulated terminal. */
    private val mMainBuffer: TerminalBuffer = TerminalBuffer(client, columns, getTerminalTranscriptRows(transcriptRows), rows)
    /**
     * The alternate screen buffer, exactly as large as the display and contains no additional saved lines (so that when
     * the alternate screen buffer is active, you cannot scroll back to view saved lines).
     * <p>
     * See http://www.xfree86.org/current/ctlseqs.html#The%20Alternate%20Screen%20Buffer
     */
    @JvmField
    val mAltBuffer: TerminalBuffer = TerminalBuffer(client, columns, rows, rows)
    /** The current screen buffer, pointing at either {@link #mMainBuffer} or {@link #mAltBuffer}. */
    private var mScreen: TerminalBuffer = mMainBuffer

    /** The terminal session this emulator is bound to. */
    private val mSession: TerminalOutput = session

    @JvmField
    var mClient: TerminalSessionClient? = client

    /** Keeps track of the current argument of the current escape sequence. Ranges from 0 to MAX_ESCAPE_PARAMETERS-1. */
    private var mArgIndex = 0
    /** Holds the arguments of the current escape sequence. */
    private val mArgs = IntArray(MAX_ESCAPE_PARAMETERS)
    /** Holds the bit flags which arguments are sub parameters (after a colon) - bit N is set if <code>mArgs[N]</code> is a sub parameter. */
    private var mArgsSubParamsBitSet = 0

    /**
     * The max length for {@link #mTerminalControlArgs} used by {@link #collectTerminalControlArgs(int)}.
     */
    private var mTerminalControlArgsMaxLength = TERMINAL_CONTROL_ARGS__DEFAULT_MAX_LENGTH

    /** The terminal control arguments string buffer, like for OSC, DCS, APC commands. */
    private var mTerminalControlArgs = StringBuilder(TERMINAL_CONTROL_ARGS__INITIAL_CAPACITY)

    /**
     * The integer Operating System Command `type` received as `ESC ] type ;`.
     * This will be set as soon as `type` followed by `;` is received, and before any further
     * optional parameters are received.
     */
    private var mOscType = -1

    /**
     * If `true`, then `processCodePoint()` will directly call `receiveOsc()` as a fast path
     * without additional checks.
     *
     * Can be enabled for OSC commands via {@link #setOscTypeVariables()}.
     */
    private var mIsFastPathOsc = false

    /**
     * If `true`, then `processCodePoint()` will not print any CR/LF characters received.
     * This is ignored if `mIsFastPathOsc` is already `true` for a command.
     *
     * Can be enabled for OSC commands via {@link #setOscTypeVariables()}.
     */
    private var mIgnoreCrLfForOsc = false

    /**
     * If `true`, then `processCodePoint()` will directly call `doDcs()` as a fast path
     * without additional checks.
     *
     * Can be enabled for DCS commands via {@link #doDcs(int)}.
     */
    private var mIsFastPathDcs = false

    /** Whether processing an `ESC` for a DCS command. */
    private var ESC_DCS__ESC = false

    /** Whether processing a sixel `DCS q s..s ST` or `DCS P1; P2; P3; q s..s ST` command to create a {@link TerminalSixel}. */
    private var ESC_DCS__SIXEL = false

    /** Whether to check if sixel command is being received when processing a DCS command. */
    private var ESC_DCS__CHECK_IF_SIXEL = true

    /** The command part number in case a long sixel command was broken into parts for processing. */
    private var mSixelCommandPartNum = 0

    /**
     * The capacity to set for {@link #mTerminalControlArgs} used to store sixel commands before processing.
     *
     * See also {@link #ensureTerminalControlArgsCapacity(int)}.
     */
    private var mSixelArgsCapacity: Int? = null

    /** The {@link ITermImage} if an iTerm image command is being processed. */
    private var mITermImage: ITermImage? = null

    /**
     * True if the current escape sequence should continue, false if the current escape sequence should be terminated.
     * Used when parsing a single character.
     */
    private var mContinueSequence = false

    /** The current state of the escape sequence state machine. One of the ESC_* constants. */
    private var mEscapeState = ESC_NONE

    private val mSavedStateMain = SavedScreenState()
    private val mSavedStateAlt = SavedScreenState()

    /** http://www.vt100.net/docs/vt102-ug/table5-15.html */
    private var mUseLineDrawingG0 = false
    private var mUseLineDrawingG1 = false
    private var mUseLineDrawingUsesG0 = true

    /**
     * @see TerminalEmulator#mapDecSetBitToInternalBit(int)
     */
    private var mCurrentDecSetFlags = 0
    private var mSavedDecSetFlags = 0

    /**
     * If insert mode (as opposed to replace mode) is active. In insert mode new characters are inserted, pushing
     * existing text to the right. Characters moved past the right margin are lost.
     */
    private var mInsertMode = false

    /** An array of tab stops. mTabStop[i] is true if there is a tab stop set for column i. */
    private var mTabStop: BooleanArray

    /**
     * Top margin of screen for scrolling ranges from 0 to mRows-2. Bottom margin ranges from mTopMargin + 2 to mRows
     * (Defines the first row after the scrolling region). Left/right margin in [0, mColumns].
     */
    private var mTopMargin = 0
    private var mBottomMargin = 0
    private var mLeftMargin = 0
    private var mRightMargin = 0

    /**
     * If the next character to be emitted will be automatically wrapped to the next line. Used to disambiguate the case
     * where the cursor is positioned on the last column (mColumns-1). When standing there, a written character will be
     * output in the last column, the cursor not moving but this flag will be set. When outputting another character
     * this will move to the next line.
     */
    private var mAboutToAutoWrap = false

    /**
     * If the cursor blinking is enabled. It requires cursor itself to be enabled, which is controlled
     * byt whether {@link #DECSET_BIT_CURSOR_ENABLED} bit is set or not.
     */
    private var mCursorBlinkingEnabled = false

    /**
     * If currently cursor should be in a visible state or not if {@link #mCursorBlinkingEnabled}
     * is {@code true}.
     */
    private var mCursorBlinkState = false

    /**
     * Current foreground, background and underline colors. Can either be a color index in [0,259] or a truecolor (24-bit) value.
     * For a 24-bit value the top byte (0xff000000) is set.
     *
     * <p>Note that the underline color is currently parsed but not yet used during rendering.
     *
     * @see TextStyle
     */
    @JvmField
    var mForeColor = 0

    @JvmField
    var mBackColor = 0

    @JvmField
    var mUnderlineColor = 0

    /** Current {@link TextStyle} effect. */
    @JvmField
    var mEffect = 0

    /**
     * The number of scrolled lines since last calling {@link #clearScrollCounter()}. Used for moving selection up along
     * with the scrolling text.
     */
    private var mScrollCounter = 0

    /** The cached value for this emulator for `TerminalView.mTopRow` as sessions/emulators may be switched.*/
    private var mTopRow = 0

    /** If automatic scrolling of terminal is disabled */
    private var mAutoScrollDisabled = false

    private var mUtf8ToFollow: Byte = 0
    private var mUtf8Index: Byte = 0
    private val mUtf8InputBuffer = ByteArray(4)
    private var mLastEmittedCodePoint = -1

    @JvmField
    val mColors = TerminalColors()

    val screen: TerminalBuffer
        get() = mScreen

    val cursorRow: Int
        get() = mCursorRow

    val cursorCol: Int
        get() = mCursorCol

    /** Get the terminal cursor style. It will be one of {@link #TERMINAL_CURSOR_STYLES_LIST} */
    val cursorStyle: Int
        get() = mCursorStyle

    val isReverseVideo: Boolean
        get() = isDecsetInternalBitSet(DECSET_BIT_REVERSE_VIDEO)

    init {
        mTabStop = BooleanArray(mColumns)
        reset()
    }

    private fun isDecsetInternalBitSet(bit: Int): Boolean {
        return (mCurrentDecSetFlags and bit) != 0
    }

    private fun setDecsetinternalBit(internalBit: Int, set: Boolean) {
        if (set) {
            // The mouse modes are mutually exclusive.
            if (internalBit == DECSET_BIT_MOUSE_TRACKING_PRESS_RELEASE) {
                setDecsetinternalBit(DECSET_BIT_MOUSE_TRACKING_BUTTON_EVENT, false)
            } else if (internalBit == DECSET_BIT_MOUSE_TRACKING_BUTTON_EVENT) {
                setDecsetinternalBit(DECSET_BIT_MOUSE_TRACKING_PRESS_RELEASE, false)
            }
        }
        if (set) {
            mCurrentDecSetFlags = mCurrentDecSetFlags or internalBit
        } else {
            mCurrentDecSetFlags = mCurrentDecSetFlags and internalBit.inv()
        }
    }

    fun updateTerminalSessionClient(client: TerminalSessionClient?) {
        mClient = client
        setCursorStyle()
        setCursorBlinkState(true)
    }

    fun getRows(): Int {
        return mRows
    }

    fun getColumns(): Int {
        return mColumns
    }

    fun getCellWidthPixels(): Int {
        return mCellWidthPixels
    }

    fun getCellHeightPixels(): Int {
        return mCellHeightPixels
    }

    fun isAlternateBufferActive(): Boolean {
        return mScreen == mAltBuffer
    }

    private fun getTerminalTranscriptRows(transcriptRows: Int?): Int {
        if (transcriptRows == null || transcriptRows < TERMINAL_TRANSCRIPT_ROWS_MIN || transcriptRows > TERMINAL_TRANSCRIPT_ROWS_MAX)
            return DEFAULT_TERMINAL_TRANSCRIPT_ROWS
        else
            return transcriptRows
    }

    /** Set the terminal cursor style. */
    fun setCursorStyle() {
        var cursorStyle: Int? = null

        val client = mClient
        if (client != null)
            cursorStyle = client.getTerminalCursorStyle()

        if (cursorStyle == null || !TERMINAL_CURSOR_STYLES_LIST.contains(cursorStyle))
            mCursorStyle = DEFAULT_TERMINAL_CURSOR_STYLE
        else
            mCursorStyle = cursorStyle
    }

    fun isCursorEnabled(): Boolean {
        return isDecsetInternalBitSet(DECSET_BIT_CURSOR_ENABLED)
    }

    fun shouldCursorBeVisible(): Boolean {
        if (!isCursorEnabled())
            return false
        else
            return if (mCursorBlinkingEnabled) mCursorBlinkState else true
    }

    fun setCursorBlinkingEnabled(cursorBlinkingEnabled: Boolean) {
        this.mCursorBlinkingEnabled = cursorBlinkingEnabled
    }

    fun setCursorBlinkState(cursorBlinkState: Boolean) {
        this.mCursorBlinkState = cursorBlinkState
    }

    fun isKeypadApplicationMode(): Boolean {
        return isDecsetInternalBitSet(DECSET_BIT_APPLICATION_KEYPAD)
    }

    fun isCursorKeysApplicationMode(): Boolean {
        return isDecsetInternalBitSet(DECSET_BIT_APPLICATION_CURSOR_KEYS)
    }

    /** If mouse events are being sent as escape codes to the terminal. */
    fun isMouseTrackingActive(): Boolean {
        return isDecsetInternalBitSet(DECSET_BIT_MOUSE_TRACKING_PRESS_RELEASE) || isDecsetInternalBitSet(DECSET_BIT_MOUSE_TRACKING_BUTTON_EVENT)
    }

    private fun setDefaultTabStops() {
        for (i in 0 until mColumns)
            mTabStop[i] = (i and 7) == 0 && i != 0
    }

    /**
     * @param mouseButton one of the MOUSE_* constants of this class.
     */
    fun sendMouseEvent(mouseButton: Int, column: Int, row: Int, pressed: Boolean) {
        var mouseButton = mouseButton
        var column = column
        var row = row
        if (column < 1) column = 1
        if (column > mColumns) column = mColumns
        if (row < 1) row = 1
        if (row > mRows) row = mRows

        if (mouseButton == MOUSE_LEFT_BUTTON_MOVED && !isDecsetInternalBitSet(DECSET_BIT_MOUSE_TRACKING_BUTTON_EVENT)) {
            // Do not send tracking.
        } else if (isDecsetInternalBitSet(DECSET_BIT_MOUSE_PROTOCOL_SGR)) {
            mSession.write(String.format("\u001B[<%d;%d;%d" + (if (pressed) 'M' else 'm'), mouseButton, column, row))
        } else {
            mouseButton = if (pressed) mouseButton else 3 // 3 for release of all buttons.
            // Clip to screen, and clip to the limits of 8-bit data.
            val out_of_bounds = column > 255 - 32 || row > 255 - 32
            if (!out_of_bounds) {
                val data = byteArrayOf(0x1B.toByte(), '['.code.toByte(), 'M'.code.toByte(), (32 + mouseButton).toByte(), (32 + column).toByte(), (32 + row).toByte())
                mSession.write(data, 0, data.size)
            }
        }
    }

    fun resize(columns: Int, rows: Int, cellWidthPixels: Int, cellHeightPixels: Int) {
        this.mCellWidthPixels = cellWidthPixels
        this.mCellHeightPixels = cellHeightPixels

        if (mRows == rows && mColumns == columns) {
            return
        } else if (columns < 2 || rows < 2) {
            throw IllegalArgumentException("rows=" + rows + ", columns=" + columns)
        }

        if (mRows != rows) {
            mRows = rows
            mTopMargin = 0
            mBottomMargin = mRows
        }
        if (mColumns != columns) {
            val oldColumns = mColumns
            mColumns = columns
            val oldTabStop = mTabStop
            mTabStop = BooleanArray(mColumns)
            setDefaultTabStops()
            val toTransfer = Math.min(oldColumns, columns)
            System.arraycopy(oldTabStop, 0, mTabStop, 0, toTransfer)
            mLeftMargin = 0
            mRightMargin = mColumns
        }

        resizeScreen()
    }

    private fun resizeScreen() {
        val cursor = intArrayOf(mCursorCol, mCursorRow)
        val newTotalRows = if (mScreen == mAltBuffer) mRows else mMainBuffer.mTotalRows
        mScreen.resize(mColumns, mRows, newTotalRows, cursor, getStyle(), isAlternateBufferActive())
        mCursorCol = cursor[0]
        mCursorRow = cursor[1]
    }

    /**
     * Accept bytes (typically from the pseudo-teletype) and process them.
     *
     * @param buffer a byte array containing the bytes to be processed
     * @param length the number of bytes in the array to process
     */
    fun append(buffer: ByteArray, length: Int) {
        for (i in 0 until length)
            processByte(buffer[i])
    }

    private fun processByte(byteToProcess: Byte) {
        if (mUtf8ToFollow.toInt() > 0) {
            if ((byteToProcess.toInt() and 0b11000000) == 0b10000000) {
                // 10xxxxxx, a continuation byte.
                mUtf8InputBuffer[mUtf8Index.toInt()] = byteToProcess
                mUtf8Index++
                mUtf8ToFollow--
                if (mUtf8ToFollow.toInt() == 0) {
                    val firstByteMask = (if (mUtf8Index.toInt() == 2) 0b00011111 else (if (mUtf8Index.toInt() == 3) 0b00001111 else 0b00000111)).toByte()
                    var codePoint = (mUtf8InputBuffer[0].toInt() and firstByteMask.toInt())
                    for (i in 1 until mUtf8Index.toInt())
                        codePoint = ((codePoint shl 6) or (mUtf8InputBuffer[i].toInt() and 0b00111111))
                    if (((codePoint <= 0b1111111) && mUtf8Index.toInt() > 1) || (codePoint < 0b11111111111 && mUtf8Index.toInt() > 2)
                        || (codePoint < 0b1111111111111111 && mUtf8Index.toInt() > 3)) {
                        // Overlong encoding.
                        codePoint = UNICODE_REPLACEMENT_CHAR
                    }

                    mUtf8Index = 0
                    mUtf8ToFollow = 0

                    if (codePoint >= 0x80 && codePoint <= 0x9F) {
                        // Sequence decoded to a C1 control character which we ignore. They are
                        // not used nowadays and increases the risk of messing up the terminal state
                        // on binary input. XTerm does not allow them in utf-8:
                        // "It is not possible to use a C1 control obtained from decoding the
                        // UTF-8 text" - http://invisible-island.net/xterm/ctlseqs/ctlseqs.html
                    } else {
                        when (Character.getType(codePoint)) {
                            Character.UNASSIGNED.toInt(),
                            Character.SURROGATE.toInt() ->
                                codePoint = UNICODE_REPLACEMENT_CHAR
                        }
                        processCodePoint(codePoint)
                    }
                }
            } else {
                // Not a UTF-8 continuation byte so replace the entire sequence up to now with the replacement char:
                mUtf8Index = 0
                mUtf8ToFollow = 0
                emitCodePoint(UNICODE_REPLACEMENT_CHAR)
                // The Unicode Standard Version 6.2 – Core Specification
                // (http://www.unicode.org/versions/Unicode6.2.0/ch03.pdf):
                // "If the converter encounters an ill-formed UTF-8 code unit sequence which starts with a valid first
                // byte, but which does not continue with valid successor bytes (see Table 3-7), it must not consume the
                // successor bytes as part of the ill-formed subsequence
                // whenever those successor bytes themselves constitute part of a well-formed UTF-8 code unit
                // subsequence."
                processByte(byteToProcess)
            }
        } else {
            if ((byteToProcess.toInt() and 0b10000000) == 0) { // The leading bit is not set so it is a 7-bit ASCII character.
                processCodePoint(byteToProcess.toInt())
                return
            } else if ((byteToProcess.toInt() and 0b11100000) == 0b11000000) { // 110xxxxx, a two-byte sequence.
                mUtf8ToFollow = 1
            } else if ((byteToProcess.toInt() and 0b11110000) == 0b11100000) { // 1110xxxx, a three-byte sequence.
                mUtf8ToFollow = 2
            } else if ((byteToProcess.toInt() and 0b11111000) == 0b11110000) { // 11110xxx, a four-byte sequence.
                mUtf8ToFollow = 3
            } else {
                // Not a valid UTF-8 sequence start, signal invalid data:
                processCodePoint(UNICODE_REPLACEMENT_CHAR)
                return
            }
            mUtf8InputBuffer[mUtf8Index.toInt()] = byteToProcess
            mUtf8Index++
        }
    }

    fun processCodePoint(b: Int) {
        mScreen.doTerminalBitmapsGC(300000)

        if (mEscapeState == ESC_OSC && mIsFastPathOsc) {
            mContinueSequence = false
            receiveOsc(b)
            if (!mContinueSequence) mEscapeState = ESC_NONE
            return
        }

        if (mEscapeState == ESC_DCS && mIsFastPathDcs) {
            if (b == 27) { // ESC
                ESC_DCS__ESC = true
                return
            }
            mContinueSequence = false
            doDcs(b)
            if (!mContinueSequence) mEscapeState = ESC_NONE
            return
        }

        // The Application Program-Control (APC) string might be arbitrary non-printable characters, so handle that early.
        if (mEscapeState == ESC_APC) {
            mContinueSequence = false
            receiveApc(b)
            if (!mContinueSequence) mEscapeState = ESC_NONE
            return
        } else if (mEscapeState == ESC_APC__ESC) {
            mContinueSequence = false
            receiveApcEsc(b)
            if (!mContinueSequence) mEscapeState = ESC_NONE
            return
        }

        when (b) {
            0 -> { // Null character (NUL, ^@). Do nothing.
            }
            7 -> { // Bell (BEL, ^G, \a). If in an OSC sequence, BEL may terminate a string; otherwise signal bell.
                if (mEscapeState == ESC_OSC)
                    receiveOsc(b)
                else
                    mSession.onBell()
            }
            8 -> { // Backspace (BS, ^H).
                if (mLeftMargin == mCursorCol) {
                    // Jump to previous line if it was auto-wrapped.
                    val previousRow = mCursorRow - 1
                    if (previousRow >= 0 && mScreen.getLineWrap(previousRow)) {
                        mScreen.clearLineWrap(previousRow)
                        setCursorRowCol(previousRow, mRightMargin - 1)
                    }
                } else {
                    setCursorCol(mCursorCol - 1)
                }
            }
            9 -> { // Horizontal tab (HT, \t) - move to next tab stop, but not past edge of screen
                // XXX: Should perhaps use color if writing to new cells. Try with
                //       printf "\033[41m\tXX\033[0m\n"
                // The OSX Terminal.app colors the spaces from the tab red, but xterm does not.
                // Note that Terminal.app only colors on new cells, in e.g.
                //       printf "\033[41m\t\r\033[42m\tXX\033[0m\n"
                // the first cells are created with a red background, but when tabbing over
                // them again with a green background they are not overwritten.
                mCursorCol = nextTabStop(1)
            }
            10, // Line feed (LF, \n).
            11, // Vertical tab (VT, \v).
            12 -> { // Form feed (FF, \f).
                // Ignore CR/LF inside DCS by default (including sixel) or OSC if requested (like for iTerm).
                if (!
                    (mEscapeState == ESC_DCS ||
                    ((mEscapeState == ESC_OSC || mEscapeState == ESC_OSC__ESC) && mIgnoreCrLfForOsc))) {
                    doLinefeed()
                }
            }
            13 -> { // Carriage return (CR, \r).
                if (!
                    (mEscapeState == ESC_DCS ||
                    ((mEscapeState == ESC_OSC || mEscapeState == ESC_OSC__ESC) && mIgnoreCrLfForOsc))) {
                    setCursorCol(mLeftMargin)
                }
            }
            14 -> { // Shift Out (Ctrl-N, SO) → Switch to Alternate Character Set. This invokes the G1 character set.
                mUseLineDrawingUsesG0 = false
            }
            15 -> { // Shift In (Ctrl-O, SI) → Switch to Standard Character Set. This invokes the G0 character set.
                mUseLineDrawingUsesG0 = true
            }
            24, // CAN.
            26 -> { // SUB.
                if (mEscapeState != ESC_NONE) {
                    // FIXME: What is this??
                    mEscapeState = ESC_NONE
                    emitCodePoint(127)
                }
            }
            27 -> { // ESC
                // Starts an escape sequence unless we're parsing a string
                if (mEscapeState == ESC_DCS) {
                    // XXX: Ignore escape when reading device control sequence, since it may be part of string terminator.
                    ESC_DCS__ESC = true
                    return
                } else if (mEscapeState != ESC_OSC) {
                    startEscapeSequence()
                } else {
                    receiveOsc(b)
                }
            }
            else -> {
                mContinueSequence = false
                when (mEscapeState) {
                    ESC_NONE ->
                        if (b >= 32) emitCodePoint(b)
                    ESC ->
                        doEsc(b)
                    ESC_POUND ->
                        doEscPound(b)
                    ESC_SELECT_LEFT_PAREN -> // Designate G0 Character Set (ISO 2022, VT100).
                        mUseLineDrawingG0 = (b == '0'.code)
                    ESC_SELECT_RIGHT_PAREN -> // Designate G1 Character Set (ISO 2022, VT100).
                        mUseLineDrawingG1 = (b == '0'.code)
                    ESC_CSI ->
                        doCsi(b)
                    ESC_CSI_UNSUPPORTED_PARAMETER_BYTE,
                    ESC_CSI_UNSUPPORTED_INTERMEDIATE_BYTE ->
                        doCsiUnsupportedParameterOrIntermediateByte(b)
                    ESC_CSI_EXCLAMATION ->
                        if (b == 'p'.code) { // Soft terminal reset (DECSTR, http://vt100.net/docs/vt510-rm/DECSTR).
                            reset()
                        } else {
                            unknownSequence(b)
                        }
                    ESC_CSI_QUESTIONMARK ->
                        doCsiQuestionMark(b)
                    ESC_CSI_BIGGERTHAN ->
                        doCsiBiggerThan(b)
                    ESC_CSI_DOLLAR -> {
                        val originMode = isDecsetInternalBitSet(DECSET_BIT_ORIGIN_MODE)
                        val effectiveTopMargin = if (originMode) mTopMargin else 0
                        val effectiveBottomMargin = if (originMode) mBottomMargin else mRows
                        val effectiveLeftMargin = if (originMode) mLeftMargin else 0
                        val effectiveRightMargin = if (originMode) mRightMargin else mColumns
                        when (b) {
                            'v'.code -> { // ${CSI}${SRC_TOP}${SRC_LEFT}${SRC_BOTTOM}${SRC_RIGHT}${SRC_PAGE}${DST_TOP}${DST_LEFT}${DST_PAGE}$v"
                                // Copy rectangular area (DECCRA - http://vt100.net/docs/vt510-rm/DECCRA):
                                // "If Pbs is greater than Pts, or Pls is greater than Prs, the terminal ignores DECCRA.
                                // The coordinates of the rectangular area are affected by the setting of origin mode (DECOM).
                                // DECCRA is not affected by the page margins.
                                // The copied text takes on the line attributes of the destination area.
                                // If the value of Pt, Pl, Pb, or Pr exceeds the width or height of the active page, then the value
                                // is treated as the width or height of that page.
                                // If the destination area is partially off the page, then DECCRA clips the off-page data.
                                // DECCRA does not change the active cursor position."
                                val topSource = Math.min(getArg(0, 1, true) - 1 + effectiveTopMargin, mRows)
                                val leftSource = Math.min(getArg(1, 1, true) - 1 + effectiveLeftMargin, mColumns)
                                // Inclusive, so do not subtract one:
                                val bottomSource = Math.min(Math.max(getArg(2, mRows, true) + effectiveTopMargin, topSource), mRows)
                                val rightSource = Math.min(Math.max(getArg(3, mColumns, true) + effectiveLeftMargin, leftSource), mColumns)
                                // int sourcePage = getArg(4, 1, true);
                                val destionationTop = Math.min(getArg(5, 1, true) - 1 + effectiveTopMargin, mRows)
                                val destinationLeft = Math.min(getArg(6, 1, true) - 1 + effectiveLeftMargin, mColumns)
                                // int destinationPage = getArg(7, 1, true);
                                val heightToCopy = Math.min(mRows - destionationTop, bottomSource - topSource)
                                val widthToCopy = Math.min(mColumns - destinationLeft, rightSource - leftSource)
                                mScreen.blockCopy(leftSource, topSource, widthToCopy, heightToCopy, destinationLeft, destionationTop)
                            }
                            '{'.code, // ${CSI}${TOP}${LEFT}${BOTTOM}${RIGHT}${"
                            'x'.code, // ${CSI}${CHAR};${TOP}${LEFT}${BOTTOM}${RIGHT}$x"
                            'z'.code -> { // ${CSI}$${TOP}${LEFT}${BOTTOM}${RIGHT}$z"
                                // Selective erase rectangular area (DECSERA - http://www.vt100.net/docs/vt510-rm/DECSERA).
                                // Fill rectangular area (DECFRA - http://www.vt100.net/docs/vt510-rm/DECFRA).
                                // Erase rectangular area (DECERA - http://www.vt100.net/docs/vt510-rm/DECERA).
                                val erase = b != 'x'.code
                                val selective = b == '{'.code
                                // Only DECSERA keeps visual attributes, DECERA does not:
                                val keepVisualAttributes = erase && selective
                                var argIndex = 0
                                val fillChar: Int
                                if (erase) {
                                    fillChar = ' '.code
                                } else {
                                    fillChar = getArg(argIndex, -1, true)
                                    argIndex++
                                }
                                // "Pch can be any value from 32 to 126 or from 160 to 255. If Pch is not in this range, then the
                                // terminal ignores the DECFRA command":
                                if ((fillChar >= 32 && fillChar <= 126) || (fillChar >= 160 && fillChar <= 255)) {
                                    // "If the value of Pt, Pl, Pb, or Pr exceeds the width or height of the active page, the value
                                    // is treated as the width or height of that page."
                                    val top = Math.min(getArg(argIndex, 1, true) + effectiveTopMargin, effectiveBottomMargin + 1)
                                    argIndex++
                                    val left = Math.min(getArg(argIndex, 1, true) + effectiveLeftMargin, effectiveRightMargin + 1)
                                    argIndex++
                                    val bottom = Math.min(getArg(argIndex, mRows, true) + effectiveTopMargin, effectiveBottomMargin)
                                    argIndex++
                                    val right = Math.min(getArg(argIndex, mColumns, true) + effectiveLeftMargin, effectiveRightMargin)
                                    val style = getStyle()
                                    for (row in top - 1 until bottom)
                                        for (col in left - 1 until right)
                                            if (!selective || (TextStyle.decodeEffect(mScreen.getStyleAt(row, col)) and TextStyle.CHARACTER_ATTRIBUTE_PROTECTED) == 0)
                                                mScreen.setChar(col, row, fillChar, if (keepVisualAttributes) mScreen.getStyleAt(row, col) else style)
                                }
                            }
                            'r'.code, // "${CSI}${TOP};${LEFT};${BOTTOM};${RIGHT}${ATTRIBUTES}$r"
                            't'.code -> { // "${CSI}${TOP};${LEFT};${BOTTOM};${RIGHT}${ATTRIBUTES}$t"
                                // Change attributes in rectangular area (DECCARA - http://vt100.net/docs/vt510-rm/DECCARA).
                                // Reverse attributes in rectangular area (DECRARA - http://www.vt100.net/docs/vt510-rm/DECRARA).
                                val reverse = b == 't'.code
                                // FIXME: "coordinates of the rectangular area are affected by the setting of origin mode (DECOM)".
                                val top = Math.min(getArg(0, 1, true) - 1, effectiveBottomMargin) + effectiveTopMargin
                                val left = Math.min(getArg(1, 1, true) - 1, effectiveRightMargin) + effectiveLeftMargin
                                val bottom = Math.min(getArg(2, mRows, true) + 1, effectiveBottomMargin - 1) + effectiveTopMargin
                                val right = Math.min(getArg(3, mColumns, true) + 1, effectiveRightMargin - 1) + effectiveLeftMargin
                                if (mArgIndex >= 4) {
                                    if (mArgIndex >= mArgs.size) mArgIndex = mArgs.size - 1
                                    for (i in 4..mArgIndex) {
                                        var bits = 0
                                        var setOrClear = true // True if setting, false if clearing.
                                        when (getArg(i, 0, false)) {
                                            0 -> { // Attributes off (no bold, no underline, no blink, positive image).
                                                bits = (TextStyle.CHARACTER_ATTRIBUTE_BOLD or TextStyle.CHARACTER_ATTRIBUTE_UNDERLINE or TextStyle.CHARACTER_ATTRIBUTE_BLINK
                                                    or TextStyle.CHARACTER_ATTRIBUTE_INVERSE)
                                                if (!reverse) setOrClear = false
                                            }
                                            1 -> // Bold.
                                                bits = TextStyle.CHARACTER_ATTRIBUTE_BOLD
                                            4 -> // Underline.
                                                bits = TextStyle.CHARACTER_ATTRIBUTE_UNDERLINE
                                            5 -> // Blink.
                                                bits = TextStyle.CHARACTER_ATTRIBUTE_BLINK
                                            7 -> // Negative image.
                                                bits = TextStyle.CHARACTER_ATTRIBUTE_INVERSE
                                            22 -> { // No bold.
                                                bits = TextStyle.CHARACTER_ATTRIBUTE_BOLD
                                                setOrClear = false
                                            }
                                            24 -> { // No underline.
                                                bits = TextStyle.CHARACTER_ATTRIBUTE_UNDERLINE
                                                setOrClear = false
                                            }
                                            25 -> { // No blink.
                                                bits = TextStyle.CHARACTER_ATTRIBUTE_BLINK
                                                setOrClear = false
                                            }
                                            27 -> { // Positive image.
                                                bits = TextStyle.CHARACTER_ATTRIBUTE_INVERSE
                                                setOrClear = false
                                            }
                                        }
                                        if (reverse && !setOrClear) {
                                            // Reverse attributes in rectangular area ignores non-(1,4,5,7) bits.
                                        } else {
                                            mScreen.setOrClearEffect(bits, setOrClear, reverse, isDecsetInternalBitSet(DECSET_BIT_RECTANGULAR_CHANGEATTRIBUTE),
                                                effectiveLeftMargin, effectiveRightMargin, top, left, bottom, right)
                                        }
                                    }
                                } else {
                                    // Do nothing.
                                }
                            }
                            else -> {
                                unknownSequence(b)
                            }
                        }
                    }
                    ESC_CSI_DOUBLE_QUOTE ->
                        if (b == 'q'.code) {
                            // http://www.vt100.net/docs/vt510-rm/DECSCA
                            val arg = getArg0(0)
                            if (arg == 0 || arg == 2) {
                                // DECSED and DECSEL can erase characters.
                                mEffect = mEffect and TextStyle.CHARACTER_ATTRIBUTE_PROTECTED.inv()
                            } else if (arg == 1) {
                                // DECSED and DECSEL cannot erase characters.
                                mEffect = mEffect or TextStyle.CHARACTER_ATTRIBUTE_PROTECTED
                            } else {
                                unknownSequence(b)
                            }
                        } else {
                            unknownSequence(b)
                        }
                    ESC_CSI_SINGLE_QUOTE ->
                        if (b == '}'.code) { // Insert Ps Column(s) (default = 1) (DECIC), VT420 and up.
                            val columnsAfterCursor = mRightMargin - mCursorCol
                            val columnsToInsert = Math.min(getArg0(1), columnsAfterCursor)
                            val columnsToMove = columnsAfterCursor - columnsToInsert
                            mScreen.blockCopy(mCursorCol, 0, columnsToMove, mRows, mCursorCol + columnsToInsert, 0)
                            blockClear(mCursorCol, 0, columnsToInsert, mRows)
                        } else if (b == '~'.code) { // Delete Ps Column(s) (default = 1) (DECDC), VT420 and up.
                            val columnsAfterCursor = mRightMargin - mCursorCol
                            val columnsToDelete = Math.min(getArg0(1), columnsAfterCursor)
                            val columnsToMove = columnsAfterCursor - columnsToDelete
                            mScreen.blockCopy(mCursorCol + columnsToDelete, 0, columnsToMove, mRows, mCursorCol, 0)
                        } else {
                            unknownSequence(b)
                        }
                    ESC_PERCENT -> {
                    }
                    ESC_OSC ->
                        receiveOsc(b)
                    ESC_OSC__ESC ->
                        receiveOscEsc(b)
                    ESC_DCS ->
                        doDcs(b)
                    ESC_CSI_QUESTIONMARK_ARG_DOLLAR ->
                        if (b == 'p'.code) {
                            // Request DEC private mode (DECRQM).
                            val mode = getArg0(0)
                            val value = if (mode == 47 || mode == 1047 || mode == 1049) {
                                // This state is carried by mScreen pointer.
                                if (mScreen == mAltBuffer) 1 else 2
                            } else {
                                val internalBit = mapDecSetBitToInternalBit(mode)
                                if (internalBit != -1) {
                                    if (isDecsetInternalBitSet(internalBit)) 1 else 2 // 1=set, 2=reset.
                                } else {
                                    Logger.logError(mClient, LOG_TAG, "Got DECRQM for unrecognized private DEC mode=" + mode)
                                    0 // 0=not recognized, 3=permanently set, 4=permanently reset
                                }
                            }
                            mSession.write(String.format(Locale.US, "\u001B[?%d;%d\$y", mode, value))
                        } else {
                            unknownSequence(b)
                        }
                    ESC_CSI_ARGS_SPACE -> {
                        val arg = getArg0(0)
                        when (b) {
                            'q'.code -> { // "${CSI}${STYLE} q" - set cursor style (http://www.vt100.net/docs/vt510-rm/DECSCUSR).
                                when (arg) {
                                    0, // Blinking block.
                                    1, // Blinking block.
                                    2 -> // Steady block.
                                        mCursorStyle = TERMINAL_CURSOR_STYLE_BLOCK
                                    3, // Blinking underline.
                                    4 -> // Steady underline.
                                        mCursorStyle = TERMINAL_CURSOR_STYLE_UNDERLINE
                                    5, // Blinking bar (xterm addition).
                                    6 -> // Steady bar (xterm addition).
                                        mCursorStyle = TERMINAL_CURSOR_STYLE_BAR
                                }
                            }
                            't'.code,
                            'u'.code -> {
                                // Set margin-bell volume - ignore.
                            }
                            else ->
                                unknownSequence(b)
                        }
                    }
                    ESC_CSI_ARGS_ASTERIX -> {
                        val attributeChangeExtent = getArg0(0)
                        if (b == 'x'.code && (attributeChangeExtent >= 0 && attributeChangeExtent <= 2)) {
                            // Select attribute change extent (DECSACE - http://www.vt100.net/docs/vt510-rm/DECSACE).
                            setDecsetinternalBit(DECSET_BIT_RECTANGULAR_CHANGEATTRIBUTE, attributeChangeExtent == 2)
                        } else {
                            unknownSequence(b)
                        }
                    }
                    else ->
                        unknownSequence(b)
                }
                if (!mContinueSequence) mEscapeState = ESC_NONE
            }
        }
    }

    /**
     * Do {@link #ESC_DCS}. Check its docs for more info.
     */
    private fun doDcs(b: Int) {
        if (
            // End of DCS if string terminator ST `ESC \` received.
            (ESC_DCS__ESC && b == '\\'.code) ||
            // If sixel continuation after sixel start and a
            // Color Introducer `#`, Graphics Repeat Introducer `!` or Raster Attributes `"`
            // command is received, then process any previous commands, or if end of input
            // with a ST received.
            // If `b` is a Color Introducer `#`, Graphics Repeat Introducer `!` or Raster Attributes `"`
            // command, then it is added to buffer in code below and more input is waited for as
            // further arguments need to be received for its command before it can be processed,
            // which is not until the next command is received.
            // We wait till at least `mTerminalControlArgsMaxLength / 2` commands string has
            // been received. The divide by 2 is done since if near the max length, a new command
            // starts and it does not end before the max length, then `Terminal control args overflow
            // error would occur.
            // If the first command has been fully received, then we run it immediately in case
            // it is the Raster Attributes command containing the "rough" horizontal and vertical
            // size of image, which is used to set the capacity of the `mTerminalControlArgs` buffer
            // and also resize the bitmap, so that memory allocations are avoided if possible.
            // `mTerminalControlArgs.length > 1` is done so that loop does not engage on first
            // character after `q` and only after first command has been fully received.
            (ESC_DCS__SIXEL && ((b == '#'.code || b == '!'.code || b == '"'.code) &&
                ((mTerminalControlArgs.length >= (mTerminalControlArgsMaxLength / 2)) || (mTerminalControlArgs.length > 1 && mSixelCommandPartNum == 1))))
        ) {
                val dcs = mTerminalControlArgs.toString()

                // Request Selection or Setting (DECRQSS) `DCS $ q P t ST`.
                // - https://vt100.net/docs/vt510-rm/DECRQSS.html
                if (dcs.startsWith("\$q")) {
                    if (dcs == "\$q\"p") {
                        // DECSCL, conformance level, http://www.vt100.net/docs/vt510-rm/DECSCL:
                        val csiString = "64;1\"p"
                        mSession.write("\u001BP1\$r" + csiString + "\u001B\\")
                    } else {
                        finishSequenceAndLogError("Unrecognized DECRQSS string: '" + dcs + "'")
                    }
                } else if (dcs.startsWith("+q")) {
                    // Request Termcap/Terminfo String. The string following the "q" is a list of names encoded in
                    // hexadecimal (2 digits per character) separated by ; which correspond to termcap or terminfo key
                    // names.
                    // Two special features are also recognized, which are not key names: Co for termcap colors (or colors
                    // for terminfo colors), and TN for termcap name (or name for terminfo name).
                    // xterm responds with DCS 1 + r P t ST for valid requests, adding to P t an = , and the value of the
                    // corresponding string that xterm would send, or DCS 0 + r P t ST for invalid requests. The strings are
                    // encoded in hexadecimal (2 digits per character).
                    // Example:
                    // :kr=\EOC: ks=\E[?1h\E=: ku=\EOA: le=^H:mb=\E[5m:md=\E[1m:\
                    // where
                    // kd=down-arrow key
                    // kl=left-arrow key
                    // kr=right-arrow key
                    // ku=up-arrow key
                    // #2=key_shome, "shifted home"
                    // #4=key_sleft, "shift arrow left"
                    // %i=key_sright, "shift arrow right"
                    // *7=key_send, "shifted end"
                    // k1=F1 function key

                    // Example: Request for ku is "ESC P + q 6 b 7 5 ESC \", where 6b7d=ku in hexadecimal.
                    // Xterm response in normal cursor mode:
                    // "<27> P 1 + r 6 b 7 5 = 1 B 5 B 4 1" where 0x1B 0x5B 0x41 = 27 91 65 = ESC [ A
                    // Xterm response in application cursor mode:
                    // "<27> P 1 + r 6 b 7 5 = 1 B 5 B 4 1" where 0x1B 0x4F 0x41 = 27 91 65 = ESC 0 A

                    // #4 is "shift arrow left":
                    // *** Device Control (DCS) for '#4'- 'ESC P + q 23 34 ESC \'
                    // Response: <27> P 1 + r 2 3 3 4 = 1 B 5 B 3 1 3 B 3 2 4 4 <27> \
                    // where 0x1B 0x5B 0x31 0x3B 0x32 0x44 = ESC [ 1 ; 2 D
                    // which we find in: TermKeyListener.java: KEY_MAP.put(KEYMOD_SHIFT | KEYCODE_DPAD_LEFT, "\033[1;2D");

                    // See http://h30097.www3.hp.com/docs/base_doc/DOCUMENTATION/V40G_HTML/MAN/MAN4/0178____.HTM for what to
                    // respond, as well as http://www.freebsd.org/cgi/man.cgi?query=termcap&sektion=5#CAPABILITIES for
                    // the meaning of e.g. "ku", "kd", "kr", "kl"

                    for (part in dcs.substring(2).split(";").dropLastWhile { it.isEmpty() }) {
                        if (part.length % 2 == 0) {
                            val transBuffer = StringBuilder()
                            var c: Char
                            for (i in part.indices step 2) {
                                c = try {
                                    ("" + part[i] + part[i + 1]).toInt(16).toChar()
                                } catch (e: NumberFormatException) {
                                    Logger.logStackTraceWithMessage(mClient, LOG_TAG, "Invalid device termcap/terminfo encoded name \"" + part + "\"", e)
                                    continue
                                }
                                transBuffer.append(c)
                            }

                            val trans = transBuffer.toString()
                            val responseValue: String? = when (trans) {
                                "Co",
                                "colors" ->
                                    "256" // Number of colors.
                                "TN",
                                "name" ->
                                    "xterm"
                                else ->
                                    KeyHandler.getCodeFromTermcap(trans, isDecsetInternalBitSet(DECSET_BIT_APPLICATION_CURSOR_KEYS),
                                        isDecsetInternalBitSet(DECSET_BIT_APPLICATION_KEYPAD))
                            }
                            if (responseValue == null) {
                                when (trans) {
                                    "%1", // Help key - ignore
                                    "&8" -> { // Undo key - ignore.
                                    }
                                    else ->
                                        Logger.logWarn(mClient, LOG_TAG, "Unhandled termcap/terminfo name: '" + trans + "'")
                                }
                                // Respond with invalid request:
                                mSession.write("\u001BP0+r" + part + "\u001B\\")
                            } else {
                                val hexEncoded = StringBuilder()
                                for (j in responseValue.indices) {
                                    hexEncoded.append("%02X".format(responseValue[j].code))
                                }
                                mSession.write("\u001BP1+r" + part + "=" + hexEncoded + "\u001B\\")
                            }
                        } else {
                            Logger.logError(mClient, LOG_TAG, "Invalid device termcap/terminfo name of odd length: " + part)
                        }
                    }
                }
                // If `s..s` or `ST` received from Sixel Device Control String `DCS q s..s ST` or `DCS P1; P2; P3; q s..s ST` command.
                else if (ESC_DCS__SIXEL) {
                    mSixelCommandPartNum++

                    val isValidDcs = processSixelDcs(dcs)

                    if (!isValidDcs) {
                        clearTerminalControlArgs()
                        clearDcsTypeVariables()
                        finishSequence()
                        return
                    }

                    if (ESC_DCS__ESC && b == '\\'.code) {
                        var n = mScreen.sixelEnd(mCursorCol, mCursorRow, mCellWidthPixels, mCellHeightPixels)
                        while (n > 0) {
                            doLinefeed()
                            n--
                        }

                        // Clear DCS args buffer and variables and finish sequence.
                    } else {
                        ESC_DCS__ESC = false

                        // Clear DCS args buffer to receive further new input in empty buffer.
                        clearTerminalControlArgs()

                        // Increase capacity to expected capacity if `Raster Attributes` command
                        // was sent with image width and height, or to default
                        // `SIXEL_ARGS__INITIAL_CAPACITY` set by `startIfSixelDcs()`.
                        val sixelArgsCapacity = mSixelArgsCapacity
                        if (sixelArgsCapacity != null) {
                            ensureTerminalControlArgsCapacity(sixelArgsCapacity)
                        }

                        // If `b` is a Color Introducer `#`, Graphics Repeat Introducer `!` or Raster Attributes `"`
                        // command, then add to buffer and wait for more input as further arguments
                        // need to be received for its command before it can be processed, which is
                        // not until the next command is received.
                        if (!collectTerminalControlArgs(b)) return

                        return
                    }
                } else {
                    if (LOG_ESCAPE_SEQUENCES)
                        Logger.logError(mClient, LOG_TAG, "Unrecognized device control string: " + dcs)
                }

                // Clear DCS args buffer and variables and finish sequence.
                clearTerminalControlArgs()
                clearDcsTypeVariables()
                finishSequence()
        } else {
                ESC_DCS__ESC = false

                if (!collectTerminalControlArgs(b)) return

                if (ESC_DCS__CHECK_IF_SIXEL && !ESC_DCS__SIXEL) {
                    // Check if `DCS q` or `DCS P1; P2; P3; q` received from Sixel
                    // Device Control String `DCS q s..s ST` or `DCS P1; P2; P3; q s..s ST` command.
                    // If received, then wait for more input after `q`.
                    if (b == 'q'.code) {
                        startIfSixelDcs()
                    } else if (b == ';'.code || (b >= '0'.code && b <= '9'.code)) {
                        // Ignore.
                    } else {
                        ESC_DCS__CHECK_IF_SIXEL = false
                    }
                }
        }
    }

    fun clearDcsTypeVariables() {
        ESC_DCS__ESC = false
        mIsFastPathDcs = false

        ESC_DCS__SIXEL = false
        ESC_DCS__CHECK_IF_SIXEL = true
        mSixelCommandPartNum = 0
        mSixelArgsCapacity = null
        mScreen.sixelClear()
    }

    private fun startIfSixelDcs() {
        val sixelDcsSetupArgs = getSixelDcsSetupArgs(mTerminalControlArgs.toString(), 0)
        if (sixelDcsSetupArgs != null) {
            mIsFastPathDcs = true
            ESC_DCS__SIXEL = true
            ESC_DCS__CHECK_IF_SIXEL = false
            mSixelCommandPartNum = 1

            // Do not actually increase capacity yet, as it will be increased by `doDcs()` after
            // first command has been received, which is checked to see if its a `Raster Attributes`
            // command with image width and height to calculate expected capacity.
            mSixelArgsCapacity = SIXEL_ARGS__INITIAL_CAPACITY

            // The `P1; P2; P3;` arguements in `sixelDcsSetupArgs` are ignored as they are not supported currently (if ever).
            mScreen.sixelStart(100, 100)
            clearTerminalControlArgs()
        }
    }

    private fun getSixelDcsSetupArgs(dcs: String, index: Int): IntArray? {
        var index = index
        val args = intArrayOf(/* `P1=0`/`2:1` */ 0, /* `P2=0` */ 0, /* `P3=0` */ 0)

        if (dcs[index] == 'q') return args

        var ch: Char

        var arg = 0
        var incArg = false
        while (index < dcs.length) {
            ch = dcs[index]
            if (ch >= '0' && ch <= '9') {
                if (incArg) { arg++; incArg = false }
                args[arg] = args[arg] * 10 + (ch - '0')
                if (args[arg] < 0) { // Overflow.
                    break
                }
                index++
            } else if (ch == ';') {
                index++

                if (arg == 2) {
                    if (index < dcs.length) {
                        if (dcs[index] == 'q') {
                            return args
                        } else {
                            // Must be some other command, so no need to check again.
                            ESC_DCS__CHECK_IF_SIXEL = false
                        }
                    }
                    break
                }

                incArg = true
            } else if (ch == 'q') {
                // If optional parameters `P1`, `P2` or `P3` are not all passed, or
                // a parameter did not end with a `;`, but is followed by `q`.
                return args
            } else {
                break
            }
        }

        return null
    }

    private fun processSixelDcs(dcs: String): Boolean {
        var index = 0

        var ch: Char = ' '
        var repeat = 1
        var color = 0
        var isValidDcs = true

        // FIXME: Use `StringUtils.truncateLogStringWithSnippet()` when its added for logging `dcs` input
        //  in errors as it may be truncated from end before index if error length is greater than
        //  `Logger.LOGGER_ENTRY_MAX_SAFE_PAYLOAD`.
        while (index < dcs.length) {
            ch = dcs[index]

            if (
                // Sixel data characters in the range of `?` (0x3F) to `~` (0x7E).
                // - https://vt100.net/docs/vt3xx-gp/chapter14.html#S14.2.1
                (ch >= '?' && ch <= '~')
                // Graphics Carriage Return `$`.
                // - https://vt100.net/docs/vt3xx-gp/chapter14.html#S14.3.4
                || ch == '$'
                // Graphics New Line `-`.
                // - https://vt100.net/docs/vt3xx-gp/chapter14.html#S14.3.5
                || ch == '-'
            ) {
                mScreen.sixelReadData(ch.code, repeat)
                index++
                repeat = 1
            }
            // Color Introducer `#`
            // - https://vt100.net/docs/vt3xx-gp/chapter14.html#S14.3.3
            else if (ch == '#') {
                index++ // Consume '#'.

                color = 0
                while (index < dcs.length) {
                    ch = dcs[index]
                    if (ch >= '0' && ch <= '9') {
                        color = color * 10 + (ch - '0')
                        if (color < 0) { // Overflow.
                            Logger.logError(mClient, LOG_TAG, "The sixel color command Pc value overflow at index " + index + " of sixel input: " + dcs)
                            isValidDcs = false
                            break
                        }
                        index++
                    } else {
                        break
                    }
                }

                if (color > 255) {
                    Logger.logError(mClient, LOG_TAG, "The sixel color command Pc value " + color + " is not between 0-255 at index " + index + " of sixel input: " + dcs)
                    isValidDcs = false
                    break
                }

                if (!isValidDcs) {
                    break
                }

                // Basic Colors `# Pc`
                // - https://vt100.net/docs/vt3xx-gp/chapter14.html#S14.3.3.1
                if (index == dcs.length || dcs[index] != ';') {
                    mScreen.sixelSetColor(color)
                }
                // HLS or RGB Colors `# Pc; Pu; Px; Py; Pz`
                // - https://vt100.net/docs/vt3xx-gp/chapter14.html#S14.3.3.2
                else if (dcs[index] == ';') {
                    index++ // Consume ';'.

                    val args = intArrayOf(0, 0, 0, 0)
                    var arg = 0
                    var incArg = false
                    while (index < dcs.length) {
                        ch = dcs[index]
                        if (ch >= '0' && ch <= '9') {
                            if (incArg) { arg++; incArg = false }
                            args[arg] = args[arg] * 10 + (ch - '0')
                            if (args[arg] < 0) { // Overflow.
                                var argName = ""
                                when (arg) { 0 -> argName = "Pu"; 1 -> argName = "pX"; 2 -> argName = "pY"; 3 -> argName = "pZ" }
                                Logger.logError(mClient, LOG_TAG, "The sixel non-basic color command " + argName + " value overflow at index " + index + " of sixel input: " + dcs)
                                isValidDcs = false
                                break
                            }
                        } else if (ch == ';') {
                            if (arg == 3) { // Pz must not end with a ';'.
                                Logger.logError(mClient, LOG_TAG, "The sixel non-basic color command Pz value " + args[3] + " must not end with a semicolon ';' at index " + index + " of sixel input: " + dcs)
                                isValidDcs = false
                                break
                            }

                            incArg = true
                        } else {
                            break
                        }
                        index++
                    }

                    if (!isValidDcs) {
                        break
                    }

                    for (i in args.indices) {
                        if (i == 0) { // Pu must equal 1 or 2.
                            if ((args[i] != 1 && args[i] != 2)) {
                                Logger.logError(mClient, LOG_TAG, "The sixel non-basic color command Pu value " + args[i] + " is not 1 or 2 at index " + index + " of sixel input: " + dcs)
                                isValidDcs = false
                                break
                            }
                        } else {
                            var limit = 100
                            if (args[0] == 1 && i == 1) limit = 360
                            if (args[i] < 0 || args[i] > limit) {
                                var argName = ""
                                when (i) { 1 -> argName = "pX"; 2 -> argName = "pY"; 3 -> argName = "pZ" }
                                Logger.logError(mClient, LOG_TAG, "The sixel non-basic color command " + argName + " value " + args[i] + " is not between 0-" + limit + " at index " + index + " of sixel input: " + dcs)
                                isValidDcs = false
                                break
                            }
                        }
                    }

                    if (isValidDcs && arg == 3) { // If complete spec is received and is valid.
                        if (args[0] == 2) { // Only RGB is supported.
                            mScreen.sixelSetRGBColor(color, args[1], args[2], args[3])
                        } else if (args[0] == 1) { // HLS is not supported.
                            Logger.logError(mClient, LOG_TAG, "The sixel non-basic color command Pu value " + args[0] + " is not supported at index " + index + " of sixel input: " + dcs)
                            mScreen.sixelIgnore()
                            break
                        }
                    } else {
                        if (isValidDcs)
                            Logger.logError(mClient, LOG_TAG, "The sixel non-basic color command expected 4 arguments at index " + index + " of sixel input: " + dcs)
                        isValidDcs = false
                        break
                    }
                }
            }
            // Graphics Repeat Introducer `! Pn character`.
            // - https://vt100.net/docs/vt3xx-gp/chapter14.html#S14.3.1
            else if (ch == '!') {
                index++ // Consume '!'.

                repeat = 0
                while (index < dcs.length) {
                    ch = dcs[index]
                    if (ch >= '0' && ch <= '9') {
                        repeat = repeat * 10 + (ch - '0')
                        if (repeat < 0) { // Overflow.
                            Logger.logError(mClient, LOG_TAG, "The sixel repeat command Pn value overflow at index " + index + " of sixel input: " + dcs)
                            isValidDcs = false
                            break
                        }
                        index++
                    } else {
                        break
                    }
                }

                if (repeat > TerminalSixel.SIXEL__MAX_REPEAT) {
                    Logger.logError(mClient, LOG_TAG, "The sixel repeat command Pn value " + repeat + " is greater than max repeat value " +
                        TerminalSixel.SIXEL__MAX_REPEAT + " at index " + index + " of sixel input: " + dcs)
                    mScreen.sixelIgnore()
                    break
                }

                if (!isValidDcs) {
                    break
                }
            }
            // Raster Attributes `" Pan; Pad; Ph; Pv`
            // - https://vt100.net/docs/vt3xx-gp/chapter14.html#S14.3.2
            else if (ch == '"') {
                index++ // Consume '"'.

                val args = intArrayOf(0, 0, 0, 0)
                var arg = 0
                var incArg = false
                while (index < dcs.length) {
                    ch = dcs[index]
                    if (ch >= '0' && ch <= '9') {
                        if (incArg) { arg++; incArg = false }
                        args[arg] = args[arg] * 10 + (ch - '0')
                        if (args[arg] < 0) { // Overflow.
                            var argName = ""
                            when (arg) { 0 -> argName = "Pan"; 1 -> argName = "Pad"; 2 -> argName = "pH"; 3 -> argName = "pV" }
                            Logger.logError(mClient, LOG_TAG, "The sixel raster command " + argName + " value overflow at index " + index + " of sixel input: " + dcs)
                            isValidDcs = false
                            break
                        }
                    } else if (ch == ';') {
                        if (arg == 3) { // Pv must not end with a ';'.
                            Logger.logError(mClient, LOG_TAG, "The sixel raster command Pv value " + args[3] + " must not end with a semicolon ';' at index " + index + " of sixel input: " + dcs)
                            isValidDcs = false
                            break
                        }
                        incArg = true
                    } else {
                        break
                    }
                    index++
                }

                if (isValidDcs && arg == 3) { // If complete spec is received and is valid.
                    // Raster pixel aspect ratio is not supported currently.
                    // Raster "rough" horizontal and vertical size of image may be sent at start of
                    // sixel data string, like done by `img2sixel`, so increase sixel commands args
                    // buffer capacity (`mTerminalControlArgs`) and resize sixel bitmap in
                    // `TerminalSixel` at start, instead of having to keep resizing buffer/bitmap
                    // as more sixel data is received, which has a performance hit due to
                    // memory reallocations and copying.
                    val sixelWidth = args[2] // `Ph`
                    val sixelHeight = args[3] // `Pv`
                    if (sixelWidth > 0 && sixelHeight > 0) {
                        // 2% extra for sixel commands/parameters in addition to image data.
                        val sixelArgsExpectedLength = (sixelWidth * sixelHeight * 1.02).toInt()
                        // If sixel commands are too long, they are divided into parts, and if a
                        // new command starts near `mTerminalControlArgsMaxLength / 2`, it could
                        // contain image data for 1 pixel line of image width, so add that.
                        val sixelArgsPartsExpectedLength = ((mTerminalControlArgsMaxLength.toDouble() / 2 + sixelWidth) * 1.02).toInt()
                        val sixelArgsExpectedCapacity = Math.min(sixelArgsPartsExpectedLength, sixelArgsExpectedLength)
                        if (sixelArgsExpectedCapacity > SIXEL_ARGS__INITIAL_CAPACITY) {
                            mSixelArgsCapacity = sixelArgsExpectedCapacity
                        }

                        mScreen.sixelResize(sixelWidth, sixelHeight)
                    }
                } else {
                    if (isValidDcs)
                        Logger.logError(mClient, LOG_TAG, "The sixel raster command expected 4 arguments at index " + index + " of sixel input: " + dcs)
                    isValidDcs = false
                    break
                }
            }
            else if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\u000C' || ch == '\r') {
                index++
            } else {
                // Invalid character.
                Logger.logError(mClient, LOG_TAG, "Invalid character '" + ch + "' (" + ch.code.toByte() + ") at index " + index + " of sixel input: " + dcs)
                isValidDcs = false
                break
            }
        }

        return isValidDcs
    }

    private fun nextTabStop(numTabs: Int): Int {
        var numTabs = numTabs
        for (i in mCursorCol + 1 until mColumns)
            if (mTabStop[i]) {
                numTabs--
                if (numTabs == 0) return Math.min(i, mRightMargin)
            }
        return mRightMargin - 1
    }

    /**
     * Process byte while in the {@link #ESC_CSI_UNSUPPORTED_PARAMETER_BYTE} or
     * {@link #ESC_CSI_UNSUPPORTED_INTERMEDIATE_BYTE} escape state.
     *
     * Parse unsupported parameter, intermediate and final bytes but ignore them.
     *
     * > For Control Sequence Introducer, ... the ESC [ is followed by
     * > - any number (including none) of "parameter bytes" in the range 0x30–0x3F (ASCII 0–9:;<=>?),
     * > - then by any number of "intermediate bytes" in the range 0x20–0x2F (ASCII space and !"#$%&'()*+,-./),
     * > - then finally by a single "final byte" in the range 0x40–0x7E (ASCII @A–Z[\]^_`a–z{|}~).
     *
     * - https://en.wikipedia.org/wiki/ANSI_escape_code#Control_Sequence_Introducer_commands
     * - https://invisible-island.net/xterm/ecma-48-parameter-format.html#section5.4
     */
    private fun doCsiUnsupportedParameterOrIntermediateByte(b: Int) {
        if (mEscapeState == ESC_CSI_UNSUPPORTED_PARAMETER_BYTE && b >= 0x30 && b <= 0x3F) {
            // Supported `0–9:;>?` or unsupported `<=` parameter byte after an
            // initial unsupported parameter byte in `doCsi()`, or a sequential parameter byte.
            continueSequence(ESC_CSI_UNSUPPORTED_PARAMETER_BYTE)
        } else if (b >= 0x20 && b <= 0x2F) {
            // Optional intermediate byte `!"#$%&'()*+,-./` after parameter or intermediate byte.
            continueSequence(ESC_CSI_UNSUPPORTED_INTERMEDIATE_BYTE)
        } else if (b >= 0x40 && b <= 0x7E) {
            // Final byte `@A–Z[\]^_`a–z{|}~` after parameter or intermediate byte.
            // Calling `unknownSequence()` would log an error with only a final byte, so ignore it for now.
            finishSequence()
        } else {
            unknownSequence(b)
        }
    }

    /** Process byte while in the {@link #ESC_CSI_QUESTIONMARK} escape state. */
    private fun doCsiQuestionMark(b: Int) {
        when (b) {
            'J'.code, // Selective erase in display (DECSED) - http://www.vt100.net/docs/vt510-rm/DECSED.
            'K'.code -> { // Selective erase in line (DECSEL) - http://vt100.net/docs/vt510-rm/DECSEL.
                mAboutToAutoWrap = false
                val fillChar = ' '.code
                var startCol = -1
                var startRow = -1
                var endCol = -1
                var endRow = -1
                val justRow = (b == 'K'.code)
                when (getArg0(0)) {
                    0 -> { // Erase from the active position to the end, inclusive (default).
                        startCol = mCursorCol
                        startRow = mCursorRow
                        endCol = mColumns
                        endRow = if (justRow) (mCursorRow + 1) else mRows
                    }
                    1 -> { // Erase from start to the active position, inclusive.
                        startCol = 0
                        startRow = if (justRow) mCursorRow else 0
                        endCol = mCursorCol + 1
                        endRow = mCursorRow + 1
                    }
                    2 -> { // Erase all of the display/line.
                        startCol = 0
                        startRow = if (justRow) mCursorRow else 0
                        endCol = mColumns
                        endRow = if (justRow) (mCursorRow + 1) else mRows
                    }
                    else ->
                        unknownSequence(b)
                }
                val style = getStyle()
                for (row in startRow until endRow) {
                    for (col in startCol until endCol) {
                        if ((TextStyle.decodeEffect(mScreen.getStyleAt(row, col)) and TextStyle.CHARACTER_ATTRIBUTE_PROTECTED) == 0)
                            mScreen.setChar(col, row, fillChar, style)
                    }
                }
            }
            'h'.code,
            'l'.code -> {
                if (mArgIndex >= mArgs.size) mArgIndex = mArgs.size - 1
                for (i in 0..mArgIndex)
                    doDecSetOrReset(b == 'h'.code, mArgs[i])
            }
            'n'.code -> { // Device Status Report (DSR, DEC-specific).
                when (getArg0(-1)) {
                    6 -> {
                        // Extended Cursor Position (DECXCPR - http://www.vt100.net/docs/vt510-rm/DECXCPR). Page=1.
                        mSession.write(String.format(Locale.US, "\u001B[?%d;%d;1R", mCursorRow + 1, mCursorCol + 1))
                    }
                    else -> {
                        finishSequence()
                        return
                    }
                }
            }
            'r'.code,
            's'.code -> {
                if (mArgIndex >= mArgs.size) mArgIndex = mArgs.size - 1
                for (i in 0..mArgIndex) {
                    val externalBit = mArgs[i]
                    val internalBit = mapDecSetBitToInternalBit(externalBit)
                    if (internalBit == -1) {
                        Logger.logWarn(mClient, LOG_TAG, "Ignoring request to save/recall decset bit=" + externalBit)
                    } else {
                        if (b == 's'.code) {
                            mSavedDecSetFlags = mSavedDecSetFlags or internalBit
                        } else {
                            doDecSetOrReset((mSavedDecSetFlags and internalBit) != 0, externalBit)
                        }
                    }
                }
            }
            '$'.code -> {
                continueSequence(ESC_CSI_QUESTIONMARK_ARG_DOLLAR)
                return
            }
            else ->
                parseArg(b)
        }
    }

    fun doDecSetOrReset(setting: Boolean, externalBit: Int) {
        val internalBit = mapDecSetBitToInternalBit(externalBit)
        if (internalBit != -1) {
            setDecsetinternalBit(internalBit, setting)
        }
        when (externalBit) {
            1 -> { // Application Cursor Keys (DECCKM).
            }
            3 -> { // Set: 132 column mode (. Reset: 80 column mode. ANSI name: DECCOLM.
                // We don't actually set/reset 132 cols, but we do want the side effects
                // (FIXME: Should only do this if the 95 DECSET bit (DECNCSM) is set, and if changing value?):
                // Sets the left, right, top and bottom scrolling margins to their default positions, which is important for
                // the "reset" utility to really reset the terminal:
                mLeftMargin = 0
                mTopMargin = 0
                mBottomMargin = mRows
                mRightMargin = mColumns
                // "DECCOLM resets vertical split screen mode (DECLRMM) to unavailable":
                setDecsetinternalBit(DECSET_BIT_LEFTRIGHT_MARGIN_MODE, false)
                // "Erases all data in page memory":
                blockClear(0, 0, mColumns, mRows)
                setCursorRowCol(0, 0)
            }
            4 -> { // DECSCLM-Scrolling Mode. Ignore.
            }
            5 -> { // Reverse video. No action.
            }
            6 -> { // Set: Origin Mode. Reset: Normal Cursor Mode. Ansi name: DECOM.
                if (setting) setCursorPosition(0, 0)
            }
            7, // Wrap-around bit, not specific action.
            8, // Auto-repeat Keys (DECARM). Do not implement.
            9, // X10 mouse reporting - outdated. Do not implement.
            12, // Control cursor blinking - ignore.
            25 -> { // Hide/show cursor - no action needed, renderer will check with shouldCursorBeVisible().
                val client = mClient
                if (client != null)
                    client.onTerminalCursorStateChange(setting)
            }
            40, // Allow 80 => 132 Mode, ignore.
            45, // TODO: Reverse wrap-around. Implement???
            66 -> { // Application keypad (DECNKM).
            }
            69 -> { // Left and right margin mode (DECLRMM).
                if (!setting) {
                    mLeftMargin = 0
                    mRightMargin = mColumns
                }
            }
            1000,
            1001,
            1002,
            1003,
            1004,
            1005, // UTF-8 mouse mode, ignore.
            1006, // SGR Mouse Mode
            1015,
            1034 -> { // Interpret "meta" key, sets eighth bit.
            }
            1048 -> { // Set: Save cursor as in DECSC. Reset: Restore cursor as in DECRC.
                if (setting)
                    saveCursor()
                else
                    restoreCursor()
            }
            47,
            1047,
            1049 -> {
                // Set: Save cursor as in DECSC and use Alternate Screen Buffer, clearing it first.
                // Reset: Use Normal Screen Buffer and restore cursor as in DECRC.
                val newScreen = if (setting) mAltBuffer else mMainBuffer
                if (newScreen != mScreen) {
                    val resized = !(newScreen.mColumns == mColumns && newScreen.mScreenRows == mRows)
                    if (setting) saveCursor()
                    mScreen = newScreen
                    if (!setting) {
                        val col = mSavedStateMain.mSavedCursorCol
                        val row = mSavedStateMain.mSavedCursorRow
                        restoreCursor()
                        if (resized) {
                            // Restore cursor position _not_ clipped to current screen (let resizeScreen() handle that):
                            mCursorCol = col
                            mCursorRow = row
                        }
                    }
                    // Check if buffer size needs to be updated:
                    if (resized) resizeScreen()
                    // Clear new screen if alt buffer:
                    if (newScreen == mAltBuffer)
                        newScreen.blockSet(0, 0, mColumns, mRows, ' '.code, getStyle())
                }
            }
            2004 -> {
                // Bracketed paste mode - setting bit is enough.
            }
            else ->
                unknownParameter(externalBit)
        }
    }

    private fun doCsiBiggerThan(b: Int) {
        when (b) {
            'c'.code -> { // "${CSI}>c" or "${CSI}>c". Secondary Device Attributes (DA2).
                // Originally this was used for the terminal to respond with "identification code, firmware version level,
                // and hardware options" (http://vt100.net/docs/vt510-rm/DA2), with the first "41" meaning the VT420
                // terminal type. This is not used anymore, but the second version level field has been changed by xterm
                // to mean it's release number ("patch numbers" listed at http://invisible-island.net/xterm/xterm.log.html),
                // and some applications use it as a feature check:
                // * tmux used to have a "xterm won't reach version 500 for a while so set that as the upper limit" check,
                // and then check "xterm_version > 270" if rectangular area operations such as DECCRA could be used.
                // * vim checks xterm version number >140 for "Request termcap/terminfo string" functionality >276 for SGR
                // mouse report.
                // The third number is a keyboard identifier not used nowadays.
                mSession.write("\u001B[>41;320;0c")
            }
            'm'.code -> {
                // https://bugs.launchpad.net/gnome-terminal/+bug/96676/comments/25
                // Depending on the first number parameter, this can set one of the xterm resources
                // modifyKeyboard, modifyCursorKeys, modifyFunctionKeys and modifyOtherKeys.
                // http://invisible-island.net/xterm/manpage/xterm.html#RESOURCES

                // * modifyKeyboard (parameter=1):
                // Normally xterm makes a special case regarding modifiers (shift, control, etc.) to handle special keyboard
                // layouts (legacy and vt220). This is done to provide compatible keyboards for DEC VT220 and related
                // terminals that implement user-defined keys (UDK).
                // The bits of the resource value selectively enable modification of the given category when these keyboards
                // are selected. The default is "0":
                // (0) The legacy/vt220 keyboards interpret only the Control-modifier when constructing numbered
                // function-keys. Other special keys are not modified.
                // (1) allows modification of the numeric keypad
                // (2) allows modification of the editing keypad
                // (4) allows modification of function-keys, overrides use of Shift-modifier for UDK.
                // (8) allows modification of other special keys

                // * modifyCursorKeys (parameter=2):
                // Tells how to handle the special case where Control-, Shift-, Alt- or Meta-modifiers are used to add a
                // parameter to the escape sequence returned by a cursor-key. The default is "2".
                // - Set it to -1 to disable it.
                // - Set it to 0 to use the old/obsolete behavior.
                // - Set it to 1 to prefix modified sequences with CSI.
                // - Set it to 2 to force the modifier to be the second parameter if it would otherwise be the first.
                // - Set it to 3 to mark the sequence with a ">" to hint that it is private.

                // * modifyFunctionKeys (parameter=3):
                // Tells how to handle the special case where Control-, Shift-, Alt- or Meta-modifiers are used to add a
                // parameter to the escape sequence returned by a (numbered) function-
                // key. The default is "2". The resource values are similar to modifyCursorKeys:
                // Set it to -1 to permit the user to use shift- and control-modifiers to construct function-key strings
                // using the normal encoding scheme.
                // - Set it to 0 to use the old/obsolete behavior.
                // - Set it to 1 to prefix modified sequences with CSI.
                // - Set it to 2 to force the modifier to be the second parameter if it would otherwise be the first.
                // - Set it to 3 to mark the sequence with a ">" to hint that it is private.
                // If modifyFunctionKeys is zero, xterm uses Control- and Shift-modifiers to allow the user to construct
                // numbered function-keys beyond the set provided by the keyboard:
                // (Control) adds the value given by the ctrlFKeys resource.
                // (Shift) adds twice the value given by the ctrlFKeys resource.
                // (Control/Shift) adds three times the value given by the ctrlFKeys resource.
                //
                // As a special case, legacy (when oldFunctionKeys is true) or vt220 (when sunKeyboard is true)
                // keyboards interpret only the Control-modifier when constructing numbered function-keys.
                // This is done to provide compatible keyboards for DEC VT220 and related terminals that
                // implement user-defined keys (UDK).

                // * modifyOtherKeys (parameter=4):
                // Like modifyCursorKeys, tells xterm to construct an escape sequence for other keys (such as "2") when
                // modified by Control-, Alt- or Meta-modifiers. This feature does not apply to function keys and
                // well-defined keys such as ESC or the control keys. The default is "0".
                // (0) disables this feature.
                // (1) enables this feature for keys except for those with well-known behavior, e.g., Tab, Backarrow and
                // some special control character cases, e.g., Control-Space to make a NUL.
                // (2) enables this feature for keys including the exceptions listed.
                Logger.logError(mClient, LOG_TAG, "(ignored) CSI > MODIFY RESOURCE: " + getArg0(-1) + " to " + getArg1(-1))
            }
            else ->
                parseArg(b)
        }
    }

    private fun startEscapeSequence() {
        mEscapeState = ESC
        mArgIndex = 0
        mArgs.fill(-1)
        mArgsSubParamsBitSet = 0
    }

    private fun doLinefeed() {
        val belowScrollingRegion = mCursorRow >= mBottomMargin
        val newCursorRow = mCursorRow + 1
        if (belowScrollingRegion) {
            // Move down (but not scroll) as long as we are above the last row.
            if (mCursorRow != mRows - 1) {
                setCursorRow(newCursorRow)
            }
        } else {
            var row = newCursorRow
            if (row == mBottomMargin) {
                scrollDownOneLine()
                row = mBottomMargin - 1
            }
            setCursorRow(row)
        }
    }

    private fun continueSequence(state: Int) {
        mEscapeState = state
        mContinueSequence = true
    }

    private fun doEscPound(b: Int) {
        when (b) {
            '8'.code -> // Esc # 8 - DEC screen alignment test - fill screen with E's.
                mScreen.blockSet(0, 0, mColumns, mRows, 'E'.code, getStyle())
            else ->
                unknownSequence(b)
        }
    }

    /** Encountering a character in the {@link #ESC} state. */
    private fun doEsc(b: Int) {
        when (b) {
            '#'.code ->
                continueSequence(ESC_POUND)
            '('.code ->
                continueSequence(ESC_SELECT_LEFT_PAREN)
            ')'.code ->
                continueSequence(ESC_SELECT_RIGHT_PAREN)
            '6'.code -> { // Back index (http://www.vt100.net/docs/vt510-rm/DECBI). Move left, insert blank column if start.
                if (mCursorCol > mLeftMargin) {
                    mCursorCol--
                } else {
                    val rows = mBottomMargin - mTopMargin
                    mScreen.blockCopy(mLeftMargin, mTopMargin, mRightMargin - mLeftMargin - 1, rows, mLeftMargin + 1, mTopMargin)
                    mScreen.blockSet(mLeftMargin, mTopMargin, 1, rows, ' '.code, TextStyle.encode(mForeColor, mBackColor, 0))
                }
            }
            '7'.code -> // DECSC save cursor - http://www.vt100.net/docs/vt510-rm/DECSC
                saveCursor()
            '8'.code -> // DECRC restore cursor - http://www.vt100.net/docs/vt510-rm/DECRC
                restoreCursor()
            '9'.code -> { // Forward Index (http://www.vt100.net/docs/vt510-rm/DECFI). Move right, insert blank column if end.
                if (mCursorCol < mRightMargin - 1) {
                    mCursorCol++
                } else {
                    val rows = mBottomMargin - mTopMargin
                    mScreen.blockCopy(mLeftMargin + 1, mTopMargin, mRightMargin - mLeftMargin - 1, rows, mLeftMargin, mTopMargin)
                    mScreen.blockSet(mRightMargin - 1, mTopMargin, 1, rows, ' '.code, TextStyle.encode(mForeColor, mBackColor, 0))
                }
            }
            'c'.code -> { // RIS - Reset to Initial State (http://vt100.net/docs/vt510-rm/RIS).
                reset()
                mMainBuffer.clearTranscript()
                blockClear(0, 0, mColumns, mRows)
                setCursorPosition(0, 0)
            }
            'D'.code -> // INDEX
                doLinefeed()
            'E'.code -> { // Next line (http://www.vt100.net/docs/vt510-rm/NEL).
                setCursorCol(if (isDecsetInternalBitSet(DECSET_BIT_ORIGIN_MODE)) mLeftMargin else 0)
                doLinefeed()
            }
            'F'.code -> // Cursor to lower-left corner of screen
                setCursorRowCol(0, mBottomMargin - 1)
            'H'.code -> // Tab set
                mTabStop[mCursorCol] = true
            'M'.code -> { // "${ESC}M" - reverse index (RI).
                // http://www.vt100.net/docs/vt100-ug/chapter3.html: "Move the active position to the same horizontal
                // position on the preceding line. If the active position is at the top margin, a scroll down is performed".
                if (mCursorRow <= mTopMargin) {
                    mScreen.blockCopy(mLeftMargin, mTopMargin, mRightMargin - mLeftMargin, mBottomMargin - (mTopMargin + 1), mLeftMargin, mTopMargin + 1)
                    blockClear(mLeftMargin, mTopMargin, mRightMargin - mLeftMargin)
                } else {
                    mCursorRow--
                }
            }
            'N'.code, // SS2, ignore.
            '0'.code -> { // SS3, ignore.
            }
            'P'.code -> { // Device control string
                clearTerminalControlArgs()
                clearDcsTypeVariables()
                continueSequence(ESC_DCS)
            }
            '['.code ->
                continueSequence(ESC_CSI)
            '='.code -> // DECKPAM
                setDecsetinternalBit(DECSET_BIT_APPLICATION_KEYPAD, true)
            ']'.code -> { // OSC
                clearTerminalControlArgs()
                clearOscTypeVariables()
                continueSequence(ESC_OSC)
            }
            '>'.code -> // DECKPNM
                setDecsetinternalBit(DECSET_BIT_APPLICATION_KEYPAD, false)
            '_'.code -> { // APC - Application Program Command.
                clearTerminalControlArgs()
                continueSequence(ESC_APC)
            }
            else ->
                unknownSequence(b)
        }
    }

    /** DECSC save cursor - http://www.vt100.net/docs/vt510-rm/DECSC . See {@link #restoreCursor()}. */
    private fun saveCursor() {
        val state = if (mScreen == mMainBuffer) mSavedStateMain else mSavedStateAlt
        state.mSavedCursorRow = mCursorRow
        state.mSavedCursorCol = mCursorCol
        state.mSavedEffect = mEffect
        state.mSavedForeColor = mForeColor
        state.mSavedBackColor = mBackColor
        state.mSavedDecFlags = mCurrentDecSetFlags
        state.mUseLineDrawingG0 = mUseLineDrawingG0
        state.mUseLineDrawingG1 = mUseLineDrawingG1
        state.mUseLineDrawingUsesG0 = mUseLineDrawingUsesG0
    }

    /** DECRS restore cursor - http://www.vt100.net/docs/vt510-rm/DECRC. See {@link #saveCursor()}. */
    private fun restoreCursor() {
        val state = if (mScreen == mMainBuffer) mSavedStateMain else mSavedStateAlt
        setCursorRowCol(state.mSavedCursorRow, state.mSavedCursorCol)
        mEffect = state.mSavedEffect
        mForeColor = state.mSavedForeColor
        mBackColor = state.mSavedBackColor
        val mask = (DECSET_BIT_AUTOWRAP or DECSET_BIT_ORIGIN_MODE)
        mCurrentDecSetFlags = (mCurrentDecSetFlags and mask.inv()) or (state.mSavedDecFlags and mask)
        mUseLineDrawingG0 = state.mUseLineDrawingG0
        mUseLineDrawingG1 = state.mUseLineDrawingG1
        mUseLineDrawingUsesG0 = state.mUseLineDrawingUsesG0
    }

    /** Following a CSI - Control Sequence Introducer, "\033[". {@link #ESC_CSI}. */
    private fun doCsi(b: Int) {
        when (b) {
            '!'.code ->
                continueSequence(ESC_CSI_EXCLAMATION)
            '"'.code ->
                continueSequence(ESC_CSI_DOUBLE_QUOTE)
            '\''.code ->
                continueSequence(ESC_CSI_SINGLE_QUOTE)
            '$'.code ->
                continueSequence(ESC_CSI_DOLLAR)
            '*'.code ->
                continueSequence(ESC_CSI_ARGS_ASTERIX)
            '@'.code -> {
                // "CSI{n}@" - Insert ${n} space characters (ICH) - http://www.vt100.net/docs/vt510-rm/ICH.
                mAboutToAutoWrap = false
                val columnsAfterCursor = mColumns - mCursorCol
                val spacesToInsert = Math.min(getArg0(1), columnsAfterCursor)
                val charsToMove = columnsAfterCursor - spacesToInsert
                mScreen.blockCopy(mCursorCol, mCursorRow, charsToMove, 1, mCursorCol + spacesToInsert, mCursorRow)
                blockClear(mCursorCol, mCursorRow, spacesToInsert)
            }
            'A'.code -> // "CSI${n}A" - Cursor up (CUU) ${n} rows.
                setCursorRow(Math.max(0, mCursorRow - getArg0(1)))
            'B'.code -> // "CSI${n}B" - Cursor down (CUD) ${n} rows.
                setCursorRow(Math.min(mRows - 1, mCursorRow + getArg0(1)))
            'C'.code, // "CSI${n}C" - Cursor forward (CUF).
            'a'.code -> // "CSI${n}a" - Horizontal position relative (HPR). From ISO-6428/ECMA-48.
                setCursorCol(Math.min(mRightMargin - 1, mCursorCol + getArg0(1)))
            'D'.code -> // "CSI${n}D" - Cursor backward (CUB) ${n} columns.
                setCursorCol(Math.max(mLeftMargin, mCursorCol - getArg0(1)))
            'E'.code -> // "CSI{n}E - Cursor Next Line (CNL). From ISO-6428/ECMA-48.
                setCursorPosition(0, mCursorRow + getArg0(1))
            'F'.code -> // "CSI{n}F - Cursor Previous Line (CPL). From ISO-6428/ECMA-48.
                setCursorPosition(0, mCursorRow - getArg0(1))
            'G'.code -> // "CSI${n}G" - Cursor horizontal absolute (CHA) to column ${n}.
                setCursorCol(Math.min(Math.max(1, getArg0(1)), mColumns) - 1)
            'H'.code, // "${CSI}${ROW};${COLUMN}H" - Cursor position (CUP).
            'f'.code -> // "${CSI}${ROW};${COLUMN}f" - Horizontal and Vertical Position (HVP).
                setCursorPosition(getArg1(1) - 1, getArg0(1) - 1)
            'I'.code -> // Cursor Horizontal Forward Tabulation (CHT). Move the active position n tabs forward.
                setCursorCol(nextTabStop(getArg0(1)))
            'J'.code -> { // "${CSI}${0,1,2,3}J" - Erase in Display (ED)
                // ED ignores the scrolling margins.
                when (getArg0(0)) {
                    0 -> { // Erase from the active position to the end of the screen, inclusive (default).
                        blockClear(mCursorCol, mCursorRow, mColumns - mCursorCol)
                        blockClear(0, mCursorRow + 1, mColumns, mRows - (mCursorRow + 1))
                    }
                    1 -> { // Erase from start of the screen to the active position, inclusive.
                        blockClear(0, 0, mColumns, mCursorRow)
                        blockClear(0, mCursorRow, mCursorCol + 1)
                    }
                    2 -> { // Erase all of the display - all lines are erased, changed to single-width, and the cursor does not
                        // move..
                        blockClear(0, 0, mColumns, mRows)
                    }
                    3 -> { // Delete all lines saved in the scrollback buffer (xterm etc)
                        mMainBuffer.clearTranscript()
                    }
                    else -> {
                        unknownSequence(b)
                        return
                    }
                }
                mAboutToAutoWrap = false
            }
            'K'.code -> { // "CSI{n}K" - Erase in line (EL).
                when (getArg0(0)) {
                    0 -> // Erase from the cursor to the end of the line, inclusive (default)
                        blockClear(mCursorCol, mCursorRow, mColumns - mCursorCol)
                    1 -> // Erase from the start of the screen to the cursor, inclusive.
                        blockClear(0, mCursorRow, mCursorCol + 1)
                    2 -> // Erase all of the line.
                        blockClear(0, mCursorRow, mColumns)
                    else -> {
                        unknownSequence(b)
                        return
                    }
                }
                mAboutToAutoWrap = false
            }
            'L'.code -> { // "${CSI}{N}L" - insert ${N} lines (IL).
                val linesAfterCursor = mBottomMargin - mCursorRow
                val linesToInsert = Math.min(getArg0(1), linesAfterCursor)
                val linesToMove = linesAfterCursor - linesToInsert
                mScreen.blockCopy(0, mCursorRow, mColumns, linesToMove, 0, mCursorRow + linesToInsert)
                blockClear(0, mCursorRow, mColumns, linesToInsert)
            }
            'M'.code -> { // "${CSI}${N}M" - delete N lines (DL).
                mAboutToAutoWrap = false
                val linesAfterCursor = mBottomMargin - mCursorRow
                val linesToDelete = Math.min(getArg0(1), linesAfterCursor)
                val linesToMove = linesAfterCursor - linesToDelete
                mScreen.blockCopy(0, mCursorRow + linesToDelete, mColumns, linesToMove, 0, mCursorRow)
                blockClear(0, mCursorRow + linesToMove, mColumns, linesToDelete)
            }
            'P'.code -> { // "${CSI}{N}P" - delete ${N} characters (DCH).
                // http://www.vt100.net/docs/vt510-rm/DCH: "If ${N} is greater than the number of characters between the
                // cursor and the right margin, then DCH only deletes the remaining characters.
                // As characters are deleted, the remaining characters between the cursor and right margin move to the left.
                // Character attributes move with the characters. The terminal adds blank spaces with no visual character
                // attributes at the right margin. DCH has no effect outside the scrolling margins."
                mAboutToAutoWrap = false
                val cellsAfterCursor = mColumns - mCursorCol
                val cellsToDelete = Math.min(getArg0(1), cellsAfterCursor)
                val cellsToMove = cellsAfterCursor - cellsToDelete
                mScreen.blockCopy(mCursorCol + cellsToDelete, mCursorRow, cellsToMove, 1, mCursorCol, mCursorRow)
                blockClear(mCursorCol + cellsToMove, mCursorRow, cellsToDelete)
            }
            'S'.code -> { // "${CSI}${N}S" - scroll up ${N} lines (default = 1) (SU).
                val linesToScroll = getArg0(1)
                for (i in 0 until linesToScroll)
                    scrollDownOneLine()
            }
            'T'.code -> {
                if (mArgIndex == 0) {
                    // "${CSI}${N}T" - Scroll down N lines (default = 1) (SD).
                    // http://vt100.net/docs/vt510-rm/SD: "N is the number of lines to move the user window up in page
                    // memory. N new lines appear at the top of the display. N old lines disappear at the bottom of the
                    // display. You cannot pan past the top margin of the current page".
                    val linesToScrollArg = getArg0(1)
                    val linesBetweenTopAndBottomMargins = mBottomMargin - mTopMargin
                    val linesToScroll = Math.min(linesBetweenTopAndBottomMargins, linesToScrollArg)
                    mScreen.blockCopy(mLeftMargin, mTopMargin, mRightMargin - mLeftMargin, linesBetweenTopAndBottomMargins - linesToScroll, mLeftMargin, mTopMargin + linesToScroll)
                    blockClear(mLeftMargin, mTopMargin, mRightMargin - mLeftMargin, linesToScroll)
                } else {
                    // "${CSI}${func};${startx};${starty};${firstrow};${lastrow}T" - initiate highlight mouse tracking.
                    unimplementedSequence(b)
                }
            }
            'X'.code -> { // "${CSI}${N}X" - Erase ${N:=1} character(s) (ECH). FIXME: Clears character attributes?
                mAboutToAutoWrap = false
                mScreen.blockSet(mCursorCol, mCursorRow, Math.min(getArg0(1), mColumns - mCursorCol), 1, ' '.code, getStyle())
            }
            'Z'.code -> { // Cursor Backward Tabulation (CBT). Move the active position n tabs backward.
                var numberOfTabs = getArg0(1)
                var newCol = mLeftMargin
                for (i in mCursorCol - 1 downTo 0)
                    if (mTabStop[i]) {
                        numberOfTabs--
                        if (numberOfTabs == 0) {
                            newCol = Math.max(i, mLeftMargin)
                            break
                        }
                    }
                mCursorCol = newCol
            }
            '?'.code -> // Esc [ ? -- start of a private parameter byte
                continueSequence(ESC_CSI_QUESTIONMARK)
            '>'.code -> // "Esc [ >" -- start of a private parameter byte
                continueSequence(ESC_CSI_BIGGERTHAN)
            '<'.code, // "Esc [ <" -- start of a private parameter byte
            '='.code -> // "Esc [ =" -- start of a private parameter byte
                continueSequence(ESC_CSI_UNSUPPORTED_PARAMETER_BYTE)
            '`'.code -> // Horizontal position absolute (HPA - http://www.vt100.net/docs/vt510-rm/HPA).
                setCursorColRespectingOriginMode(getArg0(1) - 1)
            'b'.code -> { // Repeat the preceding graphic character Ps times (REP).
                if (mLastEmittedCodePoint != -1) {
                    val numRepeat = getArg0(1)
                    for (i in 0 until numRepeat) emitCodePoint(mLastEmittedCodePoint)
                }
            }
            'c'.code -> { // Primary Device Attributes (http://www.vt100.net/docs/vt510-rm/DA1) if argument is missing or zero.
                // The important part that may still be used by some (tmux stores this value but does not currently use it)
                // is the first response parameter identifying the terminal service class, where we send 64 for "vt420".
                // This is followed by a list of attributes which is probably unused by applications. Send like xterm.
                if (getArg0(0) == 0) mSession.write("\u001B[?64;1;2;4;6;9;15;18;21;22c")
            }
            'd'.code -> // ESC [ Pn d - Vert Position Absolute
                setCursorRow(Math.min(Math.max(1, getArg0(1)), mRows) - 1)
            'e'.code -> // Vertical Position Relative (VPR). From ISO-6429 (ECMA-48).
                setCursorPosition(mCursorCol, mCursorRow + getArg0(1))
            // case 'f': "${CSI}${ROW};${COLUMN}f" - Horizontal and Vertical Position (HVP). Grouped with case 'H'.
            'g'.code -> { // Clear tab stop
                when (getArg0(0)) {
                    0 ->
                        mTabStop[mCursorCol] = false
                    3 ->
                        for (i in 0 until mColumns) {
                            mTabStop[i] = false
                        }
                    else -> {
                        // Specified to have no effect.
                    }
                }
            }
            'h'.code -> // Set Mode
                doSetMode(true)
            'l'.code -> // Reset Mode
                doSetMode(false)
            'm'.code -> // Esc [ Pn m - character attributes. (can have up to 16 numerical arguments)
                selectGraphicRendition()
            'n'.code -> { // Esc [ Pn n - ECMA-48 Status Report Commands
                // sendDeviceAttributes()
                when (getArg0(0)) {
                    5 -> { // Device status report (DSR):
                        // Answer is ESC [ 0 n (Terminal OK).
                        val dsr = byteArrayOf(27.toByte(), '['.code.toByte(), '0'.code.toByte(), 'n'.code.toByte())
                        mSession.write(dsr, 0, dsr.size)
                    }
                    6 -> { // Cursor position report (CPR):
                        // Answer is ESC [ y ; x R, where x,y is
                        // the cursor location.
                        mSession.write(String.format(Locale.US, "\u001B[%d;%dR", mCursorRow + 1, mCursorCol + 1))
                    }
                    else -> {
                    }
                }
            }
            'r'.code -> { // "CSI${top};${bottom}r" - set top and bottom Margins (DECSTBM).
                // https://vt100.net/docs/vt510-rm/DECSTBM.html
                // The top margin defaults to 1, the bottom margin defaults to mRows.
                // The escape sequence numbers top 1..23, but we number top 0..22.
                // The escape sequence numbers bottom 2..24, and so do we (because we use a zero based numbering
                // scheme, but we store the first line below the bottom-most scrolling line.
                // As a result, we adjust the top line by -1, but we leave the bottom line alone.
                // Also require that top + 2 <= bottom.
                mTopMargin = Math.max(0, Math.min(getArg0(1) - 1, mRows - 2))
                mBottomMargin = Math.max(mTopMargin + 2, Math.min(getArg1(mRows), mRows))

                // DECSTBM moves the cursor to column 1, line 1 of the page respecting origin mode.
                setCursorPosition(0, 0)
            }
            's'.code -> {
                if (isDecsetInternalBitSet(DECSET_BIT_LEFTRIGHT_MARGIN_MODE)) {
                    // Set left and right margins (DECSLRM - http://www.vt100.net/docs/vt510-rm/DECSLRM).
                    mLeftMargin = Math.min(getArg0(1) - 1, mColumns - 2)
                    mRightMargin = Math.max(mLeftMargin + 1, Math.min(getArg1(mColumns), mColumns))
                    // DECSLRM moves the cursor to column 1, line 1 of the page.
                    setCursorPosition(0, 0)
                } else {
                    // Save cursor (ANSI.SYS), available only when DECLRMM is disabled.
                    saveCursor()
                }
            }
            't'.code -> { // Window manipulation (from dtterm, as well as extensions)
                when (getArg0(0)) {
                    11 -> // Report xterm window state. If the xterm window is open (non-iconified), it returns CSI 1 t .
                        mSession.write("\u001B[1t")
                    13 -> // Report xterm window position. Result is CSI 3 ; x ; y t
                        mSession.write("\u001B[3;0;0t")
                    14 -> // Report xterm window in pixels. Result is CSI 4 ; height ; width t
                        mSession.write(String.format(Locale.US, "\u001B[4;%d;%dt", mRows * mCellHeightPixels, mColumns * mCellWidthPixels))
                    16 -> // Report xterm character cell size in pixels. Result is CSI 6 ; height ; width t
                        mSession.write(String.format(Locale.US, "\u001B[6;%d;%dt", mCellHeightPixels, mCellWidthPixels))
                    18 -> // Report the size of the text area in characters. Result is CSI 8 ; height ; width t
                        mSession.write(String.format(Locale.US, "\u001B[8;%d;%dt", mRows, mColumns))
                    19 -> { // Report the size of the screen in characters. Result is CSI 9 ; height ; width t
                        // We report the same size as the view, since it's the view really isn't resizable from the shell.
                        mSession.write(String.format(Locale.US, "\u001B[9;%d;%dt", mRows, mColumns))
                    }
                    20 -> // Report xterm windows icon label. Result is OSC L label ST. Disabled due to security concerns:
                        mSession.write("\u001B]LIconLabel\u001B\\")
                    21 -> // Report xterm windows title. Result is OSC l label ST. Disabled due to security concerns:
                        mSession.write("\u001B]l\u001B\\")
                    22 -> {
                        // 22;0 -> Save xterm icon and window title on stack.
                        // 22;1 -> Save xterm icon title on stack.
                        // 22;2 -> Save xterm window title on stack.
                        mTitleStack.push(mTitle)
                        if (mTitleStack.size > 20) {
                            // Limit size
                            mTitleStack.removeAt(0)
                        }
                    }
                    23 -> // Like 22 above but restore from stack.
                        if (!mTitleStack.isEmpty()) setTitle(mTitleStack.pop())
                    else -> {
                        // Ignore window manipulation.
                    }
                }
            }
            'u'.code -> // Restore cursor (ANSI.SYS).
                restoreCursor()
            ' '.code ->
                continueSequence(ESC_CSI_ARGS_SPACE)
            else ->
                parseArg(b)
        }
    }

    /** Select Graphic Rendition (SGR) - see http://en.wikipedia.org/wiki/ANSI_escape_code#graphics. */
    private fun selectGraphicRendition() {
        if (mArgIndex >= mArgs.size) mArgIndex = mArgs.size - 1
        var i = 0
        while (i <= mArgIndex) {
            // Skip leading sub parameters:
            if ((mArgsSubParamsBitSet and (1 shl i)) == 0) {
                var code = getArg(i, 0, false)
                if (code < 0 && mArgIndex > 0) {
                    i++
                    continue
                }
                if (code < 0) {
                    code = 0
                }
                if (code == 0) { // reset
                    mForeColor = TextStyle.COLOR_INDEX_FOREGROUND
                    mBackColor = TextStyle.COLOR_INDEX_BACKGROUND
                    mEffect = 0
                } else if (code == 1) {
                    mEffect = mEffect or TextStyle.CHARACTER_ATTRIBUTE_BOLD
                } else if (code == 2) {
                    mEffect = mEffect or TextStyle.CHARACTER_ATTRIBUTE_DIM
                } else if (code == 3) {
                    mEffect = mEffect or TextStyle.CHARACTER_ATTRIBUTE_ITALIC
                } else if (code == 4) {
                    if (i + 1 <= mArgIndex && ((mArgsSubParamsBitSet and (1 shl (i + 1))) != 0)) {
                        // Sub parameter, see https://sw.kovidgoyal.net/kitty/underlines/
                        i++
                        if (mArgs[i] == 0) {
                            // No underline.
                            mEffect = mEffect and TextStyle.CHARACTER_ATTRIBUTE_UNDERLINE.inv()
                        } else {
                            // Different variations of underlines: https://sw.kovidgoyal.net/kitty/underlines/
                            mEffect = mEffect or TextStyle.CHARACTER_ATTRIBUTE_UNDERLINE
                        }
                    } else {
                        mEffect = mEffect or TextStyle.CHARACTER_ATTRIBUTE_UNDERLINE
                    }
                } else if (code == 5) {
                    mEffect = mEffect or TextStyle.CHARACTER_ATTRIBUTE_BLINK
                } else if (code == 7) {
                    mEffect = mEffect or TextStyle.CHARACTER_ATTRIBUTE_INVERSE
                } else if (code == 8) {
                    mEffect = mEffect or TextStyle.CHARACTER_ATTRIBUTE_INVISIBLE
                } else if (code == 9) {
                    mEffect = mEffect or TextStyle.CHARACTER_ATTRIBUTE_STRIKETHROUGH
                } else if (code == 10) {
                    // Exit alt charset (TERM=linux) - ignore.
                } else if (code == 11) {
                    // Enter alt charset (TERM=linux) - ignore.
                } else if (code == 22) { // Normal color or intensity, neither bright, bold nor faint.
                    mEffect = mEffect and (TextStyle.CHARACTER_ATTRIBUTE_BOLD or TextStyle.CHARACTER_ATTRIBUTE_DIM).inv()
                } else if (code == 23) { // not italic, but rarely used as such; clears standout with TERM=screen
                    mEffect = mEffect and TextStyle.CHARACTER_ATTRIBUTE_ITALIC.inv()
                } else if (code == 24) { // underline: none
                    mEffect = mEffect and TextStyle.CHARACTER_ATTRIBUTE_UNDERLINE.inv()
                } else if (code == 25) { // blink: none
                    mEffect = mEffect and TextStyle.CHARACTER_ATTRIBUTE_BLINK.inv()
                } else if (code == 27) { // image: positive
                    mEffect = mEffect and TextStyle.CHARACTER_ATTRIBUTE_INVERSE.inv()
                } else if (code == 28) {
                    mEffect = mEffect and TextStyle.CHARACTER_ATTRIBUTE_INVISIBLE.inv()
                } else if (code == 29) {
                    mEffect = mEffect and TextStyle.CHARACTER_ATTRIBUTE_STRIKETHROUGH.inv()
                } else if (code >= 30 && code <= 37) {
                    mForeColor = code - 30
                } else if (code == 38 || code == 48 || code == 58) {
                    // Extended set foreground(38)/background(48)/underline(58) color.
                    // This is followed by either "2;$R;$G;$B" to set a 24-bit color or
                    // "5;$INDEX" to set an indexed color.
                    if (i + 2 > mArgIndex) {
                        i++
                        continue
                    }
                    val firstArg = mArgs[i + 1]
                    if (firstArg == 2) {
                        if (i + 4 > mArgIndex) {
                            Logger.logWarn(mClient, LOG_TAG, "Too few CSI" + code + ";2 RGB arguments")
                        } else {
                            val red = getArg(i + 2, 0, false)
                            val green = getArg(i + 3, 0, false)
                            val blue = getArg(i + 4, 0, false)

                            if (red < 0 || green < 0 || blue < 0 || red > 255 || green > 255 || blue > 255) {
                                finishSequenceAndLogError("Invalid RGB: " + red + "," + green + "," + blue)
                            } else {
                                val argbColor = 0xff_00_00_00.toInt() or (red shl 16) or (green shl 8) or blue
                                when (code) {
                                    38 -> mForeColor = argbColor
                                    48 -> mBackColor = argbColor
                                    58 -> mUnderlineColor = argbColor
                                }
                            }
                            i += 4 // "2;P_r;P_g;P_r"
                        }
                    } else if (firstArg == 5) {
                        val color = getArg(i + 2, 0, false)
                        i += 2 // "5;P_s"
                        if (color >= 0 && color < TextStyle.NUM_INDEXED_COLORS) {
                            when (code) {
                                38 -> mForeColor = color
                                48 -> mBackColor = color
                                58 -> mUnderlineColor = color
                            }
                        } else {
                            if (LOG_ESCAPE_SEQUENCES) Logger.logWarn(mClient, LOG_TAG, "Invalid color index: " + color)
                        }
                    } else {
                        finishSequenceAndLogError("Invalid ISO-8613-3 SGR first argument: " + firstArg)
                    }
                } else if (code == 39) { // Set default foreground color.
                    mForeColor = TextStyle.COLOR_INDEX_FOREGROUND
                } else if (code >= 40 && code <= 47) { // Set background color.
                    mBackColor = code - 40
                } else if (code == 49) { // Set default background color.
                    mBackColor = TextStyle.COLOR_INDEX_BACKGROUND
                } else if (code == 59) { // Set default underline color.
                    mUnderlineColor = TextStyle.COLOR_INDEX_FOREGROUND
                } else if (code >= 90 && code <= 97) { // Bright foreground colors (aixterm codes).
                    mForeColor = code - 90 + 8
                } else if (code >= 100 && code <= 107) { // Bright background color (aixterm codes).
                    mBackColor = code - 100 + 8
                } else {
                    if (LOG_ESCAPE_SEQUENCES)
                        Logger.logWarn(mClient, LOG_TAG, String.format("SGR unknown code %d", code))
                }
            }
            i++
        }
    }

    /**
     * Receive {@link #ESC_APC}. Check its docs for more info.
     */
    private fun receiveApc(b: Int) {
        when (b) {
            27 -> // Escape.
                continueSequence(ESC_APC__ESC)
            else -> {
                if (!collectTerminalControlArgs(b)) return
            }
        }
    }

    /**
     * Receive {@link #ESC_APC__ESC}. Check its docs for more info.
     */
    private fun receiveApcEsc(b: Int) {
        when (b) {
            '\\'.code -> {
                //doApc();
                //clearApcTypeVariables();
            }
            else -> {
                // The ESC character was not followed by a \, so insert the ESC and
                // the current character in arg buffer.
                if (!collectTerminalControlArgs(27)) return
                if (!collectTerminalControlArgs(b)) return
                continueSequence(ESC_APC)
            }
        }
    }

    /**
     * Clear {@link #ESC_APC} type variables.
     */
    fun clearApcTypeVariables() {}

    /**
     * Do {@link #ESC_APC}. Check its docs for more info.
     */
    private fun doApc() {}

    /**
     * Receive {@link #ESC_OSC}. Check its docs for more info.
     */
    private fun receiveOsc(b: Int) {
        when (b) {
            7 -> { // Bell.
                doOsc("\u0007")
                clearOscTypeVariables()
            }
            27 -> // Escape.
                continueSequence(ESC_OSC__ESC)
            else -> {
                if (!collectTerminalControlArgs(b)) return
                if (mOscType == -1) {
                    setOscTypeVariables()
                }
            }
        }
    }

    /**
     * Receive {@link #ESC_OSC__ESC}. Check its docs for more info.
     */
    private fun receiveOscEsc(b: Int) {
        when (b) {
            '\\'.code -> {
                doOsc("\u001B\\")
                clearOscTypeVariables()
            }
            else -> {
                // The ESC character was not followed by a \, so insert the ESC and
                // the current character in arg buffer.
                if (!collectTerminalControlArgs(27)) return
                if (!collectTerminalControlArgs(b)) return
                continueSequence(ESC_OSC)
            }
        }
    }

    /**
     * Set {@link #ESC_OSC} type variables.
     */
    fun setOscTypeVariables() {
        if (mOscType >= 0) return
        if (mTerminalControlArgs.indexOf(":") < 0) return

        var value = -1
        val argsLength = mTerminalControlArgs.length

        // Extract initial $value from initial "$value;..." string.
        for (i in 0 until argsLength) {
            val b = mTerminalControlArgs[i]
            if (b == ';') {
                mOscType = value
                break
            } else if (b >= '0' && b <= '9') {
                value = (if (value < 0) 0 else value * 10) + (b - '0')
            } else {
                mOscType = -2 // Unknown sequence.
                return
            }
        }

        if (mOscType >= 0) {
            var terminalControlArgsCapacity: Int? = null
            var terminalControlArgsMaxLength: Int? = null
            when (mOscType) {
                52 -> {
                    // Android has a `~100KB` limit for sharing/sending `UTF-16` encoded `String`
                    // with binder tranasactions, including clipboard, otherwise can result in a
                    // `TransactionTooLargeException`.
                    // - https://www.reddit.com/r/tasker/comments/prro8t/autoshare_crashed_when_i_pasted_the_file_path/
                    terminalControlArgsMaxLength = (100 * 1024) +
                        /* `52;Pc;` */ 10
                }
                1337 -> { // iTerm image command sends the base64 encoded image, do not run complex logic for each byte.
                    mIsFastPathOsc = true
                    mIgnoreCrLfForOsc = true
                    // Expect large amount of data for image bytes.
                    // `imgcat` utility splits image bytes into 200-byte chunks when sending with `FilePart=` commands.
                    // - https://github.com/gnachman/iTerm2-shell-integration/blob/d1d4012068c3c6761d5676c28ed73e0e2df2b715/utilities/imgcat#L89
                    // > Older versions of tmux have a limit of 256 bytes for the entire sequence.
                    // - https://iterm2.com/documentation-images.html
                    terminalControlArgsCapacity = 256
                    terminalControlArgsMaxLength = TerminalBitmap.MAX_BITMAP_SIZE +
                        /* `1337;File=inline=1;size=209715200;name=xxxxxxxxxxxxxxxxxxxxxxxx;width=8192px;height=8192px;preserveAspectRatio=1:` */ 150
                }
            }

            if (terminalControlArgsCapacity != null) {
                ensureTerminalControlArgsCapacity(terminalControlArgsCapacity)
            }

            if (terminalControlArgsMaxLength != null) {
                setTerminalControlArgsMaxLength(terminalControlArgsMaxLength)
            }
        }
    }

    /**
     * Clear {@link #ESC_OSC} type variables.
     */
    fun clearOscTypeVariables() {
        mOscType = -1
        mIsFastPathOsc = false
        mIgnoreCrLfForOsc = false
    }

    /**
     * Do {@link #ESC_OSC}. Check its docs for more info.
     *
     * This handles Set Text Parameters commands.
     *
     * The `bellOrStringTerminator` defines whether `OSC` command terminated with a `BEL` or `ST`.
     */
    private fun doOsc(bellOrStringTerminator: String) {
        var value = -1
        var textParameter = ""
        val argsLength = mTerminalControlArgs.length

        // Extract initial $value from initial "$value;..." string.
        for (i in 0 until argsLength) {
            val b = mTerminalControlArgs[i]
            if (b == ';') {
                // Do not make a copy of `mTerminalControlArgs` for lengthy commands.
                if (value != 1337) {
                    textParameter = mTerminalControlArgs.substring(i + 1)
                }
                break
            } else if (b >= '0' && b <= '9') {
                value = (if (value < 0) 0 else value * 10) + (b - '0')
            } else {
                unknownSequence(b.code)
                return
            }
        }

        when (value) {
            0, // Change icon name and window title to T.
            1, // Change icon name to T.
            2 -> // Change window title to T.
                setTitle(textParameter)
            4 -> {
                // P s = 4 ; c ; spec → Change Color Number c to the color specified by spec. This can be a name or RGB
                // specification as per XParseColor. Any number of c name pairs may be given. The color numbers correspond
                // to the ANSI colors 0-7, their bright versions 8-15, and if supported, the remainder of the 88-color or
                // 256-color table.
                // If a "?" is given rather than a name or RGB specification, xterm replies with a control sequence of the
                // same form which can be used to set the corresponding color. Because more than one pair of color number
                // and specification can be given in one control sequence, xterm can make more than one reply.
                var colorIndex = -1
                var parsingPairStart = -1
                var i = 0
                while (true) {
                    val endOfInput = i == textParameter.length
                    val b = if (endOfInput) ';' else textParameter[i]
                    if (b == ';') {
                        if (parsingPairStart < 0) {
                            parsingPairStart = i + 1
                        } else {
                            if (colorIndex < 0 || colorIndex > 255) {
                                unknownSequence(b.code)
                                return
                            } else {
                                mColors.tryParseColor(colorIndex, textParameter.substring(parsingPairStart, i))
                                mSession.onColorsChanged()
                                colorIndex = -1
                                parsingPairStart = -1
                            }
                        }
                    } else if (parsingPairStart >= 0) {
                        // We have passed a color index and are now going through color spec.
                    } else if (parsingPairStart < 0 && (b >= '0' && b <= '9')) {
                        colorIndex = (if (colorIndex < 0) 0 else colorIndex * 10) + (b - '0')
                    } else {
                        unknownSequence(b.code)
                        return
                    }
                    if (endOfInput) break
                    i++
                }
            }
            10, // Set foreground color.
            11, // Set background color.
            12 -> { // Set cursor color.
                var specialIndex = TextStyle.COLOR_INDEX_FOREGROUND + (value - 10)
                var lastSemiIndex = 0
                var charIndex = 0
                while (true) {
                    val endOfInput = charIndex == textParameter.length
                    if (endOfInput || textParameter[charIndex] == ';') {
                        try {
                            val colorSpec = textParameter.substring(lastSemiIndex, charIndex)
                            if (colorSpec == "?") {
                                // Report current color in the same format xterm and gnome-terminal does.
                                val rgb = mColors.mCurrentColors[specialIndex]
                                val r = (65535 * ((rgb and 0x00FF0000) shr 16)) / 255
                                val g = (65535 * ((rgb and 0x0000FF00) shr 8)) / 255
                                val b = (65535 * ((rgb and 0x000000FF))) / 255
                                mSession.write("\u001B]" + value + ";rgb:" + String.format(Locale.US, "%04x", r) + "/" + String.format(Locale.US, "%04x", g) + "/"
                                    + String.format(Locale.US, "%04x", b) + bellOrStringTerminator)
                            } else {
                                mColors.tryParseColor(specialIndex, colorSpec)
                                mSession.onColorsChanged()
                            }
                            specialIndex++
                            var shouldBreak = endOfInput || (specialIndex > TextStyle.COLOR_INDEX_CURSOR)
                            if (!shouldBreak) {
                                charIndex++
                                shouldBreak = charIndex >= textParameter.length
                            }
                            if (shouldBreak) break
                            lastSemiIndex = charIndex
                        } catch (e: NumberFormatException) {
                            // Ignore.
                        }
                    }
                    charIndex++
                }
            }
            52 -> { // Manipulate Selection Data. Skip the optional first selection parameter(s).
                val startIndex = textParameter.indexOf(";") + 1
                try {
                    val clipboardText = String(Base64.decode(textParameter.substring(startIndex), Base64.DEFAULT), StandardCharsets.UTF_8)
                    mSession.onCopyTextToClipboard(clipboardText)
                } catch (e: Exception) {
                    Logger.logError(mClient, LOG_TAG, "OSC Manipulate selection, invalid string '" + textParameter + "'")
                }
            }
            104 -> {
                // "104;$c" → Reset Color Number $c. It is reset to the color specified by the corresponding X
                // resource. Any number of c parameters may be given. These parameters correspond to the ANSI colors 0-7,
                // their bright versions 8-15, and if supported, the remainder of the 88-color or 256-color table. If no
                // parameters are given, the entire table will be reset.
                if (textParameter.isEmpty()) {
                    mColors.reset()
                    mSession.onColorsChanged()
                } else {
                    var lastIndex = 0
                    var charIndex = 0
                    while (true) {
                        val endOfInput = charIndex == textParameter.length
                        if (endOfInput || textParameter[charIndex] == ';') {
                            try {
                                val colorToReset = Integer.parseInt(textParameter.substring(lastIndex, charIndex))
                                mColors.reset(colorToReset)
                                mSession.onColorsChanged()
                                if (endOfInput) break
                                charIndex++
                                lastIndex = charIndex
                            } catch (e: NumberFormatException) {
                                // Ignore.
                            }
                        }
                        charIndex++
                    }
                }
            }
            110, // Reset foreground color.
            111, // Reset background color.
            112 -> { // Reset cursor color.
                mColors.reset(TextStyle.COLOR_INDEX_FOREGROUND + (value - 110))
                mSession.onColorsChanged()
            }
            119 -> { // Reset highlight color.
            }
            1337 -> { // iTerm image
                // - https://iterm2.com/documentation-images.html
                // - https://iterm2.com/documentation-escape-codes.html
                val controlCommandPrefix = mTerminalControlArgs.substring(5, Math.min(19, argsLength))

                if (controlCommandPrefix.startsWith("File=") ||
                    controlCommandPrefix.startsWith("MultipartFile=") ||
                    controlCommandPrefix.startsWith("FilePart=") ||
                    controlCommandPrefix == "FileEnd") {

                    var iTermImage: ITermImage? = null
                    var oscArgsCleared = false
                    var index = 0
                    // `File = [optional arguments] : base-64 encoded file contents ^G`
                    if (controlCommandPrefix.startsWith("File=")) {
                        if (mITermImage != null) {
                            Logger.logWarn(mClient, LOG_TAG, "A new iTerm 'File' command received while already processing a 'MultipartFile' command")
                            mITermImage = null // Unset old image.
                        }

                        iTermImage = ITermImage(mClient, /* multiPart */ false)
                        index = iTermImage.readArguments(this, mTerminalControlArgs, /* `1337;File=` */ 10)
                        if (index < 10 || !iTermImage.readImage(mTerminalControlArgs, index)) {
                            iTermImage = null
                        } else {
                            // Free image data from memory held in osc command arguments as it is no longer needed.
                            clearTerminalControlArgs()
                            oscArgsCleared = true
                            if (!iTermImage.decodeImage()) {
                                iTermImage = null
                            }
                        }
                    }
                    // `MultipartFile = [optional arguments] ^G`
                    else if (controlCommandPrefix.startsWith("MultipartFile=")) {
                        if (mITermImage != null) {
                            Logger.logWarn(mClient, LOG_TAG, "A new iTerm 'MultipartFile' command received while already processing a 'MultipartFile' command")
                            mITermImage = null // Unset old image.
                        }

                        iTermImage = ITermImage(mClient, /* multiPart */ true)
                        if (iTermImage.readArguments(this, mTerminalControlArgs, /* `1337;MultipartFile=` */ 19) < 19) {
                            iTermImage = null
                        } else {
                            mITermImage = iTermImage
                        }
                    }
                    // `FilePart = base64 encoded file contents ^G`
                    else if (controlCommandPrefix.startsWith("FilePart=")) {
                        if (mITermImage == null) {
                            Logger.logError(mClient, LOG_TAG, "An iTerm 'FilePart' command received without a 'MultipartFile' command preceding it")
                            return
                        }

                        if (!mITermImage!!.readImage(mTerminalControlArgs, /* `1337;FilePart=` */ 14)) {
                            mITermImage = null
                        }
                    }
                    // `FileEnd ^G`
                    else if (controlCommandPrefix == "FileEnd") {
                        if (mITermImage == null) {
                            Logger.logError(mClient, LOG_TAG, "An iTerm 'FileEnd' command received without a 'MultipartFile' command preceding it")
                            return
                        }

                        iTermImage = mITermImage
                        mITermImage = null // Free global reference so that memory is freed at end function.
                        if (
                            !iTermImage!!.setMultiPartImageRead() ||
                            !iTermImage.decodeImage()) {
                            iTermImage = null
                        }
                    }

                    // Free image data from memory held in osc command arguments as it is no longer needed.
                    if (!oscArgsCleared)
                        clearTerminalControlArgs()

                    if (iTermImage != null && iTermImage.isImageDecoded()) {
                        // Display image as inline in Terminal.
                        if (iTermImage.isInline()) {
                            val cursorDelta = mScreen.addTerminalBitmapForImage(iTermImage.getDecodedImage(),
                                mCursorCol, mCursorRow, mCellWidthPixels, mCellHeightPixels,
                                iTermImage.getWidth(), iTermImage.getHeight(),
                                iTermImage.shouldPreserveAspectRatio())

                            var col = cursorDelta[1] + mCursorCol
                            if (col < mColumns - 1) {
                                cursorDelta[0] -= 1
                            } else {
                                col = 0
                            }
                            while (cursorDelta[0] > 0) {
                                doLinefeed()
                                cursorDelta[0]--
                            }
                            mCursorCol = col
                        }
                        // Saving files in downloads folder is not supported currently.
                        else {}
                    }
                } else if (controlCommandPrefix.startsWith("ReportCellSize")) {
                    mSession.write(String.format(Locale.ENGLISH, "\u001B1337;ReportCellSize=%d;%d\u0007", mCellHeightPixels, mCellWidthPixels))
                }

                // Free image from memory for any non `MultipartFile=` related commands.
                mITermImage = null
            }
            else ->
                unknownParameter(value)
        }

        // Free image from memory if an incomplete `MultipartFile` command was received without a `FileEnd`.
        // The `mITermImage` cannot set to `null` in `clearOscTypeVariables()` as sequential
        // OSC commands will be received for `MultipartFile` commands, and the variable is required
        // to be set until the final `FileEnd` command is received.
        if (mITermImage != null && value != 1337) {
            mITermImage = null
        }

        finishSequence()
    }

    private fun blockClear(sx: Int, sy: Int, w: Int) {
        blockClear(sx, sy, w, 1)
    }

    private fun blockClear(sx: Int, sy: Int, w: Int, h: Int) {
        mScreen.blockSet(sx, sy, w, h, ' '.code, getStyle())
    }

    private fun getStyle(): Long {
        return TextStyle.encode(mForeColor, mBackColor, mEffect)
    }

    /** "CSI P_m h" for set or "CSI P_m l" for reset ANSI mode. */
    private fun doSetMode(newValue: Boolean) {
        val modeBit = getArg0(0)
        when (modeBit) {
            4 -> // Set="Insert Mode". Reset="Replace Mode". (IRM).
                mInsertMode = newValue
            20 -> { // Normal Linefeed (LNM).
                unknownParameter(modeBit)
                // http://www.vt100.net/docs/vt510-rm/LNM
            }
            34 -> {
                // Normal cursor visibility - when using TERM=screen, see
                // http://www.gnu.org/software/screen/manual/html_node/Control-Sequences.html
            }
            else ->
                unknownParameter(modeBit)
        }
    }

    /**
     * NOTE: The parameters of this function respect the {@link #DECSET_BIT_ORIGIN_MODE}. Use
     * {@link #setCursorRowCol(int, int)} for absolute pos.
     */
    private fun setCursorPosition(x: Int, y: Int) {
        val originMode = isDecsetInternalBitSet(DECSET_BIT_ORIGIN_MODE)
        val effectiveTopMargin = if (originMode) mTopMargin else 0
        val effectiveBottomMargin = if (originMode) mBottomMargin else mRows
        val effectiveLeftMargin = if (originMode) mLeftMargin else 0
        val effectiveRightMargin = if (originMode) mRightMargin else mColumns
        val newRow = Math.max(effectiveTopMargin, Math.min(effectiveTopMargin + y, effectiveBottomMargin - 1))
        val newCol = Math.max(effectiveLeftMargin, Math.min(effectiveLeftMargin + x, effectiveRightMargin - 1))
        setCursorRowCol(newRow, newCol)
    }

    private fun scrollDownOneLine() {
        mScrollCounter++
        val currentStyle = getStyle()
        if (mLeftMargin != 0 || mRightMargin != mColumns) {
            // Horizontal margin: Do not put anything into scroll history, just non-margin part of screen up.
            mScreen.blockCopy(mLeftMargin, mTopMargin + 1, mRightMargin - mLeftMargin, mBottomMargin - mTopMargin - 1, mLeftMargin, mTopMargin)
            // .. and blank bottom row between margins:
            mScreen.blockSet(mLeftMargin, mBottomMargin - 1, mRightMargin - mLeftMargin, 1, ' '.code, currentStyle)
        } else {
            mScreen.scrollDownOneLine(mTopMargin, mBottomMargin, currentStyle)
        }
    }

    /**
     * Process the next ASCII character of a parameter.
     *
     * <p>You must use the ; character to separate parameters and : to separate sub-parameters.
     *
     * <p>Parameter characters modify the action or interpretation of the sequence. Originally
     * you can use up to 16 parameters per sequence, but following at least xterm and alacritty
     * we use a common space for parameters and sub-parameters, allowing 32 in total.
     *
     * <p>All parameters are unsigned, positive decimal integers, with the most significant
     * digit sent first. Any parameter greater than 9999 (decimal) is set to 9999
     * (decimal). If you do not specify a value, a 0 value is assumed. A 0 value
     * or omitted parameter indicates a default value for the sequence. For most
     * sequences, the default value is 1.
     *
     * <p>References:
     * <a href="https://vt100.net/docs/vt510-rm/chapter4.html#S4.3.3">VT510 Video Terminal Programmer Information: Control Sequences</a>
     * <a href="https://github.com/alacritty/vte/issues/22">alacritty/vte: Implement colon separated CSI parameters</a>
     * */
    private fun parseArg(b: Int) {
        if (b >= '0'.code && b <= '9'.code) {
            if (mArgIndex < mArgs.size) {
                val oldValue = mArgs[mArgIndex]
                val thisDigit = b - '0'.code
                var value: Int
                if (oldValue >= 0) {
                    value = oldValue * 10 + thisDigit
                } else {
                    value = thisDigit
                }
                if (value > 9999)
                    value = 9999
                mArgs[mArgIndex] = value
            }
            continueSequence(mEscapeState)
        } else if (b == ';'.code || b == ':'.code) {
            if (mArgIndex + 1 < mArgs.size) {
                mArgIndex++
                if (b == ':'.code) {
                    mArgsSubParamsBitSet = mArgsSubParamsBitSet or (1 shl mArgIndex)
                }
            } else {
                logError("Too many parameters when in state: " + mEscapeState)
            }
            continueSequence(mEscapeState)
        } else {
            unknownSequence(b)
        }
    }

    private fun getArg0(defaultValue: Int): Int {
        return getArg(0, defaultValue, true)
    }

    private fun getArg1(defaultValue: Int): Int {
        return getArg(1, defaultValue, true)
    }

    private fun getArg(index: Int, defaultValue: Int, treatZeroAsDefault: Boolean): Int {
        var result = mArgs[index]
        if (result < 0 || (result == 0 && treatZeroAsDefault)) {
            result = defaultValue
        }
        return result
    }

    /** Collect code point in {@link #mTerminalControlArgs}. */
    private fun collectTerminalControlArgs(b: Int): Boolean {
        // FIXME: Use `Logger.logErrorDebug()` and elsewhere in terminal code when support is added
        //  to prevent logging potentially private data to logcat unless user has increased log level.
        if (mTerminalControlArgs.length < mTerminalControlArgsMaxLength) {
            try {
                // Appending can cause an increase in capacity and cause an OOM.
                mTerminalControlArgs.appendCodePoint(b)
                continueSequence(mEscapeState)
                return true
            } catch (t: Throwable) {
                if (t is OutOfMemoryError) System.gc()
                Logger.logError(mClient, LOG_TAG, "Terminal control args collect failed for" +
                " char '" + b.toChar() + "' (numeric value=" + b + ") and" +
                " args string '" + mTerminalControlArgs.substring(0, Math.min(100, mTerminalControlArgs.length)) + "...' with length " + mTerminalControlArgs.length +
                ": " + t.message)
            }
        } else {
            Logger.logError(mClient, LOG_TAG, "Terminal control args input will" +
                " overflow max args length " + mTerminalControlArgsMaxLength + " for" +
                " char '" + b.toChar() + "' (numeric value=" + b + ") and" +
                " args string '" + mTerminalControlArgs.substring(0, Math.min(100, mTerminalControlArgs.length)) + "...' with length " + mTerminalControlArgs.length)
        }

        clearTerminalControlArgs()
        finishSequence()
        return false
    }

    /** Clear {@link #mTerminalControlArgs}. */
    private fun clearTerminalControlArgs() {
        mTerminalControlArgsMaxLength = TERMINAL_CONTROL_ARGS__DEFAULT_MAX_LENGTH

        if (mTerminalControlArgs.capacity() <= TERMINAL_CONTROL_ARGS__INITIAL_CAPACITY) {
            // Mark existing buffer as empty and reuse old array already allocated in
            // `StringBuffer` for future commands if required.
            mTerminalControlArgs.setLength(0)
        } else {
            // `setLength()` will only update internal length marker and not reduce internal array
            // capacity, and to deallocate extra memory `trimToSize()` needs to be called, which
            // creates another smaller array.
            // So just allocate a new object with an array with required initial capacity directly
            // instead of setting length to 0, then trimming to create a smaller array, then
            // increasing capacity again by creating a new array with required initial capacity by
            // calling `ensureCapacity()`.
            mTerminalControlArgs = StringBuilder(TERMINAL_CONTROL_ARGS__INITIAL_CAPACITY)
        }
    }

    /**
     * Ensure enough capacity for {@link #mTerminalControlArgs} to prevent repeated reallocation of
     * memory and copying as more data is received and appended, like with `append(char)`.
     *
     * The default capacity for {@link #mTerminalControlArgs} is defined by
     * {@link #TERMINAL_CONTROL_ARGS__INITIAL_CAPACITY}.
     *
     * By default, if `StringBuilder` reaches capacity, it sets new capacity to `(oldCapacity * 2) + 2`.
     * So if initial capacity is `16`, and data to be received is 1024 bytes, then 6 reallocations
     * will be done, so command processors should
     * - https://cs.android.com/android/platform/superproject/+/android-16.0.0_r1:libcore/ojluni/src/main/java/java/lang/AbstractStringBuilder.java;l=758
     * - https://cs.android.com/android/platform/superproject/+/android-16.0.0_r1:libcore/ojluni/src/main/java/java/lang/AbstractStringBuilder.java;l=183
     * - https://cs.android.com/android/platform/superproject/+/android-16.0.0_r1:libcore/ojluni/src/main/java/java/lang/AbstractStringBuilder.java;l=210
     *
     * See also {@link StringBuilder#ensureCapacity(int)}.
     *
     * @param capacity The new capacity.
     */
    private fun ensureTerminalControlArgsCapacity(capacity: Int) {
        mTerminalControlArgs.ensureCapacity(capacity)
    }

    /**
     * Set {@link #mTerminalControlArgsMaxLength} in case a command expects a larger input.
     *
     * @param length The new max length.
     */
    private fun setTerminalControlArgsMaxLength(length: Int) {
        if (length > 0) {
            mTerminalControlArgsMaxLength = length
        }
    }

    private fun unimplementedSequence(b: Int) {
        logError("Unimplemented sequence char '" + b.toChar() + "' (U+" + String.format("%04x", b) + ")")
        finishSequence()
    }

    private fun unknownSequence(b: Int) {
        logError("Unknown sequence char '" + b.toChar() + "' (numeric value=" + b + ")")
        finishSequence()
    }

    private fun unknownParameter(parameter: Int) {
        logError("Unknown parameter: " + parameter)
        finishSequence()
    }

    private fun logError(errorType: String) {
        if (LOG_ESCAPE_SEQUENCES) {
            val buf = StringBuilder()
            buf.append(errorType)
            buf.append(", escapeState=")
            buf.append(mEscapeState)
            var firstArg = true
            if (mArgIndex >= mArgs.size) mArgIndex = mArgs.size - 1
            for (i in 0..mArgIndex) {
                val value = mArgs[i]
                if (value >= 0) {
                    if (firstArg) {
                        firstArg = false
                        buf.append(", args={")
                    } else {
                        buf.append(',')
                    }
                    buf.append(value)
                }
            }
            if (!firstArg) buf.append('}')
            finishSequenceAndLogError(buf.toString())
        }
    }

    private fun finishSequenceAndLogError(error: String) {
        if (LOG_ESCAPE_SEQUENCES) Logger.logWarn(mClient, LOG_TAG, error)
        finishSequence()
    }

    private fun finishSequence() {
        mEscapeState = ESC_NONE
    }

    /**
     * Send a Unicode code point to the screen.
     *
     * @param codePoint The code point of the character to display
     */
    private fun emitCodePoint(codePoint: Int) {
        var codePoint = codePoint
        mLastEmittedCodePoint = codePoint
        if (if (mUseLineDrawingUsesG0) mUseLineDrawingG0 else mUseLineDrawingG1) {
            // http://www.vt100.net/docs/vt102-ug/table5-15.html.
            when (codePoint) {
                '_'.code ->
                    codePoint = ' '.code // Blank.
                '`'.code ->
                    codePoint = '◆'.code // Diamond.
                '0'.code ->
                    codePoint = '█'.code // Solid block;
                'a'.code ->
                    codePoint = '▒'.code // Checker board.
                'b'.code ->
                    codePoint = '\t'.code // Horizontal tab.
                'c'.code ->
                    codePoint = '\u000C'.code // Form feed.
                'd'.code ->
                    codePoint = '\r'.code // Carriage return.
                'e'.code ->
                    codePoint = ''.code // Linefeed.
                'f'.code ->
                    codePoint = '°'.code // Degree.
                'g'.code ->
                    codePoint = '±'.code // Plus-minus.
                'h'.code ->
                    codePoint = '\n'.code // Newline.
                'i'.code ->
                    codePoint = '\u000B'.code // Vertical tab.
                'j'.code ->
                    codePoint = '┘'.code // Lower right corner.
                'k'.code ->
                    codePoint = '┐'.code // Upper right corner.
                'l'.code ->
                    codePoint = '┌'.code // Upper left corner.
                'm'.code ->
                    codePoint = '└'.code // Left left corner.
                'n'.code ->
                    codePoint = '┼'.code // Crossing lines.
                'o'.code ->
                    codePoint = '⎺'.code // Horizontal line - scan 1.
                'p'.code ->
                    codePoint = '⎻'.code // Horizontal line - scan 3.
                'q'.code ->
                    codePoint = '─'.code // Horizontal line - scan 5.
                'r'.code ->
                    codePoint = '⎼'.code // Horizontal line - scan 7.
                's'.code ->
                    codePoint = '⎽'.code // Horizontal line - scan 9.
                't'.code ->
                    codePoint = '├'.code // T facing rightwards.
                'u'.code ->
                    codePoint = '┤'.code // T facing leftwards.
                'v'.code ->
                    codePoint = '┴'.code // T facing upwards.
                'w'.code ->
                    codePoint = '┬'.code // T facing downwards.
                'x'.code ->
                    codePoint = '│'.code // Vertical line.
                'y'.code ->
                    codePoint = '≤'.code // Less than or equal to.
                'z'.code ->
                    codePoint = '≥'.code // Greater than or equal to.
                '{'.code ->
                    codePoint = 'π'.code // Pi.
                '|'.code ->
                    codePoint = '≠'.code // Not equal to.
                '}'.code ->
                    codePoint = '£'.code // UK pound.
                '~'.code ->
                    codePoint = '·'.code // Centered dot.
            }
        }

        val autoWrap = isDecsetInternalBitSet(DECSET_BIT_AUTOWRAP)
        val displayWidth = WcWidth.width(codePoint)
        val cursorInLastColumn = mCursorCol == mRightMargin - 1

        if (autoWrap) {
            if (cursorInLastColumn && ((mAboutToAutoWrap && displayWidth == 1) || displayWidth == 2)) {
                mScreen.setLineWrap(mCursorRow)
                mCursorCol = mLeftMargin
                if (mCursorRow + 1 < mBottomMargin) {
                    mCursorRow++
                } else {
                    scrollDownOneLine()
                }
            }
        } else if (cursorInLastColumn && displayWidth == 2) {
            // The behaviour when a wide character is output with cursor in the last column when
            // autowrap is disabled is not obvious - it's ignored here.
            return
        }

        if (mInsertMode && displayWidth > 0) {
            // Move character to right one space.
            val destCol = mCursorCol + displayWidth
            if (destCol < mRightMargin)
                mScreen.blockCopy(mCursorCol, mCursorRow, mRightMargin - destCol, 1, destCol, mCursorRow)
        }

        val offsetDueToCombiningChar = if ((displayWidth <= 0 && mCursorCol > 0 && !mAboutToAutoWrap)) 1 else 0
        var column = mCursorCol - offsetDueToCombiningChar

        // Fix TerminalRow.setChar() ArrayIndexOutOfBoundsException index=-1 exception reported
        // The offsetDueToCombiningChar would never be 1 if mCursorCol was 0 to get column/index=-1,
        // so was mCursorCol changed after the offsetDueToCombiningChar conditional by another thread?
        // TODO: Check if there are thread synchronization issues with mCursorCol and mCursorRow, possibly causing others bugs too.
        if (column < 0) column = 0
        mScreen.setChar(column, mCursorRow, codePoint, getStyle())

        if (autoWrap && displayWidth > 0)
            mAboutToAutoWrap = (mCursorCol == mRightMargin - displayWidth)

        mCursorCol = Math.min(mCursorCol + displayWidth, mRightMargin - 1)
    }

    private fun setCursorRow(row: Int) {
        mCursorRow = row
        mAboutToAutoWrap = false
    }

    private fun setCursorCol(col: Int) {
        mCursorCol = col
        mAboutToAutoWrap = false
    }

    /** Set the cursor mode, but limit it to margins if {@link #DECSET_BIT_ORIGIN_MODE} is enabled. */
    private fun setCursorColRespectingOriginMode(col: Int) {
        setCursorPosition(col, mCursorRow)
    }

    /** TODO: Better name, distinguished from {@link #setCursorPosition(int, int)} by not regarding origin mode. */
    private fun setCursorRowCol(row: Int, col: Int) {
        mCursorRow = Math.max(0, Math.min(row, mRows - 1))
        mCursorCol = Math.max(0, Math.min(col, mColumns - 1))
        mAboutToAutoWrap = false
    }

    fun getScrollCounter(): Int {
        return mScrollCounter
    }

    fun clearScrollCounter() {
        mScrollCounter = 0
    }

    fun isAutoScrollDisabled(): Boolean {
        return mAutoScrollDisabled
    }

    fun toggleAutoScrollDisabled() {
        mAutoScrollDisabled = !mAutoScrollDisabled
    }

    fun getTopRow(): Int {
        return mTopRow
    }

    fun setTopRow(topRow: Int) {
        mTopRow = topRow
    }

    /** Reset terminal state so user can interact with it regardless of present state. */
    fun reset() {
        setCursorStyle()
        mArgIndex = 0
        mContinueSequence = false
        mEscapeState = ESC_NONE
        mInsertMode = false
        mTopMargin = 0
        mLeftMargin = 0
        mBottomMargin = mRows
        mRightMargin = mColumns
        mAboutToAutoWrap = false
        mForeColor = TextStyle.COLOR_INDEX_FOREGROUND
        mSavedStateMain.mSavedForeColor = TextStyle.COLOR_INDEX_FOREGROUND
        mSavedStateAlt.mSavedForeColor = TextStyle.COLOR_INDEX_FOREGROUND
        mBackColor = TextStyle.COLOR_INDEX_BACKGROUND
        mSavedStateMain.mSavedBackColor = TextStyle.COLOR_INDEX_BACKGROUND
        mSavedStateAlt.mSavedBackColor = TextStyle.COLOR_INDEX_BACKGROUND
        setDefaultTabStops()

        mUseLineDrawingG0 = false
        mUseLineDrawingG1 = false
        mUseLineDrawingUsesG0 = true

        mSavedStateMain.mSavedCursorRow = 0
        mSavedStateMain.mSavedCursorCol = 0
        mSavedStateMain.mSavedEffect = 0
        mSavedStateMain.mSavedDecFlags = 0
        mSavedStateAlt.mSavedCursorRow = 0
        mSavedStateAlt.mSavedCursorCol = 0
        mSavedStateAlt.mSavedEffect = 0
        mSavedStateAlt.mSavedDecFlags = 0
        mCurrentDecSetFlags = 0
        // Initial wrap-around is not accurate but makes terminal more useful, especially on a small screen:
        setDecsetinternalBit(DECSET_BIT_AUTOWRAP, true)
        setDecsetinternalBit(DECSET_BIT_CURSOR_ENABLED, true)
        mSavedDecSetFlags = mCurrentDecSetFlags
        mSavedStateMain.mSavedDecFlags = mCurrentDecSetFlags
        mSavedStateAlt.mSavedDecFlags = mCurrentDecSetFlags

        // XXX: Should we set terminal driver back to IUTF8 with termios?
        mUtf8Index = 0
        mUtf8ToFollow = 0

        mColors.reset()
        mSession.onColorsChanged()

        clearTerminalControlArgs()
        clearOscTypeVariables()
        mITermImage = null
    }

    fun getSelectedText(x1: Int, y1: Int, x2: Int, y2: Int): String {
        return mScreen.getSelectedText(x1, y1, x2, y2)
    }

    /** Get the terminal session's title (null if not set). */
    fun getTitle(): String? {
        return mTitle
    }

    /** Change the terminal session's title. */
    private fun setTitle(newTitle: String?) {
        val oldTitle = mTitle
        mTitle = newTitle
        if (oldTitle != newTitle) {
            mSession.titleChanged(oldTitle, newTitle)
        }
    }

    /** If DECSET 2004 is set, prefix paste with "\u001B[200~" and suffix with "\u001B[201~". */
    fun paste(text: String) {
        // First: Always remove escape key and C1 control characters [0x80,0x9F]:
        var text = text.replace("(\\u001B|[\\u0080-\\u009F])".toRegex(), "")
        // Second: Replace all newlines (\n) or CRLF (\r\n) with carriage returns (\r).
        text = text.replace("\r?\n".toRegex(), "\r")

        // Then: Implement bracketed paste mode if enabled:
        val bracketed = isDecsetInternalBitSet(DECSET_BIT_BRACKETED_PASTE_MODE)
        if (bracketed) mSession.write("\u001B[200~")
        mSession.write(text)
        if (bracketed) mSession.write("\u001B[201~")
    }

    /** http://www.vt100.net/docs/vt510-rm/DECSC */
    class SavedScreenState {
        /** Saved state of the cursor position, Used to implement the save/restore cursor position escape sequences. */
        var mSavedCursorRow = 0
        var mSavedCursorCol = 0
        var mSavedEffect = 0
        var mSavedForeColor = 0
        var mSavedBackColor = 0
        var mSavedDecFlags = 0
        var mUseLineDrawingG0 = false
        var mUseLineDrawingG1 = false
        var mUseLineDrawingUsesG0 = true
    }

    override fun toString(): String {
        return "TerminalEmulator[size=" + mScreen.mColumns + "x" + mScreen.mScreenRows + ", margins={" + mTopMargin + "," + mRightMargin + "," + mBottomMargin + "," + mLeftMargin + "}]"
    }
}
