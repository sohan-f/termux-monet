package com.termux.terminal;

import junit.framework.TestCase;

public class TerminalColorSchemeTest extends TestCase {

    public void testUpdateWithMonetColorsAppliesAnsiAndDefaults() {
        TerminalColorScheme scheme = new TerminalColorScheme();
        int[] ansi = new int[16];
        for (int i = 0; i < 16; i++) ansi[i] = 0xFF000000 | (i + 1);
        int foreground = 0xFF111111;
        int background = 0xFF222222;
        int cursor = 0xFF333333;

        scheme.updateWithMonetColors(ansi, foreground, background, cursor);

        for (int i = 0; i < 16; i++) assertEquals(ansi[i], scheme.mDefaultColors[i]);
        assertEquals(foreground, scheme.mDefaultColors[TextStyle.COLOR_INDEX_FOREGROUND]);
        assertEquals(background, scheme.mDefaultColors[TextStyle.COLOR_INDEX_BACKGROUND]);
        assertEquals(cursor, scheme.mDefaultColors[TextStyle.COLOR_INDEX_CURSOR]);
    }

    public void testUpdateWithMonetColorsKeepsExtendedPalette() {
        TerminalColorScheme scheme = new TerminalColorScheme();
        int[] before = scheme.mDefaultColors.clone();
        int[] ansi = new int[16];
        for (int i = 0; i < 16; i++) ansi[i] = 0xFF0000FF;

        scheme.updateWithMonetColors(ansi, 0xFF000000, 0xFFFFFFFF, 0xFFFFFFFF);

        // Indexes 16-255 (color cube, grayscale ramp) keep stock xterm values.
        for (int i = 16; i < TextStyle.COLOR_INDEX_FOREGROUND; i++) assertEquals(before[i], scheme.mDefaultColors[i]);
    }

    public void testUpdateWithMonetColorsRejectsBadInput() {
        TerminalColorScheme scheme = new TerminalColorScheme();
        try {
            scheme.updateWithMonetColors(new int[15], 0, 0, 0);
            fail("expected IllegalArgumentException for 15 colors");
        } catch (IllegalArgumentException expected) {
        }
        try {
            scheme.updateWithMonetColors(null, 0, 0, 0);
            fail("expected IllegalArgumentException for null");
        } catch (IllegalArgumentException expected) {
        }
    }
}
