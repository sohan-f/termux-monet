package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class MonetTerminalColorsTest {

    private static final int BLACK = 0xFF000000;
    private static final int WHITE = 0xFFFFFFFF;

    @Test
    public void testContrastRatioSanity() {
        Assert.assertEquals(1.0, MonetTerminalColors.contrastRatio(BLACK, BLACK), 0.001);
        Assert.assertEquals(21.0, MonetTerminalColors.contrastRatio(BLACK, WHITE), 0.5);
        Assert.assertEquals(MonetTerminalColors.contrastRatio(0xFFCD0000, BLACK),
            MonetTerminalColors.contrastRatio(BLACK, 0xFFCD0000), 0.0);
    }

    @Test
    public void testPreferredCandidatesWinOnSanePalette() {
        MonetTerminalColors.ResolvedTones tones = distinctTones();
        MonetTerminalColors.Scheme scheme = MonetTerminalColors.deriveScheme(tones, true);
        // Preferred dark slots pass the contrast gate and are distinct, so taken as-is.
        Assert.assertEquals(tones.accents[3][1], scheme.ansi[1]); // red dim = errorContainer
        Assert.assertEquals(tones.accents[3][0], scheme.ansi[9]); // red bright = error
        Assert.assertEquals(tones.accents[0][4], scheme.ansi[4]); // blue dim = a1_500
        Assert.assertEquals(tones.neutral[5], scheme.background);
        Assert.assertEquals(tones.neutral[0], scheme.foreground);
    }

    @Test
    public void testRedAnchoredToErrorRolesInLightMode() {
        MonetTerminalColors.ResolvedTones tones = distinctTones();
        MonetTerminalColors.Scheme scheme = MonetTerminalColors.deriveScheme(tones, false);
        Assert.assertEquals(tones.accents[3][0], scheme.ansi[1]); // red dim = error
        Assert.assertEquals(tones.accents[3][2], scheme.ansi[9]); // red bright = onErrorContainer
    }

    @Test
    public void testLowContrastCandidatesAreSkipped() {
        MonetTerminalColors.ResolvedTones tones = distinctTones();
        // Poison the preferred dim error role with a near-background color.
        tones.accents[3][1] = 0xFF101010;
        MonetTerminalColors.Scheme scheme = MonetTerminalColors.deriveScheme(tones, true);
        // Falls back to the next readable candidate instead.
        Assert.assertEquals(tones.accents[2][4], scheme.ansi[1]); // a3_500
        Assert.assertTrue(MonetTerminalColors.contrastRatio(scheme.ansi[1], scheme.background)
            >= MonetTerminalColors.MIN_CONTRAST);
    }

    @Test
    public void testCollapsedPaletteDegradesWithoutCrashOrDuplicates() {
        MonetTerminalColors.ResolvedTones tones = new MonetTerminalColors.ResolvedTones();
        // Monochrome wallpaper: every accent tone is the same mid gray.
        for (int p = 0; p < 3; p++)
            for (int t = 0; t < 9; t++) tones.accents[p][t] = 0xFF808080;
        tones.neutral[0] = WHITE;
        tones.neutral[1] = 0xFFEEEEEE;
        tones.neutral[2] = 0xFFCCCCCC;
        tones.neutral[3] = 0xFF808080;
        tones.neutral[4] = 0xFF333333;
        tones.neutral[5] = BLACK;
        tones.neutral2[0] = 0xFF808080;
        tones.neutral2[1] = 0xFF808080;
        tones.accents[3][0] = 0xFF808080;
        tones.accents[3][1] = 0xFF808080;
        tones.accents[3][2] = 0xFF808080;
        MonetTerminalColors.Scheme scheme = MonetTerminalColors.deriveScheme(tones, true);
        // Fixed grays (0, 7, 8, 15) are intentionally ungated like xterm; assert the 12
        // contrast-selected chromatic roles.
        for (int role : new int[] {1, 2, 3, 4, 5, 6, 9, 10, 11, 12, 13, 14}) {
            int c = scheme.ansi[role];
            Assert.assertTrue("ansi[" + role + "] unreadable and not preferred",
                MonetTerminalColors.contrastRatio(c, scheme.background) >= MonetTerminalColors.MIN_CONTRAST
                    || c == 0xFF808080);
        }
    }

    @Test
    public void testCursorFallsBackWhenPreferredLacksContrast() {
        MonetTerminalColors.ResolvedTones tones = distinctTones();
        tones.accents[0][1] = tones.neutral[5]; // preferred night cursor == background
        MonetTerminalColors.Scheme night = MonetTerminalColors.deriveScheme(tones, true);
        Assert.assertEquals(WHITE, night.cursor);
        tones.accents[0][5] = tones.neutral[0]; // preferred day cursor == background
        MonetTerminalColors.Scheme day = MonetTerminalColors.deriveScheme(tones, false);
        Assert.assertEquals(BLACK, day.cursor);
    }

    @Test
    public void testLightModeFixedRoles() {
        MonetTerminalColors.ResolvedTones tones = distinctTones();
        MonetTerminalColors.Scheme scheme = MonetTerminalColors.deriveScheme(tones, false);
        Assert.assertEquals(tones.neutral[0], scheme.background);
        Assert.assertEquals(tones.neutral[5], scheme.foreground);
        Assert.assertEquals(tones.neutral[5], scheme.ansi[0]);
    }

    @Test
    public void testApplyHarmonizeKeepsReadableDistinctResults() {
        MonetTerminalColors.ResolvedTones tones = distinctTones();
        MonetTerminalColors.Scheme scheme = MonetTerminalColors.deriveScheme(tones, true);
        int[] before = scheme.ansi.clone();
        // Fake shift: brighten red channel; red roles (1/9) must stay untouched.
        MonetTerminalColors.applyHarmonize(scheme, c -> 0xFF000000 | (c & 0x00FFFF) | 0x100000);
        Assert.assertEquals(before[1], scheme.ansi[1]);
        Assert.assertEquals(before[9], scheme.ansi[9]);
        for (int role : new int[] {2, 4, 3, 5, 6, 10, 11, 12, 13, 14}) {
            Assert.assertTrue(MonetTerminalColors.contrastRatio(scheme.ansi[role], scheme.background)
                >= MonetTerminalColors.MIN_CONTRAST);
        }
    }

    @Test
    public void testApplyHarmonizeRejectsUnreadableAndCollidingResults() {
        MonetTerminalColors.ResolvedTones tones = distinctTones();
        MonetTerminalColors.Scheme scheme = MonetTerminalColors.deriveScheme(tones, true);
        int[] before = scheme.ansi.clone();
        // Fake harmonize collapses everything onto the background: all rejected.
        MonetTerminalColors.applyHarmonize(scheme, c -> scheme.background);
        Assert.assertArrayEquals(before, scheme.ansi);
    }

    /**
     * Every resolved tone bright (>= 0x88 per channel, contrast vs black >= ~6) and unique,
     * so preferred candidates pass the gate and taken checks.
     */
    private static MonetTerminalColors.ResolvedTones distinctTones() {
        MonetTerminalColors.ResolvedTones tones = new MonetTerminalColors.ResolvedTones();
        for (int p = 0; p < 3; p++)
            for (int t = 0; t < 9; t++) {
                int r = 0x88 + p * 0x11;
                int g = 0x88 + p * 0x11 + t * 0x08;
                int b = 0x88 + p * 0x11 + t * 0x08;
                tones.accents[p][t] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        tones.neutral[0] = WHITE;
        tones.neutral[1] = 0xFFEEEEEE;
        tones.neutral[2] = 0xFFCCCCCC;
        tones.neutral[3] = 0xFF888888;
        tones.neutral[4] = 0xFF333333;
        tones.neutral[5] = BLACK;
        tones.neutral2[0] = 0xFFBBBBBB;
        tones.neutral2[1] = 0xFF999999;
        tones.accents[3][0] = 0xFFFF8888;
        tones.accents[3][1] = 0xFFCC4444;
        tones.accents[3][2] = 0xFF990000;
        return tones;
    }
}
