package com.construct.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTest {
    @Test
    fun directionalBehaviorMovesInstanceFromTouchInput() {
        val player = LayoutObject(
            id = 1, name = "Player", plugin = "Sprite",
            x = 10f, y = 10f, w = 20f, h = 20f,
            behaviors = listOf(BehaviorSpec(BehaviorKind.EIGHT_DIRECTION, speed = 100f))
        )
        val runtime = runtime(objects = listOf(player))

        runtime.step(0.05f, RuntimeInput(right = true))

        assertEquals(15f, runtime.objects.single().x, 0.001f)
    }

    @Test
    fun eventConditionsAndExpressionsUpdateGlobalVariables() {
        val scoreEvent = EventRow(
            id = 1,
            conditions = listOf(EventCondition(kind = ConditionKind.KEY_DOWN, key = "PULAR")),
            actions = listOf(
                EventAction(
                    kind = ActionKind.ADD_VARIABLE,
                    variable = "score",
                    valueExpression = "2 + 3 * 4"
                )
            )
        )
        val runtime = runtime(events = listOf(scoreEvent))

        runtime.step(0.05f, RuntimeInput(jumpPressed = true))

        assertEquals(14f, runtime.globalVariables["score"] ?: 0f, 0.001f)
    }

    @Test
    fun startOfLayoutRunsOnlyOnFirstRuntimeStep() {
        val initEvent = EventRow(
            id = 1,
            conditions = listOf(EventCondition(kind = ConditionKind.START_OF_LAYOUT)),
            actions = listOf(EventAction(kind = ActionKind.SET_VARIABLE, variable = "initialized", value = 1f))
        )
        val runtime = runtime(events = listOf(initEvent))

        runtime.step(0.05f, RuntimeInput())
        runtime.globalVariables["initialized"] = 0f
        runtime.step(0.05f, RuntimeInput())

        assertTrue(runtime.globalVariables.containsKey("initialized"))
        assertEquals(0f, runtime.globalVariables["initialized"] ?: -1f, 0f)
    }

    @Test
    fun calledFunctionExecutesItsChildEvents() {
        val function = EventRow(
            id = 2,
            blockKind = EventBlockKind.FUNCTION,
            name = "Aumentar",
            children = listOf(
                EventRow(
                    id = 3,
                    actions = listOf(EventAction(
                        kind = ActionKind.ADD_VARIABLE, variable = "score", value = 9f
                    ))
                )
            )
        )
        val caller = EventRow(
            id = 1,
            actions = listOf(EventAction(kind = ActionKind.CALL_FUNCTION, functionName = "Aumentar"))
        )
        val runtime = runtime(events = listOf(caller, function))

        runtime.step(0.05f, RuntimeInput())

        assertEquals(9f, runtime.globalVariables["score"] ?: 0f, 0.001f)
    }

    private fun runtime(
        objects: List<LayoutObject> = emptyList(),
        events: List<EventRow> = emptyList()
    ) = GameRuntime(
        RuntimeProject(
            width = 320,
            height = 240,
            objects = objects,
            layers = listOf(LayerModel(id = 1, name = "Principal")),
            events = events,
            globalVariables = emptyMap()
        )
    )
}
