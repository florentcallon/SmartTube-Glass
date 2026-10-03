package com.liskovsoft.smartyoutubetv2.glass;

import android.content.res.Resources;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Glass layouts and drawables read the glass* theme attributes; classic colour schemes do not define them.
 */
public final class GlassTheme {
    private GlassTheme() {
    }

    /**
     * Gives the theme neutral glass values, without overriding those a Glass scheme already set.
     */
    public static void applyDefaults(Resources.Theme theme) {
        theme.applyStyle(R.style.GlassDefaults, false);
    }
}
