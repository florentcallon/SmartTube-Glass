package com.liskovsoft.smartyoutubetv2.glass;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.ConscryptMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
@ConscryptMode(ConscryptMode.Mode.OFF) // the app ships conscrypt-android, whose JNI only exists on devices
public class GlassThumbnailSamplerTest {
    /** A displayed drawable that cannot be drawn on a software canvas (like a HARDWARE bitmap). */
    private static class UndrawableDrawable extends Drawable {
        @Override
        public void draw(Canvas canvas) {
            throw new IllegalArgumentException("Software rendering doesn't support hardware bitmaps");
        }

        @Override
        public int getIntrinsicWidth() {
            return 160;
        }

        @Override
        public int getIntrinsicHeight() {
            return 90;
        }

        @Override
        public ConstantState getConstantState() {
            return new ConstantState() {
                @Override
                public Drawable newDrawable() {
                    return new UndrawableDrawable();
                }

                @Override
                public int getChangingConfigurations() {
                    return 0;
                }
            };
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }
    }

    @Test
    public void bitmapDrawableIsSampledWithoutTouchingItsBounds() {
        Bitmap bitmap = Bitmap.createBitmap(160, 90, Bitmap.Config.ARGB_8888);
        BitmapDrawable displayed = new BitmapDrawable(Resources.getSystem(), bitmap);
        displayed.setBounds(0, 0, 320, 180);

        Bitmap sample = GlassThumbnailSampler.sample(displayed);

        assertNotNull(sample);
        assertEquals(GlassAmbient.SAMPLE_WIDTH, sample.getWidth());
        assertEquals(GlassAmbient.SAMPLE_HEIGHT, sample.getHeight());
        assertEquals(new Rect(0, 0, 320, 180), displayed.getBounds());
    }

    @Test
    public void undrawableDrawableGivesNullAndKeepsItsBounds() {
        Drawable displayed = new UndrawableDrawable();
        displayed.setBounds(0, 0, 320, 180);

        assertNull(GlassThumbnailSampler.sample(displayed));
        assertEquals(new Rect(0, 0, 320, 180), displayed.getBounds());
    }

    @Test
    public void recycledBitmapGivesNull() {
        Bitmap bitmap = Bitmap.createBitmap(160, 90, Bitmap.Config.ARGB_8888);
        BitmapDrawable displayed = new BitmapDrawable(Resources.getSystem(), bitmap);
        bitmap.recycle();

        assertNull(GlassThumbnailSampler.sample(displayed));
    }

    @Test
    public void missingOrEmptyDrawableGivesNull() {
        assertNull(GlassThumbnailSampler.sample(null));
        assertNull(GlassThumbnailSampler.sample(new BitmapDrawable(Resources.getSystem(), (Bitmap) null)));
    }
}
