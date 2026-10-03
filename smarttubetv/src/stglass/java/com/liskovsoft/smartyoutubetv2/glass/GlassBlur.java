package com.liskovsoft.smartyoutubetv2.glass;

/**
 * Pixel operations for the glass ambient background, on ARGB int arrays so they run without Android.
 */
public final class GlassBlur {
    private GlassBlur() {
    }

    /**
     * Separable box blur (horizontal then vertical), repeated {@code passes} times.
     * Edge pixels are replicated, so borders do not darken. Returns a new array.
     */
    public static int[] boxBlur(int[] argb, int width, int height, int radius, int passes) {
        if (width <= 0 || height <= 0 || argb.length != width * height) {
            throw new IllegalArgumentException("Expected " + width + "x" + height + " pixels, got " + argb.length);
        }

        int[] result = argb.clone();
        if (radius <= 0) {
            return result;
        }

        int[] buffer = new int[result.length];
        for (int pass = 0; pass < passes; pass++) {
            blurLine(result, buffer, width, height, radius, true);
            blurLine(buffer, result, width, height, radius, false);
        }
        return result;
    }

    /**
     * Composites an ARGB scrim over every pixel; the result is opaque.
     */
    public static int[] applyScrim(int[] argb, int scrimArgb) {
        int alpha = (scrimArgb >>> 24) & 0xFF;
        int[] result = new int[argb.length];
        for (int i = 0; i < argb.length; i++) {
            int pixel = argb[i];
            result[i] = 0xFF000000
                    | blend((pixel >> 16) & 0xFF, (scrimArgb >> 16) & 0xFF, alpha) << 16
                    | blend((pixel >> 8) & 0xFF, (scrimArgb >> 8) & 0xFF, alpha) << 8
                    | blend(pixel & 0xFF, scrimArgb & 0xFF, alpha);
        }
        return result;
    }

    private static int blend(int source, int scrim, int alpha) {
        return Math.round((source * (255 - alpha) + scrim * alpha) / 255f);
    }

    private static void blurLine(int[] in, int[] out, int width, int height, int radius, boolean horizontal) {
        int lines = horizontal ? height : width;
        int length = horizontal ? width : height;
        int count = 2 * radius + 1;

        for (int line = 0; line < lines; line++) {
            for (int pos = 0; pos < length; pos++) {
                int a = 0, r = 0, g = 0, b = 0;
                for (int k = -radius; k <= radius; k++) {
                    int p = Math.min(length - 1, Math.max(0, pos + k));
                    int pixel = horizontal ? in[line * width + p] : in[p * width + line];
                    a += (pixel >>> 24) & 0xFF;
                    r += (pixel >> 16) & 0xFF;
                    g += (pixel >> 8) & 0xFF;
                    b += pixel & 0xFF;
                }
                int index = horizontal ? line * width + pos : pos * width + line;
                out[index] = (a + count / 2) / count << 24
                        | (r + count / 2) / count << 16
                        | (g + count / 2) / count << 8
                        | (b + count / 2) / count;
            }
        }
    }
}
