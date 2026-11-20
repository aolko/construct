package com.construct.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorStateTest {
    @Test
    fun newProjectStartsWithOneEmptyScene() {
        val state = EditorState()

        assertTrue(state.objects.isEmpty())
        assertEquals(1, state.layouts.size)
        assertEquals(1, state.layers.size)
        assertEquals(0, state.selectedId)
    }

    @Test
    fun creatingANewProjectClearsProjectContentAndRestoresBaseScene() {
        val state = EditorState().apply {
            objects = listOf(LayoutObject(1, "Sprite", "Sprite", 0f, 0f, 32f, 32f))
            assets = listOf(ImageAsset(1, "image.png", "image/png", ""))
            events = listOf(EventRow(1))
            layouts = listOf(LayoutInfo(2, "Outro", 800, 600, 2))
        }

        state.createNewProject()

        assertTrue(state.objects.isEmpty())
        assertTrue(state.assets.isEmpty())
        assertTrue(state.events.isEmpty())
        assertEquals("Cena 1", state.layouts.single().name)
        assertEquals(1, state.activeLayoutId)
    }

    @Test
    fun duplicatingAnEventTreeAssignsFreshIdsAndPreservesContents() {
        val original = EventRow(
            id = 4,
            actions = listOf(EventAction(kind = ActionKind.SET_VARIABLE, variable = "score", value = 5f)),
            children = listOf(EventRow(id = 5, name = "child"))
        )
        var nextId = 10L

        val copy = duplicateEventTree(original) { nextId++ }

        assertEquals(10L, copy.id)
        assertEquals(11L, copy.children.single().id)
        assertEquals("score", copy.actions.single().variable)
        assertEquals(5f, copy.actions.single().value, 0f)
    }
}
