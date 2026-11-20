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
    fun keyDownConditionRemainsTrueWhileKeyIsHeld() {
        val event = EventRow(
            id = 1,
            conditions = listOf(EventCondition(kind = ConditionKind.KEY_DOWN, key = "PULAR")),
            actions = listOf(EventAction(kind = ActionKind.ADD_VARIABLE, variable = "score", value = 1f))
        )
        val runtime = runtime(events = listOf(event))

        runtime.step(0.05f, RuntimeInput(jumpPressed = true))
        runtime.step(0.05f, RuntimeInput(jumpPressed = true))

        assertEquals(2f, runtime.globalVariables["score"] ?: 0f, 0.001f)
    }

    @Test
    fun localVariablesPersistAcrossRuntimeFrames() {
        val increment = EventRow(
            id = 1,
            actions = listOf(EventAction(
                kind = ActionKind.ADD_VARIABLE, variable = "local:counter", value = 1f
            ))
        )
        val check = EventRow(
            id = 2,
            conditions = listOf(EventCondition(
                kind = ConditionKind.VARIABLE_COMPARE,
                variable = "local:counter",
                compare = CompareKind.GREATER,
                value = 1f
            )),
            actions = listOf(EventAction(
                kind = ActionKind.SET_VARIABLE, variable = "ready", value = 1f
            ))
        )
        val runtime = runtime(events = listOf(increment, check))

        runtime.step(0.05f, RuntimeInput())
        runtime.step(0.05f, RuntimeInput())

        assertEquals(1f, runtime.globalVariables["ready"] ?: 0f, 0.001f)
    }

    @Test
    fun instanceVariablesCanBeEditedAndUsedByEventConditions() {
        val player = LayoutObject(
            id = 1, name = "Player", plugin = "Sprite",
            x = 10f, y = 10f, w = 20f, h = 20f
        )
        val enemy = LayoutObject(
            id = 2, name = "Enemy", plugin = "Sprite",
            x = 50f, y = 10f, w = 20f, h = 20f
        )
        val initialize = EventRow(
            id = 1,
            actions = listOf(EventAction(
                kind = ActionKind.SET_VARIABLE,
                targetName = "Player",
                variable = "instance:health",
                value = 5f
            ))
        )
        val check = EventRow(
            id = 2,
            conditions = listOf(EventCondition(
                kind = ConditionKind.VARIABLE_COMPARE,
                variable = "instance:health",
                firstObject = "Player",
                compare = CompareKind.GREATER,
                value = 2f
            )),
            actions = listOf(EventAction(
                kind = ActionKind.SET_VARIABLE,
                variable = "playerAlive",
                value = 1f
            ))
        )
        val runtime = runtime(objects = listOf(player, enemy), events = listOf(initialize, check))

        runtime.step(0.05f, RuntimeInput())

        assertEquals(5f, runtime.objects.first { it.name == "Player" }.instanceVariables["health"] ?: 0f, 0f)
        assertTrue("health" !in runtime.objects.first { it.name == "Enemy" }.instanceVariables)
        assertEquals(1f, runtime.globalVariables["playerAlive"] ?: 0f, 0f)
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
    fun intervalConditionTriggersOnceAtEachElapsedBoundary() {
        val event = EventRow(
            id = 1,
            conditions = listOf(EventCondition(
                kind = ConditionKind.EVERY_X_SECONDS,
                value = 0.1f
            )),
            actions = listOf(EventAction(
                kind = ActionKind.ADD_VARIABLE,
                variable = "ticks",
                value = 1f
            ))
        )
        val runtime = runtime(events = listOf(event))

        repeat(4) { runtime.step(0.05f, RuntimeInput()) }

        assertEquals(2f, runtime.globalVariables["ticks"] ?: 0f, 0.001f)
    }

    @Test
    fun repeatBlockExecutesItsChildrenTheConfiguredNumberOfTimes() {
        val repeat = EventRow(
            id = 1,
            blockKind = EventBlockKind.REPEAT,
            repeatCount = 3,
            children = listOf(EventRow(
                id = 2,
                actions = listOf(EventAction(
                    kind = ActionKind.ADD_VARIABLE,
                    variable = "count",
                    value = 1f
                ))
            ))
        )
        val runtime = runtime(events = listOf(repeat))

        runtime.step(0.05f, RuntimeInput())

        assertEquals(3f, runtime.globalVariables["count"] ?: 0f, 0.001f)
    }

    @Test
    fun audioActionQueuesTheSelectedResourceForRuntimePlayback() {
        val event = EventRow(
            id = 1,
            conditions = listOf(EventCondition(kind = ConditionKind.START_OF_LAYOUT)),
            actions = listOf(EventAction(kind = ActionKind.PLAY_AUDIO, audioAssetId = 12))
        )
        val runtime = runtime(events = listOf(event))

        runtime.step(0.05f, RuntimeInput())
        assertEquals(listOf(12), runtime.audioRequests.toList())

        runtime.step(0.05f, RuntimeInput())
        assertTrue(runtime.audioRequests.isEmpty())
    }

    @Test
    fun namedTimerCanStartTriggerAConditionAndPause() {
        val start = EventRow(
            id = 1,
            conditions = listOf(EventCondition(kind = ConditionKind.START_OF_LAYOUT)),
            actions = listOf(EventAction(kind = ActionKind.START_TIMER, timerName = "delay"))
        )
        val elapsed = EventRow(
            id = 2,
            conditions = listOf(EventCondition(
                kind = ConditionKind.TIMER_COMPARE,
                timerName = "delay",
                compare = CompareKind.GREATER,
                value = 0.09f
            )),
            actions = listOf(EventAction(kind = ActionKind.SET_VARIABLE, variable = "done", value = 1f))
        )
        val pause = EventRow(
            id = 3,
            conditions = listOf(EventCondition(
                kind = ConditionKind.VARIABLE_COMPARE,
                variable = "done",
                compare = CompareKind.GREATER,
                value = 0f
            )),
            actions = listOf(EventAction(kind = ActionKind.STOP_TIMER, timerName = "delay"))
        )
        val runtime = runtime(events = listOf(start, elapsed, pause))

        repeat(3) { runtime.step(0.05f, RuntimeInput()) }
        val stoppedAt = runtime.timers["delay"] ?: 0f
        runtime.step(0.05f, RuntimeInput())

        assertEquals(1f, runtime.globalVariables["done"] ?: 0f, 0f)
        assertEquals(stoppedAt, runtime.timers["delay"] ?: 0f, 0.001f)
    }

    @Test
    fun emptyProjectDoesNotCreateRuntimeObjectsAndTouchIsEventDriven() {
        val touchEvent = EventRow(
            id = 1,
            conditions = listOf(EventCondition(kind = ConditionKind.TOUCH)),
            actions = listOf(EventAction(kind = ActionKind.ADD_VARIABLE, variable = "tapped", value = 1f))
        )
        val runtime = runtime(events = listOf(touchEvent))

        runtime.step(0.05f, RuntimeInput())
        assertTrue(runtime.objects.isEmpty())
        assertTrue("tapped" !in runtime.globalVariables)

        runtime.step(0.05f, RuntimeInput(touchPressed = true))
        runtime.step(0.05f, RuntimeInput(touchPressed = true))

        assertEquals(1f, runtime.globalVariables["tapped"] ?: 0f, 0.001f)
    }

    @Test
    fun touchOnObjectConditionUsesTheTouchedWorldPosition() {
        val target = LayoutObject(
            id = 1, name = "Botao", plugin = "Sprite",
            x = 100f, y = 70f, w = 20f, h = 20f
        )
        val event = EventRow(
            id = 1,
            conditions = listOf(EventCondition(
                kind = ConditionKind.TOUCH_ON_OBJECT,
                firstObject = "Botao"
            )),
            actions = listOf(EventAction(
                kind = ActionKind.SET_VARIABLE,
                variable = "clicked",
                value = 1f
            ))
        )
        val runtime = runtime(objects = listOf(target), events = listOf(event))

        runtime.step(0.05f, RuntimeInput(touchPressed = true, touchX = 110f, touchY = 80f))
        assertEquals(1f, runtime.globalVariables["clicked"] ?: 0f, 0.001f)

        runtime.globalVariables["clicked"] = 0f
        runtime.step(0.05f, RuntimeInput())
        runtime.step(0.05f, RuntimeInput(touchPressed = true, touchX = 20f, touchY = 20f))
        assertEquals(0f, runtime.globalVariables["clicked"] ?: 0f, 0.001f)
    }

    @Test
    fun cameraFollowsTargetWithConfiguredZoomAndClampedCenter() {
        val target = LayoutObject(
            id = 1, name = "Player", plugin = "Sprite",
            x = 300f, y = 110f, w = 20f, h = 20f,
            behaviors = listOf(BehaviorSpec(BehaviorKind.SCROLL_TO, cameraZoom = 2f))
        )
        val runtime = runtime(objects = listOf(target))

        runtime.step(0.05f, RuntimeInput())

        assertEquals(2f, runtime.cameraZoom, 0.001f)
        assertEquals(240f, runtime.cameraX, 0.001f)
        assertEquals(120f, runtime.cameraY, 0.001f)
    }

    @Test
    fun elseEventRunsOnlyWhenPreviousSiblingDidNotPass() {
        val firstEvent = EventRow(
            id = 1,
            conditions = listOf(EventCondition(
                kind = ConditionKind.VARIABLE_COMPARE,
                variable = "score",
                compare = CompareKind.GREATER,
                value = 0f
            ))
        )
        val elseEvent = EventRow(
            id = 2,
            conditions = listOf(EventCondition(kind = ConditionKind.ELSE)),
            actions = listOf(EventAction(
                kind = ActionKind.SET_VARIABLE, variable = "elseRan", value = 1f
            ))
        )
        val runtime = runtime(events = listOf(firstEvent, elseEvent))

        runtime.step(0.05f, RuntimeInput())

        assertEquals(1f, runtime.globalVariables["elseRan"] ?: 0f, 0.001f)
    }

    @Test
    fun createInstanceClonesConfiguredTemplateAtExpressionPosition() {
        val template = LayoutObject(
            id = 1, name = "Moeda", plugin = "Sprite",
            x = 10f, y = 10f, w = 16f, h = 16f, assetId = 5
        )
        val event = EventRow(
            id = 1,
            actions = listOf(EventAction(
                kind = ActionKind.CREATE_INSTANCE,
                targetName = "Moeda",
                valueExpression = "40 + 2",
                valueYExpression = "90"
            ))
        )
        val runtime = runtime(objects = listOf(template), events = listOf(event))

        runtime.step(0.05f, RuntimeInput())

        assertEquals(2, runtime.objects.size)
        assertEquals(42f, runtime.objects[1].x, 0.001f)
        assertEquals(90f, runtime.objects[1].y, 0.001f)
        assertEquals(5, runtime.objects[1].assetId)
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

    @Test
    fun runtimeOnlyLoadsObjectsAndEventsFromActiveLayout() {
        val firstLayoutObject = LayoutObject(
            id = 1, name = "Cena 1", plugin = "Sprite",
            x = 10f, y = 10f, w = 20f, h = 20f, layoutId = 1
        )
        val secondLayoutObject = LayoutObject(
            id = 2, name = "Cena 2", plugin = "Sprite",
            x = 30f, y = 30f, w = 20f, h = 20f, layoutId = 2
        ).apply { layerId = 2 }
        val firstSheetEvent = EventRow(
            id = 1, eventSheetId = 1,
            actions = listOf(EventAction(kind = ActionKind.SET_VARIABLE, variable = "sheet", value = 1f))
        )
        val secondSheetEvent = EventRow(
            id = 2, eventSheetId = 2,
            actions = listOf(EventAction(kind = ActionKind.SET_VARIABLE, variable = "sheet", value = 2f))
        )
        val runtime = GameRuntime(
            RuntimeProject(
                width = 320,
                height = 240,
                objects = listOf(firstLayoutObject, secondLayoutObject),
                layers = listOf(
                    LayerModel(id = 1, name = "Primeira", layoutId = 1),
                    LayerModel(id = 2, name = "Segunda", layoutId = 2)
                ),
                events = listOf(firstSheetEvent, secondSheetEvent),
                globalVariables = emptyMap(),
                activeLayoutId = 2,
                activeEventSheetId = 2
            )
        )

        runtime.step(0.05f, RuntimeInput())

        assertEquals(1, runtime.objects.size)
        assertEquals("Cena 2", runtime.objects.single().name)
        assertEquals(setOf(2), runtime.layers.keys)
        assertEquals(2f, runtime.globalVariables["sheet"] ?: 0f, 0.001f)
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
