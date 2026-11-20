package com.construct.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class SceneTransformTest {
    @Test
    fun sceneFitsBothPortraitAndLandscapeViewports() {
        val portrait = SceneTransform(640, 480, Size(360f, 740f), 1f, Offset.Zero)
        val landscape = SceneTransform(640, 480, Size(900f, 400f), 1f, Offset.Zero)

        assertEquals(360f, portrait.sceneSize.width, 0.01f)
        assertEquals(270f, portrait.sceneSize.height, 0.01f)
        assertEquals(533.33f, landscape.sceneSize.width, 0.02f)
        assertEquals(400f, landscape.sceneSize.height, 0.01f)
    }

    @Test
    fun pinchKeepsThePointUnderTheFingersAtTheMovingFocus() {
        val viewport = Size(400f, 800f)
        val oldFocus = Offset(140f, 300f)
        val newFocus = Offset(170f, 330f)
        val oldPan = Offset(12f, -8f)
        val factor = 1.6f
        val initial = SceneTransform(640, 480, viewport, 1f, oldPan)
        val worldPoint = Offset(180f, 120f)
        val initialScreenPoint = initial.screenFromWorld(worldPoint)
        val nextPan = zoomPanAround(oldPan, viewport, oldFocus, newFocus, factor)
        val next = SceneTransform(640, 480, viewport, factor, nextPan)

        val expected = newFocus + (initialScreenPoint - oldFocus) * factor
        val actual = next.screenFromWorld(worldPoint)
        assertEquals(expected.x, actual.x, 0.01f)
        assertEquals(expected.y, actual.y, 0.01f)
    }
}
