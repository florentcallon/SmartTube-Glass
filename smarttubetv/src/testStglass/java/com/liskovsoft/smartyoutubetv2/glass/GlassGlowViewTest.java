package com.liskovsoft.smartyoutubetv2.glass;

import android.app.Application;
import android.content.Context;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class, qualifiers = "mdpi")
@ConscryptMode(ConscryptMode.Mode.OFF) // the app ships conscrypt-android, whose JNI only exists on devices
public class GlassGlowViewTest {
    private static RelativeLayout cardImage(int theme) {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(), theme);
        GlassTheme.applyDefaults(context.getTheme());
        RelativeLayout parent = new RelativeLayout(context);
        LayoutInflater.from(context).inflate(R.layout.text_badge_image_view, parent, true);
        return parent;
    }

    private static GlassGlowView glowOf(RelativeLayout cardImage) {
        for (int i = 0; i < cardImage.getChildCount(); i++) {
            View child = cardImage.getChildAt(i);
            if (child instanceof GlassGlowView) {
                return (GlassGlowView) child;
            }
        }
        throw new AssertionError("no GlassGlowView in text_badge_image_view");
    }

    @Test
    public void glowSitsBehindTheThumbnail() {
        RelativeLayout cardImage = cardImage(R.style.App_Theme_Glass_Noir_Browse);
        GlassGlowView glow = glowOf(cardImage);
        View frame = (View) cardImage.findViewById(R.id.main_image).getParent();

        assertTrue("glow must be drawn before the thumbnail", cardImage.indexOfChild(glow) < cardImage.indexOfChild(frame));
    }

    @Test
    public void glowsOnlyWhenTheCardIsSelected() {
        RelativeLayout cardImage = cardImage(R.style.App_Theme_Glass_Rose_Browse);
        GlassGlowView glow = glowOf(cardImage);

        assertFalse(glow.isGlowing());
        cardImage.setSelected(true); // BaseCardView selects itself when focused; selection reaches its children
        assertTrue(glow.isGlowing());
        assertEquals(0xAAFF5A5F, glow.getGlowColor());
        cardImage.setSelected(false);
        assertFalse(glow.isGlowing());
    }

    @Test
    public void classicSchemesNeverGlow() {
        RelativeLayout cardImage = cardImage(R.style.App_Theme_DarkGrey_Browse);
        cardImage.setSelected(true);

        assertFalse(glowOf(cardImage).isGlowing());
    }
}
