package com.termux.app.terminal;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.color.utilities.Blend;
import com.google.android.material.color.utilities.Hct;
import com.termux.shared.logger.Logger;
import com.termux.terminal.TerminalColors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Derives the terminal 16-color ANSI palette from the Material You (Monet) dynamic
 * color system on Android 12+ ({@code android:color/system_neutral1_*},
 * {@code system_neutral2_*}) combined with the live theme seed ({@code colorPrimary})
 * and error roles.
 *
 * <p>
 * Only used when the user has no {@code ~/.termux/colors.properties} file; an existing
 * colors file always takes precedence and the stock xterm scheme is kept below API 31.
 * Indexes 16-255 keep the stock xterm values.
 * </p>
 *
 * <h2>Why not the raw {@code system_accent} tones?</h2>
 * <p>
 * The framework accent slots come from the default {@code TonalSpot} scheme, whose
 * chroma is deliberately low (primary ~36, secondary ~16, tertiary ~24) because phone
 * UI surfaces must stay calm. Terminal ANSI colors need the opposite: vivid,
 * instantly distinguishable hues (popular schemes such as Dracula sit at chroma
 * ~40-80). Picking raw accent tones therefore always looks dark, gray and dull, and
 * three Monet families cannot cover six semantic hues anyway — e.g. "yellow" would
 * just be a lighter shade of the primary hue instead of actually yellow.
 * </p>
 *
 * <h2>Approach: vibrant HCT synthesis + M3 harmonization</h2>
 * <ol>
 *   <li><b>Vivid semantic bases.</b> Each ANSI role starts from the HCT hue of its
 *       classic xterm counterpart and is synthesized at a fixed high chroma and a
 *       fixed tone via {@link Hct#from(double, double, double)}. HCT tone maps
 *       almost 1:1 to perceived lightness independent of hue, so one tone pair per
 *       mode gives every role the same guaranteed contrast (dark: dim 65 / bright 76
 *       for ~6.9:1 and ~9.7:1; light: dim 45 / bright 35 for ~5:1 and ~7:1 — the
 *       "bright" variants on light backgrounds are darker, which is what keeps them
 *       readable).</li>
 *   <li><b>Wallpaper tint via M3 harmonize.</b> Each vivid base is passed through
 *       {@link Blend#harmonize}, the Material Color Utilities hue rotation toward the
 *       theme seed ({@code colorPrimary}). It shifts hue at most 15 degrees, so
 *       colors stay recognizable (red stays red) while picking up the wallpaper
 *       tint. Red is harmonized toward the theme error color instead, preserving
 *       its danger semantics. Harmonize only touches hue, so chroma, tone and
 *       contrast survive untouched.</li>
 *   <li><b>Contrast + distinctness gates.</b> Every chromatic role must clear
 *       {@link #MIN_CONTRAST} (4.5:1, WCAG AA for normal text — M3's 3:1 is a large
 *       text minimum and lets dull colors through), must not duplicate an already
 *       placed color, and must keep a minimum HCT hue distance from previously
 *       placed chromatics (harmonize can otherwise pull neighboring hues such as
 *       yellow/green together on some wallpapers). Violations step the tone toward
 *       the readable direction until they clear.</li>
 *   <li><b>Monochrome wallpapers.</b> When the seed is near-gray (chroma &lt; 8),
 *       its hue is meaningless, so harmonization is skipped and the pure semantic
 *       hues are used directly.</li>
 * </ol>
 *
 * <p>
 * Background, foreground and the gray roles (0/7/8/15) keep the Monet neutral
 * palettes so the terminal surface still matches the system theme; only the six
 * chromatic hues are synthesized.
 * </p>
 */
public final class MonetTerminalColors {

    private static final String LOG_TAG = "MonetTerminalColors";

    /**
     * Minimum WCAG contrast ratio of an ANSI color against the background. 4.5:1 is
     * the WCAG AA floor for normal-size text; M3's 3:1 large-text minimum is too
     * permissive here and lets dark/dull colors pass.
     */
    static final double MIN_CONTRAST = 4.5;

    // Process-lifetime cache: system colors only change on wallpaper/theme change.
    private static int sCachedWallpaperId = Integer.MIN_VALUE;
    private static boolean sCachedNight;
    private static Scheme sCachedScheme;

    // Palette ids into the palettes array passed to the selector.
    private static final int A1 = 0;
    private static final int A2 = 1;
    private static final int A3 = 2;
    /** Pseudo-palette holding the live theme roles {error, errorContainer, onErrorContainer}. */
    private static final int AERR = 3;
    private static final int E_ERR = 0;
    private static final int E_CONT = 1;
    private static final int E_ONCONT = 2;

    // Tone positions in the resolved accent arrays (tones 100-900).
    private static final int T100 = 0;
    private static final int T200 = 1;
    private static final int T300 = 2;
    private static final int T400 = 3;
    private static final int T500 = 4;
    private static final int T600 = 5;
    private static final int T700 = 6;
    private static final int T800 = 7;
    private static final int T900 = 8;

    // Positions in the resolved neutral array {50, 100, 300, 500, 800, 900}.
    private static final int N50 = 0;
    private static final int N100 = 1;
    private static final int N300 = 2;
    private static final int N500 = 3;
    private static final int N800 = 4;
    private static final int N900 = 5;

    /** ANSI role order: primaries first so red/green/blue win collisions over their partners. */
    private static final int[] ROLE_ORDER = {1, 2, 4, 3, 5, 6};

    /**
     * Classic xterm dim colors, used only as hue anchors. Their HCT hues are the
     * semantic identities (red ~27, yellow ~111, green ~142, cyan ~197, blue ~265,
     * magenta ~335); chroma and tone always come from the vivid targets below.
     */
    private static final int[] BASE_DIM_ARGB = {
        0xFFCD0000, // 1 red
        0xFF00CD00, // 2 green
        0xFFCDCD00, // 3 yellow
        0xFF6495ED, // 4 blue
        0xFFCD00CD, // 5 magenta
        0xFF00CDCD, // 6 cyan
    };

    /** HCT hues of {@link #BASE_DIM_ARGB}, computed once (Hct is pure JVM math). */
    private static final double[] BASE_HUES = baseHues();

    private static double[] baseHues() {
        double[] hues = new double[BASE_DIM_ARGB.length];
        for (int i = 0; i < BASE_DIM_ARGB.length; i++)
            hues[i] = Hct.fromInt(BASE_DIM_ARGB[i]).getHue();
        return hues;
    }

    // Vivid synthesis targets: (chroma, tone) per mode and brightness level.
    // Dark tones 65/76 clear ~6.9:1/~9.7:1; light tones 45/35 clear ~5:1/~7:1,
    // with chroma requests at or above the per-hue gamut ceiling so each hue
    // renders as vivid as physically possible at its tone (cf. Dracula 37-80).
    private static final double DIM_CHROMA_DARK = 85.0;
    private static final double DIM_TONE_DARK = 65.0;
    private static final double BRIGHT_CHROMA_DARK = 90.0;
    private static final double BRIGHT_TONE_DARK = 76.0;
    private static final double DIM_CHROMA_LIGHT = 70.0;
    private static final double DIM_TONE_LIGHT = 45.0;
    private static final double BRIGHT_CHROMA_LIGHT = 75.0;
    private static final double BRIGHT_TONE_LIGHT = 35.0;

    /** Seed chroma below this is treated as monochrome: harmonization is skipped. */
    static final double MONO_CHROMA_THRESHOLD = 8.0;
    /** Minimum HCT hue separation between two placed chromatic roles. */
    static final double MIN_HUE_SEPARATION = 12.0;
    /** Hue rivals closer than this in tone are resolved by tone-stepping instead. */
    static final double MIN_TONE_SEPARATION = 8.0;

    private MonetTerminalColors() {}

    /** Resolved Monet tones feeding the pure {@link #deriveScheme} selector. */
    static final class ResolvedTones {
        /** accent1/2/3 palettes, each holding tones {100..900}, plus error roles at index 3. */
        final int[][] accents = new int[4][];
        /** neutral1 palette holding tones {50, 100, 300, 500, 800, 900}. */
        final int[] neutral = new int[6];
        /** neutral2 seed-tinted grays holding tones {300, 500}. */
        final int[] neutral2 = new int[2];

        ResolvedTones() {
            for (int i = 0; i < 3; i++) accents[i] = new int[9];
            // Pseudo-palette with the live theme roles {error, errorContainer, onErrorContainer}.
            accents[AERR] = new int[3];
        }
    }

    /** Derived scheme: 16 ANSI colors plus default foreground, background and cursor. */
    static final class Scheme {
        final int[] ansi = new int[16];
        int foreground;
        int background;
        int cursor;
    }

    /**
     * Resolve Monet system colors and apply them to {@link TerminalColors#COLOR_SCHEME}.
     *
     * @param context Context used to resolve framework colors and night mode.
     * @return {@code true} if Monet colors were applied, {@code false} if unavailable
     *         (below API 31 or resolution failed) and the caller should fall back.
     */
    public static boolean applyMonetColors(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            return false;
        try {
            boolean night = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            int wallpaperId = currentWallpaperId(context);
            boolean useCache = wallpaperId > 0;
            if (useCache) {
                synchronized (MonetTerminalColors.class) {
                    if (sCachedScheme != null && sCachedNight == night && sCachedWallpaperId == wallpaperId) {
                        Scheme cached = sCachedScheme;
                        TerminalColors.COLOR_SCHEME.updateWithMonetColors(cached.ansi, cached.foreground,
                            cached.background, cached.cursor);
                        return true;
                    }
                }
            }
            ResolvedTones tones = new ResolvedTones();
            tones.accents[A1] = new int[] {
                context.getColor(android.R.color.system_accent1_100),
                context.getColor(android.R.color.system_accent1_200),
                context.getColor(android.R.color.system_accent1_300),
                context.getColor(android.R.color.system_accent1_400),
                context.getColor(android.R.color.system_accent1_500),
                context.getColor(android.R.color.system_accent1_600),
                context.getColor(android.R.color.system_accent1_700),
                context.getColor(android.R.color.system_accent1_800),
                context.getColor(android.R.color.system_accent1_900)};
            tones.accents[A2] = new int[] {
                context.getColor(android.R.color.system_accent2_100),
                context.getColor(android.R.color.system_accent2_200),
                context.getColor(android.R.color.system_accent2_300),
                context.getColor(android.R.color.system_accent2_400),
                context.getColor(android.R.color.system_accent2_500),
                context.getColor(android.R.color.system_accent2_600),
                context.getColor(android.R.color.system_accent2_700),
                context.getColor(android.R.color.system_accent2_800),
                context.getColor(android.R.color.system_accent2_900)};
            tones.accents[A3] = new int[] {
                context.getColor(android.R.color.system_accent3_100),
                context.getColor(android.R.color.system_accent3_200),
                context.getColor(android.R.color.system_accent3_300),
                context.getColor(android.R.color.system_accent3_400),
                context.getColor(android.R.color.system_accent3_500),
                context.getColor(android.R.color.system_accent3_600),
                context.getColor(android.R.color.system_accent3_700),
                context.getColor(android.R.color.system_accent3_800),
                context.getColor(android.R.color.system_accent3_900)};
            tones.neutral[0] = context.getColor(android.R.color.system_neutral1_50);
            tones.neutral[1] = context.getColor(android.R.color.system_neutral1_100);
            tones.neutral[2] = context.getColor(android.R.color.system_neutral1_300);
            tones.neutral[3] = context.getColor(android.R.color.system_neutral1_500);
            tones.neutral[4] = context.getColor(android.R.color.system_neutral1_800);
            tones.neutral[5] = context.getColor(android.R.color.system_neutral1_900);
            tones.neutral2[0] = context.getColor(android.R.color.system_neutral2_300);
            tones.neutral2[1] = context.getColor(android.R.color.system_neutral2_500);
            // Live theme error roles: the only red-hue family Monet guarantees. Fall back to the
            // tertiary slots if the theme ever lacks them. Note: colorError lives in AppCompat,
            // the container roles in MDC (verified against material 1.14.0 / appcompat 1.7.0).
            tones.accents[AERR][E_ERR] = MaterialColors.getColor(context,
                androidx.appcompat.R.attr.colorError,
                tones.accents[A3][night ? T200 : T600]);
            tones.accents[AERR][E_CONT] = MaterialColors.getColor(context,
                com.google.android.material.R.attr.colorErrorContainer, tones.accents[A3][T500]);
            tones.accents[AERR][E_ONCONT] = MaterialColors.getColor(context,
                com.google.android.material.R.attr.colorOnErrorContainer, tones.accents[A3][T800]);
            // Theme seed for harmonization: the M3 primary role carries the wallpaper hue.
            int seed = MaterialColors.getColor(context, androidx.appcompat.R.attr.colorPrimary,
                tones.accents[A1][T600]);
            Scheme scheme = deriveScheme(tones, night, seed);
            if (useCache) {
                synchronized (MonetTerminalColors.class) {
                    sCachedNight = night;
                    sCachedWallpaperId = wallpaperId;
                    sCachedScheme = scheme;
                }
            }
            TerminalColors.COLOR_SCHEME.updateWithMonetColors(scheme.ansi, scheme.foreground,
                scheme.background, scheme.cursor);
            return true;
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to resolve Monet system colors", e);
            return false;
        }
    }

    /** Wallpaper id for cache invalidation, or -1 when unreadable (disables caching). */
    private static int currentWallpaperId(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return android.app.WallpaperManager.getInstance(context)
                    .getWallpaperId(android.app.WallpaperManager.FLAG_SYSTEM);
            }
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to read wallpaper id", e);
        }
        return -1;
    }

    /**
     * Pure selector: derive a terminal scheme from resolved Monet tones plus the theme
     * seed color. Night mode picks the vivid tone targets and fixed roles; no Android
     * calls, plain-JVM testable (Hct/Blend are pure JVM math).
     *
     * @param tones resolved framework palettes (neutrals for surfaces, error roles
     *              as the red hue anchor).
     * @param night true for dark backgrounds, false for light ones.
     * @param seedArgb theme seed ({@code colorPrimary}) whose hue tints the palette.
     */
    static Scheme deriveScheme(ResolvedTones tones, boolean night, int seedArgb) {
        Scheme scheme = new Scheme();
        int[] n = tones.neutral;
        int[] n2 = tones.neutral2;
        if (night) {
            scheme.foreground = n[N50];
            scheme.background = n[N900];
            scheme.ansi[0] = n[N800];
            scheme.ansi[7] = n2[0];
            scheme.ansi[8] = n2[1];
            scheme.ansi[15] = n[N50];
        } else {
            scheme.foreground = n[N900];
            scheme.background = n[N50];
            scheme.ansi[0] = n[N900];
            scheme.ansi[7] = n2[0];
            scheme.ansi[8] = n2[1];
            scheme.ansi[15] = n[N100];
        }
        Hct seedHct = Hct.fromInt(seedArgb);
        boolean monoSeed = seedHct.getChroma() < MONO_CHROMA_THRESHOLD;
        scheme.cursor = vividCursor(seedHct, monoSeed, scheme.background, night);

        int errorArgb = tones.accents[AERR][E_ERR];
        // Red keeps the error hue so danger stays red on every wallpaper; if the theme
        // error role itself is achromatic something is very wrong — skip harmonizing.
        boolean monoError = Hct.fromInt(errorArgb).getChroma() < MONO_CHROMA_THRESHOLD;
        int redKey = monoError ? 0 : errorArgb;

        Set<Integer> taken = new HashSet<>();
        taken.add(scheme.foreground);
        taken.add(scheme.background);
        taken.add(scheme.cursor);
        taken.add(scheme.ansi[0]);
        taken.add(scheme.ansi[7]);
        taken.add(scheme.ansi[8]);
        taken.add(scheme.ansi[15]);
        List<double[]> placed = new ArrayList<>();
        for (int role : ROLE_ORDER) {
            double baseHue = BASE_HUES[role - 1];
            int keyArgb;
            if (role == 1) {
                keyArgb = redKey;
            } else if (monoSeed) {
                // Monochrome wallpaper: the seed hue is meaningless, keep pure bases.
                keyArgb = 0;
            } else {
                keyArgb = seedArgb;
            }
            int dim = vividRole(baseHue, keyArgb,
                night ? DIM_CHROMA_DARK : DIM_CHROMA_LIGHT,
                night ? DIM_TONE_DARK : DIM_TONE_LIGHT,
                scheme.background, taken, placed, night);
            taken.add(dim);
            placed.add(hueToneOf(dim));
            int bright = vividRole(baseHue, keyArgb,
                night ? BRIGHT_CHROMA_DARK : BRIGHT_CHROMA_LIGHT,
                night ? BRIGHT_TONE_DARK : BRIGHT_TONE_LIGHT,
                scheme.background, taken, placed, night);
            taken.add(bright);
            placed.add(hueToneOf(bright));
            scheme.ansi[role] = dim;
            scheme.ansi[role + 8] = bright;
        }
        return scheme;
    }

    /**
     * Synthesize one vivid ANSI color: HCT color at the requested chroma/tone for the
     * semantic base hue, harmonized toward the key color, then tone-stepped until it
     * clears the contrast, uniqueness and hue-separation gates.
     *
     * @param baseHue hue anchor of the ANSI role (from {@link #BASE_HUES}).
     * @param keyArgb harmonization key (seed or error color), or 0 to skip harmonizing.
     */
    private static int vividRole(double baseHue, int keyArgb,
                                 double reqChroma, double reqTone, int background,
                                 Set<Integer> taken, List<double[]> placed, boolean night) {
        int color = Hct.from(baseHue, reqChroma, reqTone).toInt();
        if (keyArgb != 0)
            color = Blend.harmonize(color, keyArgb);
        // Re-read everything post-harmonize: Blend only rotates hue, but the gate
        // below must work on the true hue (and guards against any solver drift).
        Hct hct = Hct.fromInt(color);
        double hue = hct.getHue();
        double chroma = hct.getChroma();
        double tone = hct.getTone();
        for (int step = 0; step < 8
            && (contrastRatio(color, background) < MIN_CONTRAST
                || taken.contains(color)
                || hueCollides(hue, tone, placed)); step++) {
            tone = night ? Math.min(92.0, tone + 5.0) : Math.max(15.0, tone - 5.0);
            color = Hct.from(hue, chroma, tone).toInt();
            Hct stepped = Hct.fromInt(color);
            hue = stepped.getHue();
            chroma = stepped.getChroma();
            tone = stepped.getTone();
            if ((night && tone >= 92.0) || (!night && tone <= 15.0))
                break;
        }
        return color;
    }

    /** Cursor uses the seed hue at a readable tone, falling back to white/black. */
    private static int vividCursor(Hct seedHct, boolean monoSeed, int background, boolean night) {
        if (!monoSeed) {
            int color = Hct.from(seedHct.getHue(), 60.0, night ? 72.0 : 42.0).toInt();
            if (contrastRatio(color, background) >= MIN_CONTRAST)
                return color;
        }
        return night ? 0xFFFFFFFF : 0xFF000000;
    }

    /** True when (hue, tone) sits too close to an already placed chromatic role. */
    private static boolean hueCollides(double hue, double tone, List<double[]> placed) {
        for (double[] ht : placed) {
            if (hueDifference(hue, ht[0]) < MIN_HUE_SEPARATION
                && Math.abs(tone - ht[1]) < MIN_TONE_SEPARATION)
                return true;
        }
        return false;
    }

    private static double[] hueToneOf(int color) {
        Hct hct = Hct.fromInt(color);
        return new double[] {hct.getHue(), hct.getTone()};
    }

    /** Smallest circular distance between two HCT hues in degrees. */
    static double hueDifference(double a, double b) {
        double d = Math.abs(a - b) % 360.0;
        return d > 180.0 ? 360.0 - d : d;
    }

    /** WCAG contrast ratio of two opaque ARGB colors. */
    static double contrastRatio(int a, int b) {
        double l1 = luminance(a);
        double l2 = luminance(b);
        if (l1 < l2) {
            double tmp = l1;
            l1 = l2;
            l2 = tmp;
        }
        return (l1 + 0.05) / (l2 + 0.05);
    }

    /** WCAG relative luminance from raw channels (no android.graphics dependency). */
    static double luminance(int color) {
        return 0.2126 * linear((color >> 16) & 0xFF)
            + 0.7152 * linear((color >> 8) & 0xFF)
            + 0.0722 * linear(color & 0xFF);
    }

    private static double linear(int channel) {
        double c = channel / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
