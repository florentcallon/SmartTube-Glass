package com.liskovsoft.smartyoutubetv2.glass;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GlassFadingGridViewTest {
    @Test
    public void topFadesOnlyWhenItemsAreHiddenAbove() {
        // first item fully below the top padding edge: nothing hidden
        assertEquals(0f, GlassFadingGridView.topStrength(0, 100, 88), 0f);
        // first item starts above the edge
        assertEquals(1f, GlassFadingGridView.topStrength(0, 60, 88), 0f);
        // list scrolled: earlier items are not even laid out
        assertEquals(1f, GlassFadingGridView.topStrength(3, 120, 88), 0f);
    }

    @Test
    public void bottomFadesOnlyWhenItemsAreHiddenBelow() {
        // last item laid out and ending above the bottom padding edge
        assertEquals(0f, GlassFadingGridView.bottomStrength(9, 10, 600, 650), 0f);
        // last item crosses the edge
        assertEquals(1f, GlassFadingGridView.bottomStrength(9, 10, 680, 650), 0f);
        // more items after the last laid out one
        assertEquals(1f, GlassFadingGridView.bottomStrength(6, 10, 600, 650), 0f);
    }
}
