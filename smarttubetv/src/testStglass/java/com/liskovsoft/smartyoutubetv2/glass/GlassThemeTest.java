package com.liskovsoft.smartyoutubetv2.glass;

import android.app.Application;
import android.content.Context;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
import com.liskovsoft.smartyoutubetv2.tv.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.ConscryptMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
@ConscryptMode(ConscryptMode.Mode.OFF) // the app ships conscrypt-android, whose JNI only exists on devices
public class GlassThemeTest {
    private static int themeColor(int themeResId, int attr) {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(), themeResId);
        TypedValue value = new TypedValue();
        assertTrue("attribute missing from theme", context.getTheme().resolveAttribute(attr, value, true));
        return value.resourceId != 0 ? ContextCompat.getColor(context, value.resourceId) : value.data;
    }

    private static Context glassContext(int themeResId) {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(), themeResId);
        GlassTheme.applyDefaults(context.getTheme());
        return context;
    }

    private static boolean themeBoolean(Context context, int attr) {
        TypedValue value = new TypedValue();
        assertTrue("attribute missing from theme", context.getTheme().resolveAttribute(attr, value, true));
        return value.data != 0;
    }

    private static int resolvedColor(Context context, int attr) {
        TypedValue value = new TypedValue();
        assertTrue("attribute missing from theme", context.getTheme().resolveAttribute(attr, value, true));
        return value.resourceId != 0 ? ContextCompat.getColor(context, value.resourceId) : value.data;
    }

    @Test
    public void headersDrawablesInflateInEveryScheme() {
        int[] themes = {R.style.App_Theme_Glass_Noir_Browse, R.style.App_Theme_Glass_Rose_Browse, R.style.App_Theme_DarkGrey_Browse};
        for (int theme : themes) {
            Context context = glassContext(theme);
            assertNotNull(ContextCompat.getDrawable(context, R.drawable.glass_headers_panel));
            assertNotNull(ContextCompat.getDrawable(context, R.drawable.glass_header_pill));
        }
    }

    @Test
    public void defaultsDoNotOverrideGlassThemes() {
        Context context = glassContext(R.style.App_Theme_Glass_Rose_Browse);

        assertEquals(0xFFFF5A5F, resolvedColor(context, R.attr.glassAccent));
        assertTrue(themeBoolean(context, R.attr.glassAmbient));
    }

    @Test
    public void classicSchemeGetsNeutralDefaults() {
        Context context = glassContext(R.style.App_Theme_DarkGrey_Browse);

        assertEquals(0xFFFF0033, resolvedColor(context, R.attr.glassAccent));
        assertFalse(themeBoolean(context, R.attr.glassAmbient));
    }

    private static int stylePaddingDp(Context context, int styleAttr, int paddingAttr) {
        TypedValue style = new TypedValue();
        assertTrue("style attribute missing", context.getTheme().resolveAttribute(styleAttr, style, true));
        android.content.res.TypedArray a = context.obtainStyledAttributes(style.resourceId, new int[]{paddingAttr});
        try {
            return Math.round(a.getDimension(0, -1) / context.getResources().getDisplayMetrics().density);
        } finally {
            a.recycle();
        }
    }

    private static int styleFontFamily(Context context, int styleAttr) {
        TypedValue style = new TypedValue();
        assertTrue("style attribute missing", context.getTheme().resolveAttribute(styleAttr, style, true));
        android.content.res.TypedArray a = context.obtainStyledAttributes(style.resourceId, new int[]{android.R.attr.fontFamily});
        try {
            return a.getResourceId(0, 0);
        } finally {
            a.recycle();
        }
    }

    @Test
    public void cardAndRowTextsUseFigtree() {
        for (int theme : new int[]{R.style.App_Theme_Glass_Noir_Browse, R.style.App_Theme_Glass_Rose_Browse}) {
            Context context = glassContext(theme);
            for (int attr : new int[]{R.attr.imageCardViewTitleStyle, R.attr.imageCardViewContentStyle, R.attr.rowHeaderStyle}) {
                assertEquals("fontFamily of style attr " + attr, R.font.glass_figtree, styleFontFamily(context, attr));
            }
        }
    }

    /**
     * With the 88dp icon rail as main fragment margin, grids keep upstream's usable width (960 - 2 x 56 = 848dp)
     * only if their paddings shrink to 8 + 16dp: GridFragmentHelper sizes columns from the display width.
     */
    @Test
    public void browsePaddingsLeaveRoomForTheRail() {
        for (int theme : new int[]{R.style.App_Theme_Glass_Noir_Browse, R.style.App_Theme_Glass_Rose_Browse}) {
            Context context = glassContext(theme);
            assertEquals(8, stylePaddingDp(context, R.attr.itemsVerticalGridStyle, android.R.attr.paddingStart));
            assertEquals(16, stylePaddingDp(context, R.attr.itemsVerticalGridStyle, android.R.attr.paddingEnd));
            assertEquals(8, stylePaddingDp(context, R.attr.rowHorizontalGridStyle, android.R.attr.paddingStart));
            assertEquals(16, stylePaddingDp(context, R.attr.rowHorizontalGridStyle, android.R.attr.paddingEnd));
            assertEquals(8, stylePaddingDp(context, R.attr.rowHeaderDockStyle, android.R.attr.paddingStart));
            // Title (search, account) left-aligned above the panel, which starts below it
            assertEquals(24, stylePaddingDp(context, R.attr.browseTitleViewStyle, android.R.attr.paddingStart));
        }
    }

    /**
     * Upstream card presenters paint cardDefaultBackground on several nested views (the settings card on its
     * container and on its title): a translucent colour stacks into visible squares.
     */
    @Test
    public void cardBackgroundsAreOpaque() {
        for (int theme : new int[]{R.style.App_Theme_Glass_Noir_Browse, R.style.App_Theme_Glass_Rose_Browse}) {
            for (int attr : new int[]{R.attr.cardDefaultBackground, R.attr.cardSelectedBackground}) {
                int color = themeColor(theme, attr);
                assertEquals("alpha of card background attr " + attr + " in theme " + theme, 0xFF, color >>> 24);
            }
        }
    }
}
