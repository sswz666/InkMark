package com.inkmark.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GeomTest {

    private static final float EPS = 0.01f;

    @Test
    public void pointOnSegmentHasZeroDistance() {
        assertEquals(0f, Geom.distToSegment(5f, 5f, 0f, 5f, 10f, 5f), EPS);
    }

    @Test
    public void perpendicularDistanceIsMeasured() {
        assertEquals(3f, Geom.distToSegment(5f, 2f, 0f, 5f, 10f, 5f), EPS);
    }

    @Test
    public void beyondEndpointUsesEndpointDistance() {
        // 落在 (10,5) 之外，距离应为到端点的 5px
        assertEquals(5f, Geom.distToSegment(15f, 5f, 0f, 5f, 10f, 5f), EPS);
    }

    @Test
    public void degenerateSegmentStillWorks() {
        assertEquals(7.0711f, Geom.distToSegment(5f, 5f, 0f, 0f, 0f, 0f), 0.001f);
    }

    @Test
    public void insideRectReturnsDistanceToNearestEdge() {
        assertEquals(2f, Geom.distToRectEdge(12f, 12f, 10f, 10f, 50f, 40f), EPS);
        assertEquals(4f, Geom.distToRectEdge(46f, 20f, 10f, 10f, 50f, 40f), EPS);
    }

    @Test
    public void outsideRectReturnsDistanceToCorner() {
        float d = Geom.distToRectEdge(25f, 25f, 10f, 10f, 20f, 20f);
        assertEquals(7.0711f, d, 0.001f);
    }

    @Test
    public void rectWithReversedCornersIsNormalised() {
        assertEquals(2f, Geom.distToRectEdge(12f, 12f, 50f, 40f, 10f, 10f), EPS);
    }

    @Test
    public void ellipseOutlineDistance() {
        assertEquals(0f, Geom.distToEllipseOutline(100f, 50f, 50f, 50f, 50f, 30f), 0.5f);
        assertEquals(30f, Geom.distToEllipseOutline(50f, 50f, 50f, 50f, 50f, 30f), 0.5f);
        assertEquals(0f, Geom.distToEllipseOutline(50f, 20f, 50f, 50f, 50f, 30f), 0.5f);
    }

    @Test
    public void emptyEllipseNeverHits() {
        assertTrue(Geom.distToEllipseOutline(0f, 0f, 0f, 0f, 0f, 0f) > 1f);
    }

    @Test
    public void clampKeepsValueInRange() {
        assertEquals(3f, Geom.clamp(5f, 1f, 3f), EPS);
        assertEquals(1f, Geom.clamp(-5f, 1f, 3f), EPS);
        assertEquals(2f, Geom.clamp(2f, 1f, 3f), EPS);
    }
}
