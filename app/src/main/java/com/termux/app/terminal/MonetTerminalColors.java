package com.termux.app.terminal;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import com.termux.shared.logger.Logger;
import com.termux.terminal.TerminalColors;

/**
 * Derives the terminal 16-color ANSI palette from the Material You (Monet) dynamic
 * color palettes exposed by the framework on Android 12+ ({@code android:color/system_accent1_*},
 * {@code system_accent2_*}, {@code system_accent3_*}, {@code system_neutral1_*}).
 *
 * <p>
 * Only used when the user has no {@code ~/.termux/colors.properties} file; an existing
 * colors file always takes precedence and the stock xterm scheme is kept below API 31.
 * </p>
 *
 * <p>
 * Mapping notes (my design decision, not in M3 — the framework only exposes three
 * chromatic hues, so each accent palette serves two ANSI roles at distinct tones, and
 * exact hues follow the user's wallpaper):
 * <ul>
 * <li>blue/yellow &lt;- accent1 (primary), green/cyan &lt;- accent2, red/magenta &lt;- accent3</li>
 * <li>black/white/grays, foreground and background &lt;- neutral1</li>
 * <li>cursor &lt;- accent1 at the M3 primary tone (200 dark / 600 light)</li>
 * </ul>
 * Indexes 16-255 keep the stock xterm values.
 * </p>
 */
public final class MonetTerminalColors {

    private static final String LOG_TAG = "MonetTerminalColors";

    private MonetTerminalColors() {}

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
            int[] ansi = new int[16];
            int foreground, background, cursor;
            if (night) {
                foreground = color(context, android.R.color.system_neutral1_50);
                background = color(context, android.R.color.system_neutral1_900);
                cursor = color(context, android.R.color.system_accent1_200);
                ansi[0] = color(context, android.R.color.system_neutral1_800);
                ansi[1] = color(context, android.R.color.system_accent3_500);
                ansi[2] = color(context, android.R.color.system_accent2_500);
                ansi[3] = color(context, android.R.color.system_accent1_300);
                ansi[4] = color(context, android.R.color.system_accent1_500);
                ansi[5] = color(context, android.R.color.system_accent3_300);
                ansi[6] = color(context, android.R.color.system_accent2_300);
                ansi[7] = color(context, android.R.color.system_neutral1_300);
                ansi[8] = color(context, android.R.color.system_neutral1_500);
                ansi[9] = color(context, android.R.color.system_accent3_200);
                ansi[10] = color(context, android.R.color.system_accent2_200);
                ansi[11] = color(context, android.R.color.system_accent1_100);
                ansi[12] = color(context, android.R.color.system_accent1_200);
                ansi[13] = color(context, android.R.color.system_accent3_100);
                ansi[14] = color(context, android.R.color.system_accent2_100);
                ansi[15] = color(context, android.R.color.system_neutral1_50);
            } else {
                foreground = color(context, android.R.color.system_neutral1_900);
                background = color(context, android.R.color.system_neutral1_50);
                cursor = color(context, android.R.color.system_accent1_600);
                ansi[0] = color(context, android.R.color.system_neutral1_900);
                ansi[1] = color(context, android.R.color.system_accent3_600);
                ansi[2] = color(context, android.R.color.system_accent2_600);
                ansi[3] = color(context, android.R.color.system_accent1_500);
                ansi[4] = color(context, android.R.color.system_accent1_600);
                ansi[5] = color(context, android.R.color.system_accent3_500);
                ansi[6] = color(context, android.R.color.system_accent2_500);
                ansi[7] = color(context, android.R.color.system_neutral1_300);
                ansi[8] = color(context, android.R.color.system_neutral1_500);
                ansi[9] = color(context, android.R.color.system_accent3_800);
                ansi[10] = color(context, android.R.color.system_accent2_800);
                ansi[11] = color(context, android.R.color.system_accent1_700);
                ansi[12] = color(context, android.R.color.system_accent1_800);
                ansi[13] = color(context, android.R.color.system_accent3_700);
                ansi[14] = color(context, android.R.color.system_accent2_700);
                ansi[15] = color(context, android.R.color.system_neutral1_100);
            }
            TerminalColors.COLOR_SCHEME.updateWithMonetColors(ansi, foreground, background, cursor);
            return true;
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to resolve Monet system colors", e);
            return false;
        }
    }

    private static int color(Context context, int resId) {
        return context.getColor(resId);
    }
}
