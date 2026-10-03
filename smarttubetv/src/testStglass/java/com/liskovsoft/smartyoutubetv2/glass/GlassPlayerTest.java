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
}
