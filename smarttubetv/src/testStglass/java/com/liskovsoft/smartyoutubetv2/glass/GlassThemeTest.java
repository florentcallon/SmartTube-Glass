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

    private static float themeDimensionDp(Context context, int attr) {
        TypedValue value = new TypedValue();
        assertTrue("attribute missing from theme", context.getTheme().resolveAttribute(attr, value, true));
        return value.getDimension(context.getResources().getDisplayMetrics()) / context.getResources().getDisplayMetrics().density;
    }

    private static int themeReference(Context context, int attr) {
        TypedValue value = new TypedValue();
        assertTrue("attribute missing from theme", context.getTheme().resolveAttribute(attr, value, false));
        return value.data; // 0 for @null
    }

    /** Classic schemes in the fork must look like upstream: no rail, panel, pills, rounded or outlined cards. */
    @Test
    public void classicSchemeHasNoGlassStructure() {
        Context context = glassContext(R.style.App_Theme_DarkGrey_Browse);

        assertEquals(0f, themeDimensionDp(context, R.attr.glassHeadersRailWidth), 0f);
        assertEquals(0f, themeDimensionDp(context, R.attr.glassHeadersPaddingTop), 0f);
        assertEquals(0f, themeDimensionDp(context, R.attr.glassCardRadius), 0f);
        assertEquals(0f, themeDimensionDp(context, R.attr.glassBadgeRadius), 0f);
        for (int attr : new int[]{R.attr.glassHeadersPanel, R.attr.glassHeaderPill, R.attr.glassCardFocus, R.attr.glassFocusOutline}) {
            assertEquals("reference attr " + attr, 0, themeReference(context, attr));
        }
    }

    @Test
    public void glassSchemesHaveTheGlassStructure() {
        for (int theme : new int[]{R.style.App_Theme_Glass_Noir_Browse, R.style.App_Theme_Glass_Rose_Browse}) {
            Context context = glassContext(theme);
            assertEquals(88f, themeDimensionDp(context, R.attr.glassHeadersRailWidth), 0f);
            assertEquals(88f, themeDimensionDp(context, R.attr.glassHeadersPaddingTop), 0f);
            assertEquals(16f, themeDimensionDp(context, R.attr.glassCardRadius), 0f);
            assertEquals(6f, themeDimensionDp(context, R.attr.glassBadgeRadius), 0f);
            assertEquals(R.drawable.glass_headers_panel, themeReference(context, R.attr.glassHeadersPanel));
            assertEquals(R.drawable.glass_header_pill, themeReference(context, R.attr.glassHeaderPill));
            assertEquals("the focus glow replaces the outline", 0, themeReference(context, R.attr.glassCardFocus));
            assertEquals(R.drawable.glass_focus_outline, themeReference(context, R.attr.glassFocusOutline));
        }
    }

    private static boolean styleClipsChildren(Context context, int styleAttr) {
        TypedValue style = new TypedValue();
        assertTrue("style attribute missing", context.getTheme().resolveAttribute(styleAttr, style, true));
        android.content.res.TypedArray a = context.obtainStyledAttributes(style.resourceId, new int[]{android.R.attr.clipChildren});
        try {
            return a.getBoolean(0, true);
        } finally {
            a.recycle();
        }
    }

    /** The focus glow draws outside the thumbnail: cards, rows and grids must not clip their children. */
    @Test
    public void cardsRowsAndGridsLetTheGlowOut() {
        for (int theme : new int[]{R.style.App_Theme_Glass_Noir_Browse, R.style.App_Theme_Glass_Rose_Browse}) {
            Context context = glassContext(theme);
            for (int attr : new int[]{R.attr.imageCardViewStyle, R.attr.rowHorizontalGridStyle, R.attr.itemsVerticalGridStyle}) {
                assertFalse("clipChildren of style attr " + attr, styleClipsChildren(context, attr));
            }
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
            // grids: 16 + 8 keeps upstream's usable width (960 - 88 - 24 = 848dp)
            assertEquals(16, stylePaddingDp(context, R.attr.itemsVerticalGridStyle, android.R.attr.paddingStart));
            assertEquals(8, stylePaddingDp(context, R.attr.itemsVerticalGridStyle, android.R.attr.paddingEnd));
            // rows scroll horizontally: upstream end padding (56dp) so the last card is never cut
            assertEquals(16, stylePaddingDp(context, R.attr.rowHorizontalGridStyle, android.R.attr.paddingStart));
            assertEquals(56, stylePaddingDp(context, R.attr.rowHorizontalGridStyle, android.R.attr.paddingEnd));
            assertEquals(16, stylePaddingDp(context, R.attr.rowHeaderDockStyle, android.R.attr.paddingStart));
            // Title (search, account) left-aligned above the panel, which starts below it
            assertEquals(24, stylePaddingDp(context, R.attr.browseTitleViewStyle, android.R.attr.paddingStart));
        }
    }

    /**
     * Upstream card presenters paint the card background colours on nested views (card and info area,
     * settings container and title): a partly translucent colour stacks into visible squares.
     */
    @Test
    public void cardBackgroundsAreOpaqueOrTransparent() {
        for (int theme : new int[]{R.style.App_Theme_Glass_Noir_Browse, R.style.App_Theme_Glass_Rose_Browse}) {
            for (int attr : new int[]{R.attr.cardDefaultBackground, R.attr.cardSelectedBackground}) {
                int alpha = themeColor(theme, attr) >>> 24;
                assertTrue("card background attr " + attr + " in theme " + theme + " has alpha " + alpha, alpha == 0 || alpha == 0xFF);
            }
        }
    }

    /**
     * card_selected_background_white is the focus fill of tag chips and settings titles (video and channel
     * cards use the transparent cardSelectedBackground attribute): an opaque accent keeps focus visible.
     */
    @Test
    public void glassSchemesUseFreeTextCardColours() {
        int[][] themeAccents = {{R.style.App_Theme_Glass_Noir_Browse, 0xFFFF0033}, {R.style.App_Theme_Glass_Rose_Browse, 0xFFFF5A5F}};
        for (int[] themeAccent : themeAccents) {
            Context context = glassContext(themeAccent[0]);
            assertEquals(0xC7FFFFFF, ContextCompat.getColor(context, R.color.card_default_text));
            assertEquals(0xFFFFFFFF, ContextCompat.getColor(context, R.color.card_selected_text_grey));
            assertEquals(themeAccent[1], ContextCompat.getColor(context, R.color.card_selected_background_white));
        }
    }

    @Test
    public void rowTitlesAreSemiBold() {
        Context context = glassContext(R.style.App_Theme_Glass_Noir_Browse);
        TypedValue style = new TypedValue();
        assertTrue(context.getTheme().resolveAttribute(R.attr.rowHeaderStyle, style, true));
        android.content.res.TypedArray a = context.obtainStyledAttributes(style.resourceId, new int[]{android.R.attr.textStyle});
        try {
            // bold (700) maps to Figtree's 600 entry in glass_figtree.xml
            assertEquals(android.graphics.Typeface.BOLD, a.getInt(0, 0));
        } finally {
            a.recycle();
        }
    }

    @Test
    public void classicSchemeKeepsUpstreamCardColours() {
        Context context = glassContext(R.style.App_Theme_DarkGrey_Browse);

        assertEquals(0xFFFFFFFF, ContextCompat.getColor(context, R.color.card_default_text));
        assertEquals(0xFF343434, ContextCompat.getColor(context, R.color.card_selected_text_grey));
        assertEquals(0xFFFFFFFF, ContextCompat.getColor(context, R.color.card_selected_background_white));
    }
}
