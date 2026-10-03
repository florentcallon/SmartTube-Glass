package com.liskovsoft.smartyoutubetv2.glass;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class GlassBlurTest {
    private static int[] filled(int length, int color) {
        int[] pixels = new int[length];
        Arrays.fill(pixels, color);
        return pixels;
    }

    private static int red(int argb) {
        return (argb >> 16) & 0xFF;
    }

    @Test
    public void uniformImageStaysUniform() {
        int[] result = GlassBlur.boxBlur(filled(8 * 4, 0xFF336699), 8, 4, 2, 3);

        assertArrayEquals(filled(8 * 4, 0xFF336699), result);
    }

    @Test
    public void zeroRadiusIsIdentity() {
        int[] input = {0xFF000000, 0xFF111111, 0xFF222222, 0xFF333333, 0xFF444444, 0xFF555555, 0xFF666666, 0xFF777777, 0xFF888888};

        int[] result = GlassBlur.boxBlur(input, 3, 3, 0, 3);

        assertArrayEquals(input, result);
        assertNotSame(input, result);
    }

    @Test
    public void singleBrightPixelSpreadsSymmetrically() {
        int[] input = filled(5 * 5, 0xFF000000);
        input[2 * 5 + 2] = 0xFFFFFFFF;

        // Two passes: a single box pass would give the centre and its neighbours the same value.
        int[] result = GlassBlur.boxBlur(input, 5, 5, 1, 2);

        int center = red(result[2 * 5 + 2]);
        int left = red(result[2 * 5 + 1]);
        int right = red(result[2 * 5 + 3]);
        int top = red(result[1 * 5 + 2]);
        int bottom = red(result[3 * 5 + 2]);
        int corner = red(result[0]);
        assertEquals(left, right);
        assertEquals(left, top);
        assertEquals(left, bottom);
        assertTrue("neighbours darker than center", left < center);
        assertTrue("neighbours brighter than far corner", left > corner);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMismatchedSize() {
        GlassBlur.boxBlur(new int[5], 2, 2, 1, 1);
    }

    @Test
    public void scrimBlackOverWhite() {
        assertArrayEquals(new int[]{0xFF4C4C4C}, GlassBlur.applyScrim(new int[]{0xFFFFFFFF}, 0xB3000000));
    }

    @Test
    public void scrimRoseOverBlack() {
        assertArrayEquals(new int[]{0xFF1B040C}, GlassBlur.applyScrim(new int[]{0xFF000000}, 0xA62A0612));
    }
}
