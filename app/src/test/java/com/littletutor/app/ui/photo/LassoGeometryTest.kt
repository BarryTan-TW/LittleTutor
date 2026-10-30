package com.littletutor.app.ui.photo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LassoGeometryTest {
    // Drawn clockwise, so it has edges going both up and down.
    private val square = listOf(Offset(0f, 0f), Offset(100f, 0f), Offset(100f, 100f), Offset(0f, 100f))

    @Test
    fun pointInsideSquareIsDetectedForEitherWindingDirection() {
        assertTrue(isPointInPolygon(Offset(50f, 50f), square))
        assertTrue(isPointInPolygon(Offset(50f, 50f), square.reversed()))
        assertFalse(isPointInPolygon(Offset(150f, 50f), square))
    }

    @Test
    fun tokenPartiallyCoveredByLassoCountsWhenTwoCornersAreInside() {
        assertTrue(isTokenHitByLasso(Rect(80f, 10f, 140f, 30f), square))
        assertFalse(isTokenHitByLasso(Rect(95f, 90f, 160f, 160f), square))
    }
}
