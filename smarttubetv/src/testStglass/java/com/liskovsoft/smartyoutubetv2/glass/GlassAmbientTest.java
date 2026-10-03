package com.liskovsoft.smartyoutubetv2.glass;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class GlassAmbientTest {
    private static class FakeClock implements GlassAmbient.Clock {
        long now;

        @Override
        public long now() {
            return now;
        }
    }

    @Test
    public void zeroSizeIsUnusable() {
        assertFalse(GlassAmbient.isUsableSize(0, 54));
        assertFalse(GlassAmbient.isUsableSize(96, 0));
        assertFalse(GlassAmbient.isUsableSize(-1, 54));
        assertTrue(GlassAmbient.isUsableSize(96, 54));
    }

    @Test
    public void renderAppliesBlurThenScrim() {
        int[] white = new int[GlassAmbient.SAMPLE_WIDTH * GlassAmbient.SAMPLE_HEIGHT];
        Arrays.fill(white, 0xFFFFFFFF);
        int[] expected = new int[white.length];
        Arrays.fill(expected, 0xFF4C4C4C);

        assertArrayEquals(expected, GlassAmbient.render(white, 0xB3000000));
    }

    @Test
    public void debouncerWaitsForQuiet() {
        FakeClock clock = new FakeClock();
        GlassAmbient.Debouncer debouncer = new GlassAmbient.Debouncer(clock, GlassAmbient.DEBOUNCE_MS);

        clock.now = 0;
        debouncer.request("a");
        clock.now = 200;
        debouncer.request("b");
        clock.now = 450;
        assertNull(debouncer.poll());
        clock.now = 500;
        assertEquals("b", debouncer.poll());
        clock.now = 600;
        assertNull(debouncer.poll());
    }

    @Test
    public void debouncerSingleRequest() {
        FakeClock clock = new FakeClock();
        GlassAmbient.Debouncer debouncer = new GlassAmbient.Debouncer(clock, GlassAmbient.DEBOUNCE_MS);

        clock.now = 0;
        debouncer.request("x");
        clock.now = 300;
        assertEquals("x", debouncer.poll());
    }
}
