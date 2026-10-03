package com.liskovsoft.smartyoutubetv2.glass;

import android.app.Application;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
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
        // Expanded panel: grid 262dp wide (270 - 8 root padding) minus 24dp grid padding = 238dp available.
        View item = measuredHeader(238);

        assertEquals(44, item.getMeasuredHeight());
        assertEquals(230, item.getMeasuredWidth());
    }

    @Test
    public void pillShrinksToTheRail() {
        // Collapsed: grid clamped to 80dp minus 24dp padding = 56dp available.
        assertEquals(56, measuredHeader(56).getMeasuredWidth());
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
