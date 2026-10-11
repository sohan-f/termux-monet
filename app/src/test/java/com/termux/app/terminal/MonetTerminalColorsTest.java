package com.termux.app.terminal;

import com.google.android.material.color.utilities.Hct;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class MonetTerminalColorsTest {

    private static final int BLACK = 0xFF000000;
    private static final int WHITE = 0xFFFFFFFF;

    /** Vivid blue seed (wallpaper-derived primary stand-in). */
    private static final int BLUE_SEED = 0xFF5B6DF5;
    /** Green seed, far from red — used to prove red stays red. */
    private static final int GREEN_SEED = 0xFF4CAF50;
    /** Achromatic seed (monochrome wallpaper). */
    private static final int GRAY_SEED = 0xFF808080;

    /** Classic xterm dim hues are the semantic anchors; keep roles recognizable. */
    private static final int[] XTERM_DIM = {
        0xFFCD0000, 0xFF00CD00, 0xFFCDCD00, 0xFF6495ED, 0xFFCD00CD, 0xFF00CDCD};

    @Test
    public void testContrastRatioSanity() {
        Assert.assertEquals(1.0, MonetTerminalColors.contrastRatio(BLACK, BLACK), 0.001);
        Assert.assertEquals(21.0, MonetTerminalColors.contrastRatio(BLACK, WHITE), 0.5);
        Assert.assertEquals(MonetTerminalColors.contrastRatio(0xFFCD0000, BLACK),
            MonetTerminalColors.contrastRatio(BLACK, 0xFFCD0000), 0.0);
    }

    @Test
    public void testHueDifferenceSanity() {
        Assert.assertEquals(0.0, MonetTerminalColors.hueDifference(10, 370), 0.001);
        Assert.assertEquals(20.0, MonetTerminalColors.hueDifference(350, 10), 0.001);
        Assert.assertEquals(180.0, MonetTerminalColors.hueDifference(0, 180), 0.001);
    }

    @Test
    public void testNightChromaticsAreVividAndReadable() {
        MonetTerminalColors.Scheme scheme =
            MonetTerminalColors.deriveScheme(saneTones(), true, BLUE_SEED);
        for (int role : new int[] {1, 2, 3, 4, 5, 6, 9, 10, 11, 12, 13, 14}) {
            int c = scheme.ansi[role];
            Assert.assertTrue("ansi[" + role + "] contrast "
                + MonetTerminalColors.contrastRatio(c, scheme.background),
                MonetTerminalColors.contrastRatio(c, scheme.background)
                    >= MonetTerminalColors.MIN_CONTRAST);
            Assert.assertTrue("ansi[" + role + "] chroma " + Hct.fromInt(c).getChroma(),
                Hct.fromInt(c).getChroma() >= 30.0);
        }
        Assert.assertTrue(MonetTerminalColors.contrastRatio(scheme.cursor, scheme.background)
            >= MonetTerminalColors.MIN_CONTRAST);
    }

    @Test
    public void testLightChromaticsAreVividAndReadable() {
        MonetTerminalColors.Scheme scheme =
            MonetTerminalColors.deriveScheme(saneTones(), false, BLUE_SEED);
        for (int role : new int[] {1, 2, 3, 4, 5, 6, 9, 10, 11, 12, 13, 14}) {
            int c = scheme.ansi[role];
            Assert.assertTrue("ansi[" + role + "] contrast "
                + MonetTerminalColors.contrastRatio(c, scheme.background),
                MonetTerminalColors.contrastRatio(c, scheme.background)
                    >= MonetTerminalColors.MIN_CONTRAST);
            Assert.assertTrue("ansi[" + role + "] chroma " + Hct.fromInt(c).getChroma(),
                Hct.fromInt(c).getChroma() >= 30.0);
        }
        Assert.assertTrue(MonetTerminalColors.contrastRatio(scheme.cursor, scheme.background)
            >= MonetTerminalColors.MIN_CONTRAST);
    }

    @Test
    public void testHuesKeepSemanticIdentity() {
        MonetTerminalColors.Scheme scheme =
            MonetTerminalColors.deriveScheme(saneTones(), true, BLUE_SEED);
        // M3 harmonize shifts hue at most ~15 degrees, so every role must still read
        // as its classic color (red reads red, green reads green, ...).
        for (int role = 1; role <= 6; role++) {
            double baseHue = Hct.fromInt(XTERM_DIM[role - 1]).getHue();
            double dimHue = Hct.fromInt(scheme.ansi[role]).getHue();
            double brightHue = Hct.fromInt(scheme.ansi[role + 8]).getHue();
            Assert.assertTrue("role " + role + " dim drifted "
                + MonetTerminalColors.hueDifference(dimHue, baseHue),
                MonetTerminalColors.hueDifference(dimHue, baseHue) < 30.0);
            Assert.assertTrue("role " + role + " bright drifted "
                + MonetTerminalColors.hueDifference(brightHue, baseHue),
                MonetTerminalColors.hueDifference(brightHue, baseHue) < 30.0);
        }
    }

    @Test
    public void testRedAnchoredToErrorHueNotSeed() {
        MonetTerminalColors.ResolvedTones tones = saneTones();
        MonetTerminalColors.Scheme scheme =
            MonetTerminalColors.deriveScheme(tones, true, GREEN_SEED);
        double redHue = Hct.fromInt(scheme.ansi[1]).getHue();
        double errorHue = Hct.fromInt(tones.accents[3][0]).getHue();
        double seedHue = Hct.fromInt(GREEN_SEED).getHue();
        // Even on a green wallpaper, red must stay near the error hue, not the seed.
        Assert.assertTrue(MonetTerminalColors.hueDifference(redHue, errorHue) < 30.0);
        Assert.assertTrue(MonetTerminalColors.hueDifference(redHue, errorHue)
            < MonetTerminalColors.hueDifference(redHue, seedHue));
    }

    @Test
    public void testMonochromeSeedKeepsPureHues() {
        MonetTerminalColors.Scheme scheme =
            MonetTerminalColors.deriveScheme(saneTones(), true, GRAY_SEED);
        // Achromatic seed: harmonization is skipped, hues stay at the semantic bases.
        for (int role = 1; role <= 6; role++) {
            double baseHue = Hct.fromInt(XTERM_DIM[role - 1]).getHue();
            double dimHue = Hct.fromInt(scheme.ansi[role]).getHue();
            Assert.assertTrue("role " + role + " drifted "
                + MonetTerminalColors.hueDifference(dimHue, baseHue),
                MonetTerminalColors.hueDifference(dimHue, baseHue) < 8.0);
        }
        // Monochrome cursor falls back to plain white/black (a random vivid hue would
        // look arbitrary on a gray wallpaper).
        Assert.assertEquals(WHITE, scheme.cursor);
        MonetTerminalColors.Scheme day =
            MonetTerminalColors.deriveScheme(saneTones(), false, GRAY_SEED);
        Assert.assertEquals(BLACK, day.cursor);
    }

    @Test
    public void testNoDuplicateAnsiColors() {
        for (boolean night : new boolean[] {true, false}) {
            MonetTerminalColors.Scheme scheme =
                MonetTerminalColors.deriveScheme(saneTones(), night, BLUE_SEED);
            Set<Integer> seen = new HashSet<>();
            for (int c : scheme.ansi)
                Assert.assertTrue("duplicate ansi color " + Integer.toHexString(c),
                    seen.add(c));
            for (int role = 1; role <= 6; role++)
                Assert.assertNotEquals(scheme.ansi[role], scheme.ansi[role + 8]);
        }
    }

    @Test
    public void testChromaticRolesAreHueDistinct() {
        MonetTerminalColors.Scheme scheme =
            MonetTerminalColors.deriveScheme(saneTones(), true, BLUE_SEED);
        // Neighboring roles (e.g. yellow/green) must not collapse onto one hue even
        // after the wallpaper tint is applied.
        for (int a = 1; a <= 6; a++) {
            for (int b = a + 1; b <= 6; b++) {
                double ha = Hct.fromInt(scheme.ansi[a]).getHue();
                double hb = Hct.fromInt(scheme.ansi[b]).getHue();
                Assert.assertTrue("roles " + a + "/" + b + " collapsed",
                    MonetTerminalColors.hueDifference(ha, hb)
                        >= MonetTerminalColors.MIN_HUE_SEPARATION);
            }
        }
    }

    @Test
    public void testCollapsedPaletteDegradesWithoutCrash() {
        MonetTerminalColors.ResolvedTones tones = new MonetTerminalColors.ResolvedTones();
        // Monochrome wallpaper: every accent tone is the same mid gray.
        for (int p = 0; p < 3; p++)
            for (int t = 0; t < 9; t++) tones.accents[p][t] = 0xFF808080;
        tones.neutral[0] = WHITE;
        tones.neutral[1] = 0xFFEEEEEE;
        tones.neutral[2] = 0xFFCCCCCC;
        tones.neutral[3] = 0xFF888888;
        tones.neutral[4] = 0xFF333333;
        tones.neutral[5] = BLACK;
        tones.neutral2[0] = 0xFFBBBBBB;
        tones.neutral2[1] = 0xFF999999;
        tones.accents[3][0] = 0xFF808080;
        tones.accents[3][1] = 0xFF808080;
        tones.accents[3][2] = 0xFF808080;
        for (boolean night : new boolean[] {true, false}) {
            MonetTerminalColors.Scheme scheme =
                MonetTerminalColors.deriveScheme(tones, night, GRAY_SEED);
            // Chromatic synthesis needs no accent hues, so readability holds anyway.
            for (int role : new int[] {1, 2, 3, 4, 5, 6, 9, 10, 11, 12, 13, 14}) {
                Assert.assertTrue("ansi[" + role + "] unreadable",
                    MonetTerminalColors.contrastRatio(scheme.ansi[role], scheme.background)
                        >= MonetTerminalColors.MIN_CONTRAST);
            }
        }
    }

    @Test
    public void testLightModeFixedRoles() {
        MonetTerminalColors.ResolvedTones tones = saneTones();
        MonetTerminalColors.Scheme scheme =
            MonetTerminalColors.deriveScheme(tones, false, BLUE_SEED);
        Assert.assertEquals(tones.neutral[0], scheme.background);
        Assert.assertEquals(tones.neutral[5], scheme.foreground);
        Assert.assertEquals(tones.neutral[5], scheme.ansi[0]);
    }

    /**
     * Neutral surfaces plus red error roles. Accent slots are irrelevant to the new
     * synthesizer (hues come from semantic bases, tint from the seed), so only the
     * neutrals and error roles need sane values here.
     */
    private static MonetTerminalColors.ResolvedTones saneTones() {
        MonetTerminalColors.ResolvedTones tones = new MonetTerminalColors.ResolvedTones();
        for (int p = 0; p < 3; p++)
            for (int t = 0; t < 9; t++) tones.accents[p][t] = 0xFF808080;
        tones.neutral[0] = WHITE;
        tones.neutral[1] = 0xFFEEEEEE;
        tones.neutral[2] = 0xFFCCCCCC;
        tones.neutral[3] = 0xFF888888;
        tones.neutral[4] = 0xFF333333;
        tones.neutral[5] = BLACK;
        tones.neutral2[0] = 0xFFBBBBBB;
        tones.neutral2[1] = 0xFF999999;
        tones.accents[3][0] = 0xFFBA1A1A; // M3 light error (red hue anchor)
        tones.accents[3][1] = 0xFF93000A;
        tones.accents[3][2] = 0xFF690005;
        return tones;
    }
}
