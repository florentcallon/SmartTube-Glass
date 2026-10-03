package com.liskovsoft.smartyoutubetv2.glass;

import android.app.Application;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class, qualifiers = "mdpi")
@ConscryptMode(ConscryptMode.Mode.OFF) // the app ships conscrypt-android, whose JNI only exists on devices
public class GlassHeadersTest {
    private static Context context() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.App_Theme_Glass_Noir_Browse);
        GlassTheme.applyDefaults(context.getTheme());
        return context;
    }

    /**
     * IconHeaderItemPresenter inflates the item with a null parent and HeadersSupportFragment wraps it in a
     * FrameLayout measured with AT_MOST width: the pill size must come from view attributes, not layout params.
     */
    private static View measuredHeader(int availableWidthPx) {
        Context context = context();
        View item = LayoutInflater.from(context).inflate(R.layout.icon_header_item, null);
        FrameLayout wrapper = new FrameLayout(context);
        wrapper.addView(item);
        wrapper.measure(MeasureSpec.makeMeasureSpec(availableWidthPx, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        return item;
    }

    @Test
    public void pillIs44dpHighAndFillsTheExpandedPanel() {
        // Expanded: grid 254dp wide (270 - 16 root paddingEnd) minus 24dp grid padding = 230dp available;
        // the pill (24-254dp) ends 8dp before the panel edge (262dp).
        View item = measuredHeader(230);

        assertEquals(44, item.getMeasuredHeight());
        assertEquals(230, item.getMeasuredWidth());
    }

    @Test
    public void pillShrinksToTheRail() {
        // Collapsed: grid clamped to 72dp (88 - 16) minus 24dp padding = 48dp: pill 24-72dp inside the 16-80dp panel.
        assertEquals(48, measuredHeader(48).getMeasuredWidth());
    }

    @Test
    public void currentSectionIconTakesTheAccentColour() {
        View item = LayoutInflater.from(context()).inflate(R.layout.icon_header_item, null);
        ImageView icon = item.findViewById(R.id.header_icon);
        icon.setImageDrawable(new ColorDrawable(Color.WHITE));

        GlassHeaders.tintIcon(item, true, 0xFFFF0033);
        assertNotNull(icon.getColorFilter());

        GlassHeaders.tintIcon(item, false, 0xFFFF0033);
        assertNull(icon.getColorFilter());
    }

    @Test
    public void channelAvatarsAreNeverTinted() {
        View item = LayoutInflater.from(context()).inflate(R.layout.icon_header_item, null);
        ImageView icon = item.findViewById(R.id.header_icon);
        icon.setTag(com.bumptech.glide.R.id.glide_custom_view_target_tag, new Object()); // loaded by Glide

        GlassHeaders.tintIcon(item, true, 0xFFFF0033);

        assertNull(icon.getColorFilter());
    }

    @Test
    public void activatesOnlyTheSelectedEntry() {
        LinearLayout grid = new LinearLayout(context());
        for (int i = 0; i < 4; i++) {
            grid.addView(new View(grid.getContext()));
        }
        grid.getChildAt(0).setActivated(true);

        GlassHeaders.activateSelected(grid, 2, grid::indexOfChild);

        assertFalse(grid.getChildAt(0).isActivated());
        assertFalse(grid.getChildAt(1).isActivated());
        assertTrue(grid.getChildAt(2).isActivated());
        assertFalse(grid.getChildAt(3).isActivated());
    }
}
