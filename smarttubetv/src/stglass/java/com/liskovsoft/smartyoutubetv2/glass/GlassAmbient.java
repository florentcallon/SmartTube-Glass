package com.liskovsoft.smartyoutubetv2.glass;

/**
 * Android-free part of the ambient background: sample size, blur settings and focus debouncing.
 */
public final class GlassAmbient {
    public static final int SAMPLE_WIDTH = 96;
    public static final int SAMPLE_HEIGHT = 54;
    public static final int BLUR_RADIUS = 4;
    public static final int BLUR_PASSES = 3;
    public static final long DEBOUNCE_MS = 300;

    private GlassAmbient() {
    }

    public static boolean isUsableSize(int width, int height) {
        return width > 0 && height > 0;
    }

    /**
     * Blurs a {@link #SAMPLE_WIDTH}x{@link #SAMPLE_HEIGHT} thumbnail sample and lays the theme scrim over it.
     */
    public static int[] render(int[] samplePixels, int scrimArgb) {
        int[] blurred = GlassBlur.boxBlur(samplePixels, SAMPLE_WIDTH, SAMPLE_HEIGHT, BLUR_RADIUS, BLUR_PASSES);
        return GlassBlur.applyScrim(blurred, scrimArgb);
    }

    public interface Clock {
        long now();
    }

    /**
     * Keeps the last requested key and hands it out once no new request came for {@code delayMs}.
     */
    public static final class Debouncer {
        private final Clock mClock;
        private final long mDelayMs;
        private Object mPending;
        private long mRequestedAt;

        public Debouncer(Clock clock, long delayMs) {
            mClock = clock;
            mDelayMs = delayMs;
        }

        public void request(Object key) {
            mPending = key;
            mRequestedAt = mClock.now();
        }

        public Object poll() {
            if (mPending == null || mClock.now() - mRequestedAt < mDelayMs) {
                return null;
            }
            Object key = mPending;
            mPending = null;
            return key;
        }
    }
}
