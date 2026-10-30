package com.littletutor.app.ui.tutor

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoBitmapsTest {
    @Test
    fun sampleSizeKeepsLongestSideAtOrAboveTarget() {
        assertEquals(1, PhotoBitmaps.sampleSizeFor(1000, 800, 2048))
        assertEquals(1, PhotoBitmaps.sampleSizeFor(4032, 3024, 2048))
        assertEquals(2, PhotoBitmaps.sampleSizeFor(8000, 6000, 2048))
        assertEquals(8, PhotoBitmaps.sampleSizeFor(3024, 4032, 320))
    }
}
