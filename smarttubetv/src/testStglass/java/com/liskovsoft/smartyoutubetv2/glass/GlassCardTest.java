package com.liskovsoft.smartyoutubetv2.glass;

import android.app.Application;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import com.liskovsoft.smartyoutubetv2.tv.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.ConscryptMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class, qualifiers = "mdpi")
@ConscryptMode(ConscryptMode.Mode.OFF) // the app ships conscrypt-android, whose JNI only exists on devices
public class GlassCardTest {
    private static Context context() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.App_Theme_Glass_Noir_Browse);
        GlassTheme.applyDefaults(context.getTheme());
        return context;
    }

    /** ComplexImageView (a RelativeLayout) inflates text_badge_image_view.xml, a merge layout, into itself. */
    private static View inflateCardImage() {
        Context context = context();
        RelativeLayout parent = new RelativeLayout(context);
        LayoutInflater.from(context).inflate(R.layout.text_badge_image_view, parent, true);
        return parent;
    }

    private static GlassRoundedFrameLayout roundedParentOf(View root, int childId) {
        View parent = (View) root.findViewById(childId).getParent();
        assertTrue("parent of " + childId + " is " + parent.getClass().getSimpleName(), parent instanceof GlassRoundedFrameLayout);
        return (GlassRoundedFrameLayout) parent;
    }

    @Test
    public void thumbnailIsClippedToRoundedCorners() {
        GlassRoundedFrameLayout frame = roundedParentOf(inflateCardImage(), R.id.main_image);

        assertTrue(frame.getClipToOutline());
        assertEquals(16f, frame.getCornerRadius(), 0.01f);
    }

    @Test
    public void badgeIsRounded() {
        GlassRoundedFrameLayout frame = roundedParentOf(inflateCardImage(), R.id.extra_text_badge);

        assertTrue(frame.getClipToOutline());
        assertEquals(6f, frame.getCornerRadius(), 0.01f);
    }

    @Test
    public void badgeColourFromCodeKeepsRoundedClip() {
        View root = inflateCardImage();
        root.findViewById(R.id.extra_text_badge).setBackgroundColor(Color.RED); // what ComplexImageView.setBadgeColor does

        assertTrue(roundedParentOf(root, R.id.extra_text_badge).getClipToOutline());
    }

    @Test
    public void channelCardShowsFocusOutline() {
        View card = LayoutInflater.from(context()).inflate(R.layout.channel_card, null);

        assertTrue(card.isFocusable());
        assertTrue("channel card needs a focus foreground", card.getForeground() != null);
    }

    @Test
    public void progressUsesGlassDrawable() {
        Context context = context();
        TypedValue style = new TypedValue();
        assertTrue(context.getTheme().resolveAttribute(R.attr.cardProgressStyle, style, true));
        TypedArray a = context.obtainStyledAttributes(style.resourceId, new int[]{android.R.attr.progressDrawable});
        try {
            assertEquals(R.drawable.glass_card_progress, a.getResourceId(0, 0));
        } finally {
            a.recycle();
        }
    }
}
