package com.termux.app.terminal;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import com.google.android.material.color.MaterialColors;
import com.termux.shared.logger.Logger;
import com.termux.terminal.TerminalColors;

import java.util.HashSet;
import java.util.Set;

/**
 * Derives the terminal 16-color ANSI palette from the Material You (Monet) dynamic
 * color palettes exposed by the framework on Android 12+ ({@code android:color/system_accent1_*},
 * {@code system_accent2_*}, {@code system_accent3_*}, {@code system_neutral1_*}).
 *
 * <p>
 * Only used when the user has no {@code ~/.termux/colors.properties} file; an existing
 * colors file always takes precedence and the stock xterm scheme is kept below API 31.
 * Indexes 16-255 keep the stock xterm values.
 * </p>
 *
 * <p>
 * Role mapping (my design decision, not in M3 — the framework only exposes three
 * chromatic hues, so each accent palette serves two ANSI roles):
 * blue/yellow &lt;- accent1 (primary), green/cyan &lt;- accent2, magenta &lt;- accent3,
 * red &lt;- the live theme error roles (the only red-hue family Monet guarantees),
 * mid grays &lt;- seed-tinted neutral2, black/white and foreground/background &lt;- neutral1,
 * cursor &lt;- accent1 at the M3 primary tone. Exact hues follow the user's wallpaper.
 * </p>
 *
 * <p>
 * Robustness: every chromatic role is picked from a preference-ordered candidate list of
 * (palette, tone) slots. The first candidate with a WCAG contrast ratio of at least
 * {@link #MIN_CONTRAST} against the background <i>and</i> not already used by another role
 * wins. Afterwards the non-red chromatics are harmonized toward the theme primary
 * (M3 {@code Blend.harmonize} color science) subject to the same contrast and
 * distinctness gates, so the palette stays cohesive without losing readability.
 * Minimum 3:1 comes from M3 (3:1 large text minimum; roles guarantee 3:1 pairs).
 * </p>
 */
public final class MonetTerminalColors {

    private static final String LOG_TAG = "MonetTerminalColors";

    /** Minimum WCAG contrast ratio of a chromatic ANSI color against the background. */
    static final double MIN_CONTRAST = 3.0;

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

    /** Candidate (palette, tone) slots per ANSI role for dark backgrounds: [role][dim/bright][candidates]. */
    private static final int[][][][] DARK_CANDIDATES = {
        null, // 0 black: fixed neutral
        {{{AERR, E_CONT}, {A3, T500}, {A3, T400}, {A3, T600}, {A3, T300}}, {{AERR, E_ERR}, {A3, T200}, {A3, T300}, {A3, T100}}}, // 1 red
        {{{A2, T500}, {A2, T400}, {A2, T600}, {A2, T300}}, {{A2, T200}, {A2, T300}, {A2, T100}}}, // 2 green
        {{{A1, T300}, {A1, T400}, {A1, T200}, {A1, T500}}, {{A1, T100}, {A1, T200}, {A1, T300}}}, // 3 yellow
        {{{A1, T500}, {A1, T400}, {A1, T600}, {A1, T300}}, {{A1, T200}, {A1, T300}, {A1, T100}}}, // 4 blue
        {{{A3, T300}, {A3, T400}, {A3, T200}, {A3, T500}}, {{A3, T100}, {A3, T200}, {A3, T300}}}, // 5 magenta
        {{{A2, T300}, {A2, T400}, {A2, T200}, {A2, T500}}, {{A2, T100}, {A2, T200}, {A2, T300}}}, // 6 cyan
    };

    /** Candidate (palette, tone) slots per ANSI role for light backgrounds. */
    private static final int[][][][] LIGHT_CANDIDATES = {
        null, // 0 black: fixed neutral
        {{{AERR, E_ERR}, {A3, T600}, {A3, T500}, {A3, T700}}, {{AERR, E_ONCONT}, {A3, T800}, {A3, T700}, {A3, T900}}}, // 1 red
        {{{A2, T600}, {A2, T500}, {A2, T700}}, {{A2, T800}, {A2, T700}, {A2, T900}}}, // 2 green
        {{{A1, T500}, {A1, T600}, {A1, T400}}, {{A1, T700}, {A1, T800}, {A1, T600}}}, // 3 yellow
        {{{A1, T600}, {A1, T500}, {A1, T700}}, {{A1, T800}, {A1, T700}, {A1, T900}}}, // 4 blue
        {{{A3, T500}, {A3, T600}, {A3, T400}}, {{A3, T700}, {A3, T800}, {A3, T600}}}, // 5 magenta
        {{{A2, T500}, {A2, T600}, {A2, T400}}, {{A2, T700}, {A2, T800}, {A2, T600}}}, // 6 cyan
    };

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
            Scheme scheme = deriveScheme(tones, night);
            int primary = MaterialColors.getColor(context, androidx.appcompat.R.attr.colorPrimary,
                scheme.ansi[12]);
            final int seedPrimary = primary;
            applyHarmonize(scheme, color -> MaterialColors.harmonize(color, seedPrimary));
            TerminalColors.COLOR_SCHEME.updateWithMonetColors(scheme.ansi, scheme.foreground,
                scheme.background, scheme.cursor);
            return true;
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to resolve Monet system colors", e);
            return false;
        }
    }

    /**
     * Pure selector: derive a terminal scheme from resolved Monet tones. Night mode picks
     * the candidate tables and fixed roles; no Android calls, plain-JVM testable.
     */
    static Scheme deriveScheme(ResolvedTones tones, boolean night) {
        Scheme scheme = new Scheme();
        int[] n = tones.neutral;
        int[] n2 = tones.neutral2;
        if (night) {
            scheme.foreground = n[N50];
            scheme.background = n[N900];
            scheme.cursor = gatedCursor(tones.accents[A1][T200], scheme.background, true);
            scheme.ansi[0] = n[N800];
            scheme.ansi[7] = n2[0];
            scheme.ansi[8] = n2[1];
            scheme.ansi[15] = n[N50];
        } else {
            scheme.foreground = n[N900];
            scheme.background = n[N50];
            scheme.cursor = gatedCursor(tones.accents[A1][T600], scheme.background, false);
            scheme.ansi[0] = n[N900];
            scheme.ansi[7] = n2[0];
            scheme.ansi[8] = n2[1];
            scheme.ansi[15] = n[N100];
        }
        Set<Integer> taken = new HashSet<>();
        taken.add(scheme.foreground);
        taken.add(scheme.background);
        taken.add(scheme.cursor);
        taken.add(scheme.ansi[0]);
        taken.add(scheme.ansi[7]);
        taken.add(scheme.ansi[8]);
        taken.add(scheme.ansi[15]);
        int[][][][] tables = night ? DARK_CANDIDATES : LIGHT_CANDIDATES;
        for (int role : ROLE_ORDER) {
            int[][][] pair = tables[role];
            int dim = select(pair[0], tones.accents, scheme.background, taken);
            taken.add(dim);
            int bright = select(pair[1], tones.accents, scheme.background, taken);
            taken.add(bright);
            scheme.ansi[role] = dim;
            scheme.ansi[role + 8] = bright;
        }
        return scheme;
    }

    /**
     * Harmonize the non-red chromatics of an already-derived scheme, keeping the result only
     * when it still meets {@link #MIN_CONTRAST} and stays distinct. Red (1/9) is excluded to
     * preserve the error-role anchor. The harmonize function is injected so this stays
     * plain-JVM testable (MDC's {@code Blend} needs real android.graphics at runtime).
     */
    static void applyHarmonize(Scheme scheme, java.util.function.IntUnaryOperator harmonize) {
        Set<Integer> taken = new HashSet<>();
        taken.add(scheme.foreground);
        taken.add(scheme.background);
        taken.add(scheme.cursor);
        taken.add(scheme.ansi[0]);
        taken.add(scheme.ansi[1]);
        taken.add(scheme.ansi[7]);
        taken.add(scheme.ansi[8]);
        taken.add(scheme.ansi[9]);
        taken.add(scheme.ansi[15]);
        for (int role : new int[] {2, 4, 3, 5, 6, 10, 11, 12, 13, 14}) {
            int harmonized = harmonize.applyAsInt(scheme.ansi[role]);
            if (!taken.contains(harmonized)
                && contrastRatio(harmonized, scheme.background) >= MIN_CONTRAST) {
                scheme.ansi[role] = harmonized;
            }
            taken.add(scheme.ansi[role]);
        }
    }

    /**
     * Pick the first candidate that is unused and meets {@link #MIN_CONTRAST} against the
     * background; fall back to the first readable candidate, then to the preferred one.
     */
    private static int select(int[][] candidates, int[][] palettes, int background, Set<Integer> taken) {
        for (int[] c : candidates) {
            int color = palettes[c[0]][c[1]];
            if (!taken.contains(color) && contrastRatio(color, background) >= MIN_CONTRAST)
                return color;
        }
        for (int[] c : candidates) {
            int color = palettes[c[0]][c[1]];
            if (contrastRatio(color, background) >= MIN_CONTRAST)
                return color;
        }
        return palettes[candidates[0][0]][candidates[0][1]];
    }

    /** Cursor uses the M3 primary tone, falling back to white/black when it lacks contrast. */
    private static int gatedCursor(int preferred, int background, boolean night) {
        if (contrastRatio(preferred, background) >= MIN_CONTRAST)
            return preferred;
        return night ? 0xFFFFFFFF : 0xFF000000;
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
