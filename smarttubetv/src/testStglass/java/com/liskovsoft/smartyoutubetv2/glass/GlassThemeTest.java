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
