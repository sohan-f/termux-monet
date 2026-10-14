package com.termux.view.textselection

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.text.TextUtils
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.annotation.Nullable
import com.termux.terminal.TerminalBuffer
import com.termux.terminal.WcWidth
import com.termux.view.R
import com.termux.view.TerminalView

class TextSelectionCursorController(private val terminalView: TerminalView) : CursorController {

    private val mStartHandle: TextSelectionHandleView
    private val mEndHandle: TextSelectionHandleView

    private var mStoredSelectedText: String? = null

    private var mIsSelectingText = false

    private var mShowStartTime = System.currentTimeMillis()

    private val mHandleHeight: Int

    private var mSelX1 = -1
    private var mSelX2 = -1
    private var mSelY1 = -1
    private var mSelY2 = -1

    private var mActionMode: ActionMode? = null

    @JvmField
    val ACTION_COPY = 1

    @JvmField
    val ACTION_PASTE = 2

    @JvmField
    val ACTION_MORE = 3

    init {
        mStartHandle = TextSelectionHandleView(terminalView, this, TextSelectionHandleView.LEFT)
        mEndHandle = TextSelectionHandleView(terminalView, this, TextSelectionHandleView.RIGHT)
        mHandleHeight = Math.max(mStartHandle.getHandleHeight(), mEndHandle.getHandleHeight())
    }

    override fun show(event: MotionEvent?) {
        setInitialTextSelectionPosition(event)
        mStartHandle.positionAtCursor(mSelX1, mSelY1, true)
        mEndHandle.positionAtCursor(mSelX2 + 1, mSelY2, true)
        setActionModeCallBacks()
        mShowStartTime = System.currentTimeMillis()
        mIsSelectingText = true
    }

    override fun hide(): Boolean {
        if (!isActive())
            return false
        // prevent hide calls right after a show call, like long pressing the down key
        // 300ms seems long enough that it wouldn't cause hide problems if action button
        // is quickly clicked after the show, otherwise decrease it
        if (System.currentTimeMillis() - mShowStartTime < 300L) {
            return false
        }
        mStartHandle.hide()
        mEndHandle.hide()
        mActionMode?.finish()
        mSelX1 = -1
        mSelY1 = -1
        mSelX2 = -1
        mSelY2 = -1
        mIsSelectingText = false
        return true
    }

    override fun render() {
        if (!isActive())
            return
        mStartHandle.positionAtCursor(mSelX1, mSelY1, false)
        mEndHandle.positionAtCursor(mSelX2 + 1, mSelY2, false)
        mActionMode?.invalidate()
    }

    fun setInitialTextSelectionPosition(event: MotionEvent?) {
        val columnAndRow = terminalView.getColumnAndRow(event, true)
        mSelX1 = columnAndRow[0]
        mSelX2 = mSelX1
        mSelY1 = columnAndRow[1]
        mSelY2 = mSelY1
        val screen = terminalView.mEmulator!!.screen
        if (" " != screen.getSelectedText(mSelX1, mSelY1, mSelX1, mSelY1)) {
            // Selecting something other than whitespace. Expand to word.
            while (mSelX1 > 0 && "" != screen.getSelectedText(mSelX1 - 1, mSelY1, mSelX1 - 1, mSelY1)) {
                mSelX1--
            }
            while (mSelX2 < terminalView.mEmulator!!.mColumns - 1 && "" != screen.getSelectedText(mSelX2 + 1, mSelY1, mSelX2 + 1, mSelY1)) {
                mSelX2++
            }
        }
    }

    fun setActionModeCallBacks() {
        val callback = object : ActionMode.Callback {

            override fun onCreateActionMode(mode: ActionMode, actionMode: Menu): Boolean {
                val show = MenuItem.SHOW_AS_ACTION_IF_ROOM or MenuItem.SHOW_AS_ACTION_WITH_TEXT
                val clipboard = terminalView.getContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                actionMode.add(Menu.NONE, ACTION_COPY, Menu.NONE, R.string.copy_text).setShowAsAction(show)
                actionMode.add(Menu.NONE, ACTION_PASTE, Menu.NONE, R.string.paste_text).setEnabled(clipboard != null && clipboard.hasPrimaryClip()).setShowAsAction(show)
                actionMode.add(Menu.NONE, ACTION_MORE, Menu.NONE, R.string.text_selection_more)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                return false
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                if (!isActive()) {
                    // Fix issue where the dialog is pressed while being dismissed.
                    return true
                }
                when (item.itemId) {
                    ACTION_COPY -> {
                        val selectedText = getSelectedText()
                        terminalView.mTermSession!!.onCopyTextToClipboard(selectedText)
                        terminalView.stopTextSelectionMode()
                    }
                    ACTION_PASTE -> {
                        terminalView.stopTextSelectionMode()
                        terminalView.mTermSession!!.onPasteTextFromClipboard()
                    }
                    ACTION_MORE -> {
                        // We first store the selected text in case TerminalViewClient needs the
                        // selected text before MORE button was pressed since we are going to
                        // stop selection mode
                        mStoredSelectedText = getSelectedText()
                        // The text selection needs to be stopped before showing context menu,
                        // otherwise handles will show above popup
                        terminalView.stopTextSelectionMode()
                        terminalView.showContextMenu()
                    }
                }
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode) {
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mActionMode = terminalView.startActionMode(callback)
            return
        }
        //noinspection NewApi
        mActionMode = terminalView.startActionMode(object : ActionMode.Callback2() {

            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                return callback.onCreateActionMode(mode, menu)
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                return false
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                return callback.onActionItemClicked(mode, item)
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                // Ignore.
            }

            override fun onGetContentRect(mode: ActionMode, view: View, outRect: Rect) {
                val x1 = Math.round(mSelX1.toFloat() * terminalView.mRenderer!!.getFontWidth())
                val x2 = Math.round(mSelX2.toFloat() * terminalView.mRenderer!!.getFontWidth())
                val y1 = Math.round(((mSelY1 - terminalView.getTopRow()) * terminalView.mRenderer!!.getFontLineSpacing()).toFloat())
                val y2 = Math.round(((mSelY2 + 1 - terminalView.getTopRow()) * terminalView.mRenderer!!.getFontLineSpacing()).toFloat())
                var xx1 = x1
                var xx2 = x2
                if (xx1 > xx2) {
                    val tmp = xx1
                    xx1 = xx2
                    xx2 = tmp
                }
                val terminalBottom = terminalView.getBottom()
                var top = y1 + terminalView.mRenderer!!.getFontLineSpacingAndAscent()
                var bottom = y2 + mHandleHeight
                if (top > terminalBottom)
                    top = terminalBottom
                if (bottom > terminalBottom)
                    bottom = terminalBottom
                outRect.set(xx1, top, xx2, bottom)
            }
        }, ActionMode.TYPE_FLOATING)
    }

    override fun updatePosition(handle: TextSelectionHandleView?, x: Int, y: Int) {
        val screen = terminalView.mEmulator!!.screen
        val scrollRows = screen.getActiveRows() - terminalView.mEmulator!!.mRows
        if (handle == mStartHandle) {
            mSelX1 = terminalView.getCursorX(x.toFloat())
            mSelY1 = terminalView.getCursorY(y.toFloat())
            if (mSelX1 < 0) {
                mSelX1 = 0
            }
            if (mSelY1 < -scrollRows) {
                mSelY1 = -scrollRows
            } else if (mSelY1 > terminalView.mEmulator!!.mRows - 1) {
                mSelY1 = terminalView.mEmulator!!.mRows - 1
            }
            if (mSelY1 > mSelY2) {
                mSelY1 = mSelY2
            }
            if (mSelY1 == mSelY2 && mSelX1 > mSelX2) {
                mSelX1 = mSelX2
            }
            if (!terminalView.mEmulator!!.isAlternateBufferActive()) {
                var topRow = terminalView.getTopRow()
                if (mSelY1 <= topRow) {
                    topRow--
                    if (topRow < -scrollRows) {
                        topRow = -scrollRows
                    }
                } else if (mSelY1 >= topRow + terminalView.mEmulator!!.mRows) {
                    topRow++
                    if (topRow > 0) {
                        topRow = 0
                    }
                }
                terminalView.setTopRow(topRow)
            }
            mSelX1 = getValidCurX(screen, mSelY1, mSelX1)
        } else {
            mSelX2 = terminalView.getCursorX(x.toFloat())
            mSelY2 = terminalView.getCursorY(y.toFloat())
            if (mSelX2 < 0) {
                mSelX2 = 0
            }
            if (mSelY2 < -scrollRows) {
                mSelY2 = -scrollRows
            } else if (mSelY2 > terminalView.mEmulator!!.mRows - 1) {
                mSelY2 = terminalView.mEmulator!!.mRows - 1
            }
            if (mSelY1 > mSelY2) {
                mSelY2 = mSelY1
            }
            if (mSelY1 == mSelY2 && mSelX1 > mSelX2) {
                mSelX2 = mSelX1
            }
            if (!terminalView.mEmulator!!.isAlternateBufferActive()) {
                var topRow = terminalView.getTopRow()
                if (mSelY2 <= topRow) {
                    topRow--
                    if (topRow < -scrollRows) {
                        topRow = -scrollRows
                    }
                } else if (mSelY2 >= topRow + terminalView.mEmulator!!.mRows) {
                    topRow++
                    if (topRow > 0) {
                        topRow = 0
                    }
                }
                terminalView.setTopRow(topRow)
            }
            mSelX2 = getValidCurX(screen, mSelY2, mSelX2)
        }
        terminalView.invalidate()
    }

    private fun getValidCurX(screen: TerminalBuffer, cy: Int, cx: Int): Int {
        val line = screen.getSelectedText(0, cy, cx, cy)
        if (!TextUtils.isEmpty(line)) {
            var col = 0
            var i = 0
            val len = line.length
            while (i < len) {
                val ch1 = line[i]
                if (ch1.code == 0) {
                    break
                }
                val wc: Int
                if (ch1.isHighSurrogate() && i + 1 < len) {
                    i++
                    val ch2 = line[i]
                    wc = WcWidth.width(Character.toCodePoint(ch1, ch2))
                } else {
                    wc = WcWidth.width(ch1.code)
                }
                val cend = col + wc
                if (cx > col && cx < cend) {
                    return cend
                }
                if (cend == col) {
                    return col
                }
                col = cend
                i++
            }
        }
        return cx
    }

    fun decrementYTextSelectionCursors(decrement: Int) {
        mSelY1 -= decrement
        mSelY2 -= decrement
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return false
    }

    override fun onTouchModeChanged(isInTouchMode: Boolean) {
        if (!isInTouchMode) {
            terminalView.stopTextSelectionMode()
        }
    }

    override fun onDetached() {
    }

    override fun isActive(): Boolean {
        return mIsSelectingText
    }

    fun getSelectors(sel: IntArray?) {
        if (sel == null || sel.size != 4) {
            return
        }
        sel[0] = mSelY1
        sel[1] = mSelY2
        sel[2] = mSelX1
        sel[3] = mSelX2
    }

    /**
     * Get the currently selected text.
     */
    fun getSelectedText(): String? {
        return terminalView.mEmulator!!.getSelectedText(mSelX1, mSelY1, mSelX2, mSelY2)
    }

    /**
     * Get the selected text stored before "MORE" button was pressed on the context menu.
     */
    @Nullable
    fun getStoredSelectedText(): String? {
        return mStoredSelectedText
    }

    /**
     * Unset the selected text stored before "MORE" button was pressed on the context menu.
     */
    fun unsetStoredSelectedText() {
        mStoredSelectedText = null
    }

    fun getActionMode(): ActionMode? {
        return mActionMode
    }

    /**
     * @return true if this controller is currently used to move the start selection.
     */
    fun isSelectionStartDragged(): Boolean {
        return mStartHandle.isDragging()
    }

    /**
     * @return true if this controller is currently used to move the end selection.
     */
    fun isSelectionEndDragged(): Boolean {
        return mEndHandle.isDragging()
    }
}
