package com.liskovsoft.smartyoutubetv2.glass;

import android.app.Application;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import com.liskovsoft.smartyoutubetv2.tv.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.ConscryptMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class, qualifiers = "mdpi")
@ConscryptMode(ConscryptMode.Mode.OFF) // the app ships conscrypt-android, whose JNI only exists on devices
public class GlassPlayerTest {
    private static Context playerContext(int theme) {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(), theme);
        GlassTheme.applyDefaults(context.getTheme());
        return context;
    }

    private static View controlsRow(int theme) {
        return LayoutInflater.from(playerContext(theme)).inflate(R.layout.lb_playback_transport_controls_row, null);
    }

    /** Rules as resolved once attached (start/end become left/right; the parent forces layoutDirection=ltr). */
    private static int rule(View view, int verb) {
        return ((RelativeLayout.LayoutParams) view.getLayoutParams()).getRule(verb);
    }

    @Test
    public void glassPlayerHasFloatingBar() {
        View row = controlsRow(R.style.App_Theme_Glass_Noir_Player);
        View bar = row.findViewById(R.id.transport_row);
        ViewGroup.MarginLayoutParams barParams = (ViewGroup.MarginLayoutParams) bar.getLayoutParams();

        assertNotNull("glass bar background", bar.getBackground());
        assertEquals(16, barParams.leftMargin);
        assertEquals(16, barParams.rightMargin);
        assertEquals(16, barParams.bottomMargin);
        assertNotNull("title capsule background", row.findViewById(R.id.controls_card).getBackground());
        View primaryControls = row.findViewById(R.id.controls_dock);
        assertEquals(RelativeLayout.TRUE, rule(primaryControls, RelativeLayout.CENTER_HORIZONTAL));
        assertEquals(0, rule(primaryControls, RelativeLayout.ALIGN_PARENT_LEFT));
    }

    @Test
    public void classicSchemeKeepsUpstreamPlayer() {
        View row = controlsRow(R.style.App_Theme_DarkGrey_Player);
        View bar = row.findViewById(R.id.transport_row);
        ViewGroup.MarginLayoutParams barParams = (ViewGroup.MarginLayoutParams) bar.getLayoutParams();

        assertNull(bar.getBackground());
        assertEquals(0, barParams.leftMargin);
        assertEquals(0, barParams.rightMargin);
        assertEquals(0, barParams.bottomMargin);
        assertNull(row.findViewById(R.id.controls_card).getBackground());
        View primaryControls = row.findViewById(R.id.controls_dock);
        assertEquals(RelativeLayout.TRUE, rule(primaryControls, RelativeLayout.ALIGN_PARENT_LEFT));
        assertEquals(0, rule(primaryControls, RelativeLayout.CENTER_HORIZONTAL));
    }

    @Test
    public void playerTimeUsesFigtree() {
        Context context = playerContext(R.style.App_Theme_Glass_Rose_Player);
        TypedValue style = new TypedValue();
        assertTrue(context.getTheme().resolveAttribute(R.attr.playbackControlsTimeStyle, style, true));
        TypedArray a = context.obtainStyledAttributes(style.resourceId, new int[]{android.R.attr.fontFamily});
        try {
            assertEquals(R.font.glass_figtree, a.getResourceId(0, 0));
        } finally {
            a.recycle();
        }
    }

    // Drawables with theme attributes are not cached, so instances never compare equal: check their kind.
    private static android.graphics.drawable.Drawable buttonDrawable(int theme, int layout) {
        View button = LayoutInflater.from(playerContext(theme)).inflate(layout, null).findViewById(R.id.button);
        return ((android.widget.ImageView) button).getDrawable();
    }

    @Test
    public void controlButtonsUseGlassFocus() {
        for (int layout : new int[]{R.layout.lb_control_button_primary, R.layout.lb_control_button_secondary}) {
            android.graphics.drawable.Drawable drawable = buttonDrawable(R.style.App_Theme_Glass_Noir_Player, layout);
            assertTrue("glass button background is a selector", drawable instanceof android.graphics.drawable.StateListDrawable);
            drawable.setState(new int[]{android.R.attr.state_focused});
            assertTrue("focused glass button is a disc", drawable.getCurrent() instanceof android.graphics.drawable.GradientDrawable);
        }
    }

    @Test
    public void classicControlButtonsAreUpstream() {
        Context context = playerContext(R.style.App_Theme_DarkGrey_Player);
        assertEquals(androidx.core.content.ContextCompat.getDrawable(context, R.drawable.lb_control_button_primary).getClass(),
                buttonDrawable(R.style.App_Theme_DarkGrey_Player, R.layout.lb_control_button_primary).getClass());
        assertEquals(androidx.core.content.ContextCompat.getDrawable(context, R.drawable.lb_control_button_secondary).getClass(),
                buttonDrawable(R.style.App_Theme_DarkGrey_Player, R.layout.lb_control_button_secondary).getClass());
        assertTrue(!(buttonDrawable(R.style.App_Theme_DarkGrey_Player, R.layout.lb_control_button_primary) instanceof android.graphics.drawable.StateListDrawable));
    }
}
