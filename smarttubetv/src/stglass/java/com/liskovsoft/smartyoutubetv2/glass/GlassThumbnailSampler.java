package com.liskovsoft.smartyoutubetv2.glass;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

/**
 * Takes a small software copy of a thumbnail that is on screen, without ever touching the displayed
 * drawable (its bounds belong to the ImageView showing it).
 */
public final class GlassThumbnailSampler {
    private static final String TAG = "Glass";

    private GlassThumbnailSampler() {
    }

    /**
     * Returns a {@link GlassAmbient#SAMPLE_WIDTH}x{@link GlassAmbient#SAMPLE_HEIGHT} ARGB_8888 bitmap,
     * or null when the drawable is missing, not loaded, recycled or cannot be drawn. Never throws.
     */
    public static Bitmap sample(Drawable displayed) {
        if (displayed == null || !GlassAmbient.isUsableSize(displayed.getIntrinsicWidth(), displayed.getIntrinsicHeight())) {
            return null;
        }

        try {
            if (displayed instanceof BitmapDrawable) {
                return sampleBitmap(((BitmapDrawable) displayed).getBitmap());
            }
            return sampleCopy(displayed);
        } catch (RuntimeException e) {
            Log.w(TAG, "Thumbnail sample skipped", e);
            return null;
        }
    }

    private static Bitmap sampleBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            return null;
        }

        // Glide decodes thumbnails as HARDWARE bitmaps on API 26+: their pixels cannot be read directly.
        Bitmap source = bitmap;
        if (Build.VERSION.SDK_INT >= 26 && bitmap.getConfig() == Bitmap.Config.HARDWARE) {
            source = bitmap.copy(Bitmap.Config.ARGB_8888, false);
            if (source == null) {
                return null;
            }
        }

        Bitmap sample = Bitmap.createScaledBitmap(source, GlassAmbient.SAMPLE_WIDTH, GlassAmbient.SAMPLE_HEIGHT, true);
        if (source != bitmap && source != sample) {
            source.recycle();
        }
        return sample;
    }

    private static Bitmap sampleCopy(Drawable displayed) {
        Drawable.ConstantState state = displayed.getConstantState();
        if (state == null) {
            return null;
        }

        Drawable copy = state.newDrawable().mutate();
        Bitmap sample = Bitmap.createBitmap(GlassAmbient.SAMPLE_WIDTH, GlassAmbient.SAMPLE_HEIGHT, Bitmap.Config.ARGB_8888);
        copy.setBounds(0, 0, GlassAmbient.SAMPLE_WIDTH, GlassAmbient.SAMPLE_HEIGHT);
        try {
            copy.draw(new Canvas(sample));
        } catch (RuntimeException e) {
            sample.recycle();
            throw e;
        }
        return sample;
    }
}
