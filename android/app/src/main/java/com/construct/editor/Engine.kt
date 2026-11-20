package com.construct.editor

import kotlin.math.sqrt

enum class BehaviorKind {
    EIGHT_DIRECTION,
    PLATFORM,
    BULLET,
    SOLID,
    SCROLL_TO,
    BOUND_TO_LAYOUT,
    PHYSICS
}

data class BehaviorSpec(
    val kind: BehaviorKind,
    val speed: Float = 180f,
    val gravity: Float = 900f,
    val jumpStrength: Float = 380f,
    val cameraZoom: Float = 1f
)

data class AnimationFrame(
    val assetId: Int,
    val durationSeconds: Float = 0.15f
)

data class LayerModel(
    val id: Int,
    val name: String,
    val layoutId: Int = 1,
    val visible: Boolean = true,
    val opacity: Float = 1f,
    val scrollX: Float = 1f,
    val scrollY: Float = 1f
)

enum class EventBlockKind { EVENT, GROUP, COMMENT, FUNCTION }

enum class ConditionKind { ALWAYS, START_OF_LAYOUT, KEY_DOWN, TOUCH, VARIABLE_COMPARE, OVERLAP, ELSE, UNSUPPORTED }

enum class CompareKind { EQUAL, NOT_EQUAL, GREATER, LESS }
enum class ConditionJoin { AND, OR }

data class EventCondition(
    val kind: ConditionKind = ConditionKind.ALWAYS,
    val key: String = "DIREITA",
    val variable: String = "pontuacao",
    val compare: CompareKind = CompareKind.GREATER,
    val value: Float = 0f,
    val firstObject: String = "",
    val secondObject: String = "",
    val valueExpression: String = "",
    val inverted: Boolean = false
)

enum class ActionKind {
    UNSUPPORTED,
    MOVE_BY,
    SET_POSITION,
    SET_VARIABLE,
    ADD_VARIABLE,
    DESTROY_INSTANCE,
    CREATE_INSTANCE,
    ROTATE,
    CALL_FUNCTION
}

data class EventAction(
    val kind: ActionKind = ActionKind.MOVE_BY,
    val targetName: String = "",
    val variable: String = "pontuacao",
    val value: Float = 0f,
    val valueY: Float = 0f,
    val functionName: String = "",
    val valueExpression: String = "",
    val valueYExpression: String = ""
)

data class EventRow(
    val id: Long,
    val eventSheetId: Int = 1,
    val blockKind: EventBlockKind = EventBlockKind.EVENT,
    val conditionJoin: ConditionJoin = ConditionJoin.AND,
    val name: String = "",
    val conditions: List<EventCondition> = listOf(EventCondition()),
    val actions: List<EventAction> = listOf(EventAction()),
    val children: List<EventRow> = emptyList(),
    val enabled: Boolean = true,
    val legacyCondition: String = "",
    val legacyAction: String = ""
)

data class RuntimeInput(
    val left: Boolean = false,
    val right: Boolean = false,
    val up: Boolean = false,
    val down: Boolean = false,
    val jumpPressed: Boolean = false,
    val touchPressed: Boolean = false,
    val touchX: Float = 0f,
    val touchY: Float = 0f
) {
    fun keyDown(key: String) = when (key.uppercase()) {
        "ESQUERDA", "LEFT", "A" -> left
        "DIREITA", "RIGHT", "D" -> right
        "CIMA", "UP", "W" -> up
        "BAIXO", "DOWN", "S" -> down
        "PULAR", "JUMP", "SPACE" -> jumpPressed
        else -> false
    }
}

data class RuntimeProject(
    val width: Int,
    val height: Int,
    val objects: List<LayoutObject>,
    val layers: List<LayerModel>,
    val events: List<EventRow>,
    val globalVariables: Map<String, Float>,
    val activeLayoutId: Int = 1,
    val activeEventSheetId: Int = 1
)

class GameRuntime(project: RuntimeProject) {
    val width = project.width.coerceAtLeast(1)
    val height = project.height.coerceAtLeast(1)
    val objects = project.objects
        .filter { it.visible && it.layoutId == project.activeLayoutId }
        .map { it.copy() }
        .toMutableList()
    private val templates = project.objects
        .filter { it.layoutId == project.activeLayoutId }
        .distinctBy { it.name }
        .associateBy { it.name }
    val layers = project.layers.filter { it.layoutId == project.activeLayoutId }.associateBy { it.id }
    val globalVariables = project.globalVariables.toMutableMap()
    private val events = project.events.filter { it.eventSheetId == project.activeEventSheetId }
    private val localVariables = mutableMapOf<String, Float>()
    private val functionLocalScopes = mutableMapOf<Long, MutableMap<String, Float>>()
    private var startPending = true
    private val previousInput = mutableSetOf<String>()
    private val verticalVelocity = mutableMapOf<Int, Float>()
    private val animationElapsed = mutableMapOf<Int, Float>()
    private val animationFrameIndex = mutableMapOf<Int, Int>()
    var cameraX = width / 2f
        private set
    var cameraY = height / 2f
        private set
    var cameraZoom = 1f
        private set
    var revision = 0
        private set
    var elapsedSeconds = 0f
        private set
    var lastError: String? = null
        private set
    var fps: Float = 0f
        private set
    val eventStates = mutableMapOf<Long, Boolean>()

    fun step(deltaSeconds: Float, input: RuntimeInput) {
        val dt = deltaSeconds.coerceIn(0f, 0.05f)
        if (dt <= 0f) return
        fps = if (fps == 0f) 1f / dt else fps * 0.9f + (1f / dt) * 0.1f
        eventStates.clear()
        val jumpInput = input.copy(jumpPressed = input.jumpPressed && "PULAR" !in previousInput)
        val eventInput = input.copy(touchPressed = input.touchPressed && "TOUCH" !in previousInput)
        elapsedSeconds += dt
        moveBehaviors(dt, jumpInput)
        runBlocks(events, eventInput, localVariables)
        advanceAnimations(dt)
        updateCamera()
        startPending = false
        if (input.left) previousInput.add("ESQUERDA") else previousInput.remove("ESQUERDA")
        if (input.right) previousInput.add("DIREITA") else previousInput.remove("DIREITA")
        if (input.up) previousInput.add("CIMA") else previousInput.remove("CIMA")
        if (input.down) previousInput.add("BAIXO") else previousInput.remove("BAIXO")
        if (input.jumpPressed) previousInput.add("PULAR") else previousInput.remove("PULAR")
        if (input.touchPressed) previousInput.add("TOUCH") else previousInput.remove("TOUCH")
        revision++
    }

    private fun advanceAnimations(dt: Float) {
        objects.forEach { instance ->
            if (instance.animationFrames.isEmpty()) return@forEach
            var elapsed = (animationElapsed[instance.id] ?: 0f) + dt
            var index = animationFrameIndex[instance.id] ?: 0
            var guard = 0
            while (guard++ < instance.animationFrames.size * 2) {
                val frame = instance.animationFrames[index]
                val duration = frame.durationSeconds.coerceAtLeast(0.02f)
                if (elapsed < duration) break
                elapsed -= duration
                index = (index + 1) % instance.animationFrames.size
            }
            animationElapsed[instance.id] = elapsed
            animationFrameIndex[instance.id] = index
            instance.assetId = instance.animationFrames[index].assetId
        }
    }

    private fun moveBehaviors(dt: Float, input: RuntimeInput) {
        objects.forEach { instance ->
            if (instance.behaviors.isEmpty()) return@forEach
            val previousX = instance.x
            val previousY = instance.y
            val movement = instance.behaviors.firstOrNull {
                it.kind == BehaviorKind.EIGHT_DIRECTION || it.kind == BehaviorKind.PLATFORM
            }
            if (movement != null) {
                val dx = (if (input.right) 1f else 0f) - (if (input.left) 1f else 0f)
                val dy = (if (input.down) 1f else 0f) - (if (input.up) 1f else 0f)
                if (movement.kind == BehaviorKind.PLATFORM) {
                    instance.x += dx * movement.speed * dt
                    if (input.jumpPressed && isOnGround(instance)) {
                        verticalVelocity[instance.id] = -movement.jumpStrength
                    }
                    val velocity = (verticalVelocity[instance.id] ?: 0f) + movement.gravity * dt
                    verticalVelocity[instance.id] = velocity
                    instance.y += velocity * dt
                    val floor = height - instance.h
                    if (instance.y >= floor) {
                        instance.y = floor
                        verticalVelocity[instance.id] = 0f
                    }
                } else {
                    val length = sqrt(dx * dx + dy * dy)
                    if (length > 0f) {
                        val scale = movement.speed * dt / maxOf(1f, length)
                        instance.x += dx * scale
                        instance.y += dy * scale
                    }
                }
            }
            if (hasBehavior(instance, BehaviorKind.BULLET)) {
                val bullet = instance.behaviors.first { it.kind == BehaviorKind.BULLET }
                val radians = instance.rotation * (Math.PI.toFloat() / 180f)
                instance.x += kotlin.math.cos(radians) * bullet.speed * dt
                instance.y += kotlin.math.sin(radians) * bullet.speed * dt
            }
            if (hasBehavior(instance, BehaviorKind.PHYSICS) && movement == null) {
                val physics = instance.behaviors.first { it.kind == BehaviorKind.PHYSICS }
                val velocity = (verticalVelocity[instance.id] ?: 0f) + physics.gravity * dt
                verticalVelocity[instance.id] = velocity
                instance.y += velocity * dt
                val floor = height - instance.h
                if (instance.y >= floor) {
                    instance.y = floor
                    verticalVelocity[instance.id] = 0f
                }
            }
            if (hasBehavior(instance, BehaviorKind.BOUND_TO_LAYOUT)) {
                instance.x = instance.x.coerceIn(0f, (width - instance.w).coerceAtLeast(0f))
                instance.y = instance.y.coerceIn(0f, (height - instance.h).coerceAtLeast(0f))
            }
            resolveSolidCollisions(instance, previousX, previousY)
        }
    }

    private fun isOnGround(instance: LayoutObject) =
        instance.y + instance.h >= height - 1f || objects.any { other ->
            other.id != instance.id && hasBehavior(other, BehaviorKind.SOLID) &&
                instance.y + instance.h >= other.y && instance.y + instance.h <= other.y + 8f &&
                overlapsX(instance, other)
        }

    private fun resolveSolidCollisions(instance: LayoutObject, previousX: Float, previousY: Float) {
        for (solid in objects) {
            if (solid.id == instance.id || !hasBehavior(solid, BehaviorKind.SOLID) || !intersects(instance, solid)) continue
            if (previousY + instance.h <= solid.y && instance.y + instance.h > solid.y) {
                instance.y = solid.y - instance.h
                verticalVelocity[instance.id] = 0f
            } else if (previousY >= solid.y + solid.h && instance.y < solid.y + solid.h) {
                instance.y = solid.y + solid.h
                verticalVelocity[instance.id] = 0f
            } else if (previousX + instance.w <= solid.x && instance.x + instance.w > solid.x) {
                instance.x = solid.x - instance.w
            } else if (previousX >= solid.x + solid.w && instance.x < solid.x + solid.w) {
                instance.x = solid.x + solid.w
            }
        }
    }

    private fun runBlocks(
        blocks: List<EventRow>,
        input: RuntimeInput,
        localScope: MutableMap<String, Float> = mutableMapOf()
    ) {
        var previousSiblingPassed = false
        for (event in blocks) {
            if (!event.enabled) {
                eventStates[event.id] = false
                continue
            }
            when (event.blockKind) {
                EventBlockKind.COMMENT -> {
                    eventStates[event.id] = false
                    continue
                }
                EventBlockKind.GROUP -> {
                    eventStates[event.id] = true
                    runBlocks(event.children, input, localScope)
                    continue
                }
                EventBlockKind.FUNCTION -> {
                    eventStates[event.id] = false
                    continue
                }
                EventBlockKind.EVENT -> Unit
            }
            val passed = when (event.conditionJoin) {
                ConditionJoin.AND -> event.conditions.all { evaluate(it, input, localScope, previousSiblingPassed) }
                ConditionJoin.OR -> event.conditions.any { evaluate(it, input, localScope, previousSiblingPassed) }
            }
            previousSiblingPassed = passed
            eventStates[event.id] = passed
            if (passed) {
                event.actions.forEach { execute(it, input, localScope) }
                runBlocks(event.children, input, localScope)
            }
        }
    }

    private fun evaluate(
        condition: EventCondition,
        input: RuntimeInput,
        localScope: Map<String, Float>,
        previousSiblingPassed: Boolean
    ): Boolean {
        val result = when (condition.kind) {
        ConditionKind.ALWAYS -> true
        ConditionKind.START_OF_LAYOUT -> startPending
        ConditionKind.KEY_DOWN -> input.keyDown(condition.key)
        ConditionKind.TOUCH -> input.touchPressed
        ConditionKind.ELSE -> !previousSiblingPassed
        ConditionKind.VARIABLE_COMPARE -> {
            val name = condition.variable.removePrefix("local:").removePrefix("global:")
            val actual = if (condition.variable.startsWith("local:")) {
                localScope[name] ?: 0f
            } else {
                globalVariables[name] ?: 0f
            }
            val comparison = evaluateNumber(condition.valueExpression, condition.value, localScope)
            when (condition.compare) {
                CompareKind.EQUAL -> actual == comparison
                CompareKind.NOT_EQUAL -> actual != comparison
                CompareKind.GREATER -> actual > comparison
                CompareKind.LESS -> actual < comparison
            }
        }
        ConditionKind.OVERLAP -> {
            objects.any { first ->
                first.name == condition.firstObject && objects.any { second ->
                    second.name == condition.secondObject && first.id != second.id && intersects(first, second)
                }
            }
        }
        ConditionKind.UNSUPPORTED -> false
        }
        return if (condition.inverted) !result else result
    }

    private fun execute(action: EventAction, input: RuntimeInput, localScope: MutableMap<String, Float>) {
        if (action.kind == ActionKind.CALL_FUNCTION) {
            val function = findFunction(events, action.functionName)
            if (function != null) {
                runBlocks(function.children, input, functionLocalScopes.getOrPut(function.id) { mutableMapOf() })
            }
            else lastError = "Função '${action.functionName}' não encontrada."
            return
        }
        val targets = objects.filter { action.targetName.isBlank() || it.name == action.targetName }
        val value = evaluateNumber(action.valueExpression, action.value, localScope)
        val valueY = evaluateNumber(action.valueYExpression, action.valueY, localScope)
        when (action.kind) {
            ActionKind.SET_VARIABLE -> {
                if (action.variable.startsWith("local:")) {
                    localScope[action.variable.removePrefix("local:")] = value
                } else globalVariables[action.variable.removePrefix("global:")] = value
            }
            ActionKind.ADD_VARIABLE -> {
                if (action.variable.startsWith("local:")) {
                    val name = action.variable.removePrefix("local:")
                    localScope[name] = (localScope[name] ?: 0f) + value
                } else {
                    val name = action.variable.removePrefix("global:")
                    globalVariables[name] = (globalVariables[name] ?: 0f) + value
                }
            }
            ActionKind.DESTROY_INSTANCE ->
                objects.removeAll { action.targetName.isBlank() || it.name == action.targetName }
            ActionKind.CREATE_INSTANCE -> {
                val template = templates[action.targetName]
                if (template == null) {
                    lastError = "Não há instância-modelo '${action.targetName}' nesta cena."
                } else {
                    val nextId = (objects.maxOfOrNull { it.id } ?: 0) + 1
                    objects += template.copy(
                        id = nextId,
                        x = value,
                        y = valueY,
                        behaviors = template.behaviors.toList(),
                        animationFrames = template.animationFrames.toList()
                    )
                }
            }
            ActionKind.MOVE_BY ->
                targets.forEach { it.x += value; it.y += valueY }
            ActionKind.SET_POSITION ->
                targets.forEach { it.x = value; it.y = valueY }
            ActionKind.ROTATE ->
                targets.forEach { it.rotation = value }
            ActionKind.CALL_FUNCTION, ActionKind.UNSUPPORTED -> Unit
        }
    }

    private fun updateCamera() {
        val target = objects.firstOrNull { hasBehavior(it, BehaviorKind.SCROLL_TO) } ?: return
        val camera = target.behaviors.first { it.kind == BehaviorKind.SCROLL_TO }
        cameraZoom = camera.cameraZoom.coerceIn(0.25f, 4f)
        val halfWidth = width / (2f * cameraZoom)
        val halfHeight = height / (2f * cameraZoom)
        cameraX = (target.x + target.w / 2f).coerceIn(
            minOf(halfWidth, width / 2f), maxOf(width - halfWidth, width / 2f)
        )
        cameraY = (target.y + target.h / 2f).coerceIn(
            minOf(halfHeight, height / 2f), maxOf(height - halfHeight, height / 2f)
        )
    }

    private fun findFunction(blocks: List<EventRow>, name: String): EventRow? {
        blocks.forEach { block ->
            if (block.blockKind == EventBlockKind.FUNCTION && block.name == name) return block
            findFunction(block.children, name)?.let { return it }
        }
        return null
    }

    private fun evaluateNumber(expression: String, fallback: Float, scope: Map<String, Float>): Float {
        if (expression.isBlank()) return fallback
        val variables = globalVariables + scope
        return try {
            ExpressionEvaluator(expression, variables).evaluate()
        } catch (error: IllegalArgumentException) {
            lastError = error.message
            fallback
        }
    }

    private fun hasBehavior(instance: LayoutObject, kind: BehaviorKind) =
        instance.behaviors.any { it.kind == kind }

    private fun overlapsX(a: LayoutObject, b: LayoutObject) =
        a.x < b.x + b.w && a.x + a.w > b.x

    private fun intersects(a: LayoutObject, b: LayoutObject) =
        overlapsX(a, b) && a.y < b.y + b.h && a.y + a.h > b.y
}

private class ExpressionEvaluator(
    private val source: String,
    private val variables: Map<String, Float>
) {
    private var index = 0

    fun evaluate(): Float {
        val value = expression()
        whitespace()
        require(index == source.length) { "Expressão inválida próximo de '${source.drop(index)}'." }
        require(value.isFinite()) { "A expressão produziu um valor inválido." }
        return value
    }

    private fun expression(): Float {
        var value = term()
        while (true) {
            whitespace()
            value = when {
                consume('+') -> value + term()
                consume('-') -> value - term()
                else -> return value
            }
        }
    }

    private fun term(): Float {
        var value = factor()
        while (true) {
            whitespace()
            value = when {
                consume('*') -> value * factor()
                consume('/') -> {
                    val divisor = factor()
                    require(divisor != 0f) { "Divisão por zero na expressão." }
                    value / divisor
                }
                else -> return value
            }
        }
    }

    private fun factor(): Float {
        whitespace()
        if (consume('-')) return -factor()
        if (consume('+')) return factor()
        if (consume('(')) {
            val result = expression()
            whitespace()
            require(consume(')')) { "Parêntese sem fechamento na expressão." }
            return result
        }
        if (index < source.length && (source[index].isDigit() || source[index] == '.')) return number()
        val start = index
        while (index < source.length && (source[index].isLetterOrDigit() || source[index] == '_' || source[index] == '.')) {
            index++
        }
        require(index > start) { "Esperado número ou variável na expressão." }
        val name = source.substring(start, index)
        return variables[name] ?: variables[name.removePrefix("global:")]
            ?: variables[name.removePrefix("local:")]
            ?: throw IllegalArgumentException("Variável '$name' não encontrada na expressão.")
    }

    private fun number(): Float {
        val start = index
        while (index < source.length && (source[index].isDigit() || source[index] == '.')) index++
        return source.substring(start, index).toFloatOrNull()
            ?: throw IllegalArgumentException("Número inválido na expressão.")
    }

    private fun consume(char: Char): Boolean {
        if (index < source.length && source[index] == char) {
            index++
            return true
        }
        return false
    }

    private fun whitespace() {
        while (index < source.length && source[index].isWhitespace()) index++
    }
}

internal fun intersects(a: LayoutObject, b: LayoutObject) =
    a.x < b.x + b.w && a.x + a.w > b.x &&
        a.y < b.y + b.h && a.y + a.h > b.y
