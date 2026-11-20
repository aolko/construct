package com.construct.editor

import android.net.Uri
import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.ByteArrayOutputStream
import android.util.Base64
import kotlin.math.min
import kotlin.math.roundToInt

object CoreNative {
    init { System.loadLibrary("construct_core") }
    external fun inspectCap(path: String): String
    external fun openCap(path: String, targetPath: String): Boolean
}

data class LayoutObject(
    var id: Int,
    var name: String,
    var plugin: String,
    var x: Float,
    var y: Float,
    var w: Float,
    var h: Float,
    var layoutId: Int = 1,
    var text: String = "",
    var rotation: Float = 0f,
    var layerId: Int = 1,
    var visible: Boolean = true,
    var assetId: Int = 0,
    var behaviors: List<BehaviorSpec> = emptyList(),
    var animationName: String = "Parado",
    var animationFrames: List<AnimationFrame> = emptyList(),
    var frameDuration: Float = 0.15f,
    var instanceVariables: Map<String, Float> = emptyMap()
)

data class LayoutInfo(
    val id: Int,
    val name: String,
    val width: Int = 640,
    val height: Int = 480,
    val eventSheetId: Int = 1
)

data class ImageAsset(
    val id: Int,
    var name: String,
    val mimeType: String,
    val base64: String
)

private data class ProjectSnapshot(
    val name: String,
    val width: Int,
    val height: Int,
    val layouts: List<LayoutInfo>,
    val startLayoutId: Int,
    val objectTypes: List<String>,
    val layers: List<LayerModel>,
    val objects: List<LayoutObject>,
    val events: List<EventRow>,
    val globalVariables: Map<String, Float>,
    val assets: List<ImageAsset>
)

class EditorState {
    var projectName by mutableStateOf("Projeto sem título")
    var width by mutableStateOf(640)
    var height by mutableStateOf(480)
    var objectTypes by mutableStateOf(listOf("Imagem", "Texto"))
    var layouts by mutableStateOf(listOf(LayoutInfo(1, "Cena 1", 640, 480, 1)))
    var activeLayoutId by mutableIntStateOf(1)
    var startLayoutId by mutableIntStateOf(1)
    var objects by mutableStateOf(emptyList<LayoutObject>())
    var events by mutableStateOf(emptyList<EventRow>())
    var layers by mutableStateOf(listOf(LayerModel(1, "Principal")))
    var globalVariables by mutableStateOf(mapOf("pontuacao" to 0f))
    var assets by mutableStateOf(emptyList<ImageAsset>())
    var animationTargetId by mutableIntStateOf(0)
    var nextEventId by mutableLongStateOf(1L)
    var selectedId by mutableStateOf(0)
    var modified by mutableStateOf(false)
    var notice by mutableStateOf("Novo projeto")
    var objectRevision by mutableIntStateOf(0)

    fun activeLayerId() = layers.firstOrNull { it.layoutId == activeLayoutId }?.id ?: 1

    fun createNewProject() {
        projectName = "Projeto sem título"
        width = 640
        height = 480
        objectTypes = listOf("Imagem", "Texto")
        layouts = listOf(LayoutInfo(1, "Cena 1", width, height, 1))
        activeLayoutId = 1
        startLayoutId = 1
        objects = emptyList()
        events = emptyList()
        layers = listOf(LayerModel(1, "Principal", layoutId = 1))
        globalVariables = emptyMap()
        assets = emptyList()
        selectedId = 0
        animationTargetId = 0
        nextEventId = 1L
        modified = false
        notice = "Novo projeto"
        objectRevision++
    }

    fun markObjectsChanged() {
        objectRevision++
        if (!modified) modified = true
    }

    fun markProjectChanged() {
        if (!modified) modified = true
    }
}

class MainActivity : ComponentActivity() {
    private val state = EditorState()

    private val openCap = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { openUri(it) }
    }
    private val saveProject = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let { saveUri(it) }
    }
    private val importImage = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) importImages(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConstructTheme {
                ConstructApp(
                    state = state,
                    onOpen = { openCap.launch(arrayOf("*/*")) },
                    onSave = { saveProject.launch("${state.projectName}.ccproj") },
                    onImport = { importImage.launch(arrayOf("image/*")) }
                )
            }
        }
    }

    private fun importImages(uris: List<Uri>) = lifecycleScope.launch {
        state.notice = "Importando ${uris.size} imagem(ns)…"
        val animationTargetId = state.animationTargetId
        try {
            for (uri in uris) {
                val assetId = (state.assets.maxOfOrNull { it.id } ?: 0) + 1
                val asset = withContext(Dispatchers.IO) {
                val source = requireNotNull(contentResolver.openInputStream(uri)) {
                    "Não foi possível ler a imagem."
                }.use { it.readBytes() }
                require(source.isNotEmpty()) { "A imagem selecionada está vazia." }
                require(source.size <= 32 * 1024 * 1024) { "A imagem excede o limite de 32 MB." }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(source, 0, source.size, bounds)
                require(bounds.outWidth > 0 && bounds.outHeight > 0) { "O arquivo não é uma imagem compatível." }
                var sampleSize = 1
                while (bounds.outWidth / sampleSize > 2048 || bounds.outHeight / sampleSize > 2048) {
                    sampleSize *= 2
                }
                val bitmap = BitmapFactory.decodeByteArray(
                    source, 0, source.size, BitmapFactory.Options().apply { inSampleSize = sampleSize }
                ) ?: error("Não foi possível decodificar a imagem.")
                val encoded = ByteArrayOutputStream().use { output ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                    Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                }
                bitmap.recycle()
                ImageAsset(
                    id = assetId,
                    name = uri.lastPathSegment?.substringAfterLast('/') ?: "Imagem",
                    mimeType = "image/png",
                    base64 = encoded
                )
                }
                state.assets = state.assets + asset
                val animationTarget = state.objects.firstOrNull { it.id == animationTargetId }
                if (animationTarget != null) {
                    animationTarget.animationFrames = animationTarget.animationFrames + AnimationFrame(asset.id)
                    state.markObjectsChanged()
                } else {
                    val objectId = (state.objects.maxOfOrNull { it.id } ?: 0) + 1
                    state.objects = state.objects + LayoutObject(
                        objectId, asset.name.substringBeforeLast('.'), "Sprite",
                        220f, 180f, 128f, 128f, layoutId = state.activeLayoutId,
                        layerId = state.activeLayerId(), assetId = asset.id
                    )
                    state.selectedId = objectId
                    state.markObjectsChanged()
                }
            }
            state.notice = if (animationTargetId != 0) "Quadro(s) importado(s)" else "Imagem(ns) importada(s)"
        } catch (error: Exception) {
            state.notice = "Falha ao importar imagem: ${error.message ?: "erro desconhecido"}"
        } finally {
            state.animationTargetId = 0
        }
    }

    private fun openUri(uri: Uri) = lifecycleScope.launch {
        state.notice = "Abrindo projeto…"
        try {
            val result = withContext(Dispatchers.IO) {
                val temp = File(cacheDir, "opened-project.tmp")
                val input = requireNotNull(contentResolver.openInputStream(uri)) {
                    "Não foi possível abrir o arquivo selecionado."
                }
                input.use { stream -> temp.outputStream().use { output -> stream.copyTo(output) } }
                val inspection = CoreNative.inspectCap(temp.absolutePath)
                if (inspection.contains("\"valid\":true")) {
                    val originalName = uri.lastPathSegment?.substringAfterLast('/')
                        ?.replace(Regex("[^A-Za-z0-9._-]"), "_") ?: "projeto.cap"
                    val preserved = File(filesDir, originalName)
                    check(CoreNative.openCap(temp.absolutePath, preserved.absolutePath)) {
                        "Não foi possível preservar o arquivo CAP."
                    }
                    OpenResult.Cap(originalName)
                } else {
                    val json = temp.readText(Charsets.UTF_8)
                    if (json.startsWith("{")) OpenResult.AndroidProject(parseNativeProject(json))
                    else OpenResult.Unsupported
                }
            }
            when (result) {
                is OpenResult.Cap -> {
                    state.projectName = result.name.substringBeforeLast('.')
                    state.notice = "CAP aberto; arquivo original preservado"
                    state.modified = false
                }
                is OpenResult.AndroidProject -> loadNativeProject(result.project)
                OpenResult.Unsupported -> state.notice = "Arquivo .cap inválido ou formato não reconhecido"
            }
        } catch (error: Exception) {
            state.notice = "Falha ao abrir: ${error.message ?: "erro desconhecido"}"
        }
    }

    private fun saveUri(uri: Uri) = lifecycleScope.launch {
        state.notice = "Salvando projeto…"
        try {
            val snapshot = projectSnapshot()
            withContext(Dispatchers.IO) {
                val json = projectJson(snapshot)
                val output = requireNotNull(contentResolver.openOutputStream(uri, "wt")) {
                    "Não foi possível gravar no destino selecionado."
                }
                output.bufferedWriter().use { it.write(json) }
            }
            state.modified = false
            state.notice = "Projeto salvo"
        } catch (error: Exception) {
            state.notice = "Falha ao salvar: ${error.message ?: "erro desconhecido"}"
        }
    }

    private fun projectSnapshot() = ProjectSnapshot(
        name = state.projectName,
        width = state.width,
        height = state.height,
        layouts = state.layouts.toList(),
        startLayoutId = state.startLayoutId,
        objectTypes = state.objectTypes.toList(),
        layers = state.layers.map { it.copy() },
        objects = state.objects.map { it.copy(instanceVariables = it.instanceVariables.toMap()) },
        events = state.events.map { it.copy(conditions = it.conditions.toList(), actions = it.actions.toList(), children = it.children.toList()) },
        globalVariables = state.globalVariables.toMap(),
        assets = state.assets.map { it.copy() }
    )

    private fun serializeEvent(event: EventRow): JSONObject = JSONObject()
        .put("id", event.id)
        .put("eventSheetId", event.eventSheetId)
        .put("blockKind", event.blockKind.name)
        .put("conditionJoin", event.conditionJoin.name)
        .put("name", event.name)
        .put("enabled", event.enabled)
        .put("legacyCondition", event.legacyCondition)
        .put("legacyAction", event.legacyAction)
        .put("conditions", JSONArray().also { array ->
            event.conditions.forEach { condition ->
                array.put(JSONObject().put("kind", condition.kind.name).put("key", condition.key)
                    .put("variable", condition.variable).put("compare", condition.compare.name)
                    .put("value", condition.value).put("firstObject", condition.firstObject)
                    .put("secondObject", condition.secondObject).put("valueExpression", condition.valueExpression)
                    .put("inverted", condition.inverted))
            }
        })
        .put("actions", JSONArray().also { array ->
            event.actions.forEach { action ->
                array.put(JSONObject().put("kind", action.kind.name).put("targetName", action.targetName)
                    .put("variable", action.variable).put("value", action.value)
                    .put("valueY", action.valueY).put("functionName", action.functionName)
                    .put("valueExpression", action.valueExpression).put("valueYExpression", action.valueYExpression))
            }
        })
        .put("children", JSONArray().also { array -> event.children.forEach { array.put(serializeEvent(it)) } })

    private fun readEvent(json: JSONObject): EventRow {
        val conditionArray = json.optJSONArray("conditions")
        val conditions = conditionArray?.let { values ->
            List(values.length()) { index ->
                val value = values.getJSONObject(index)
                EventCondition(
                    kind = enumValue(value.optString("kind"), ConditionKind.UNSUPPORTED),
                    key = value.optString("key", "DIREITA"),
                    variable = value.optString("variable", "pontuacao"),
                    compare = enumValue(value.optString("compare"), CompareKind.GREATER),
                    value = value.optDouble("value").toFloat(),
                    firstObject = value.optString("firstObject"),
                    secondObject = value.optString("secondObject"),
                    valueExpression = value.optString("valueExpression"),
                    inverted = value.optBoolean("inverted", false)
                )
            }
        } ?: listOf(EventCondition(ConditionKind.UNSUPPORTED))
        val actionArray = json.optJSONArray("actions")
        val actions = actionArray?.let { values ->
            List(values.length()) { index ->
                val value = values.getJSONObject(index)
                EventAction(
                    kind = enumValue(value.optString("kind"), ActionKind.UNSUPPORTED),
                    targetName = value.optString("targetName"),
                    variable = value.optString("variable", "pontuacao"),
                    value = value.optDouble("value").toFloat(),
                    valueY = value.optDouble("valueY").toFloat(),
                    functionName = value.optString("functionName"),
                    valueExpression = value.optString("valueExpression"),
                    valueYExpression = value.optString("valueYExpression")
                )
            }
        } ?: listOf(EventAction(ActionKind.UNSUPPORTED))
        val children = json.optJSONArray("children")?.let { values ->
            List(values.length()) { readEvent(values.getJSONObject(it)) }
        } ?: emptyList()
        return EventRow(
            id = json.optLong("id", System.nanoTime()),
            eventSheetId = json.optInt("eventSheetId", 1),
            blockKind = enumValue(json.optString("blockKind"), EventBlockKind.EVENT),
            conditionJoin = enumValue(json.optString("conditionJoin"), ConditionJoin.AND),
            name = json.optString("name"),
            conditions = conditions,
            actions = actions,
            children = children,
            enabled = json.optBoolean("enabled", true),
            legacyCondition = json.optString("condition", json.optString("legacyCondition")),
            legacyAction = json.optString("action", json.optString("legacyAction"))
        )
    }

    private fun readBehaviors(json: JSONArray?): List<BehaviorSpec> {
        if (json == null) return emptyList()
        return List(json.length()) { index ->
            val behavior = json.getJSONObject(index)
            BehaviorSpec(
                kind = enumValue(behavior.optString("kind"), BehaviorKind.EIGHT_DIRECTION),
                speed = behavior.optDouble("speed", 180.0).toFloat(),
                gravity = behavior.optDouble("gravity", 900.0).toFloat(),
                jumpStrength = behavior.optDouble("jumpStrength", 380.0).toFloat(),
                cameraZoom = behavior.optDouble("cameraZoom", 1.0).toFloat()
            )
        }
    }

    private fun readAnimationFrames(json: JSONArray?): List<AnimationFrame> {
        if (json == null) return emptyList()
        return List(json.length()) { index ->
            val frame = json.getJSONObject(index)
            AnimationFrame(frame.optInt("assetId"), frame.optDouble("durationSeconds", 0.15).toFloat())
        }
    }

    private fun projectJson(project: ProjectSnapshot): String {
        val objects = JSONArray()
        project.objects.forEach { o ->
            objects.put(JSONObject()
                .put("id", o.id).put("name", o.name).put("plugin", o.plugin)
                .put("x", o.x).put("y", o.y).put("w", o.w).put("h", o.h)
                .put("layoutId", o.layoutId)
                .put("text", o.text)
                .put("rotation", o.rotation).put("layerId", o.layerId).put("visible", o.visible)
                .put("assetId", o.assetId)
                .put("animationName", o.animationName)
                .put("frameDuration", o.frameDuration)
                .put("instanceVariables", JSONObject().also { values ->
                    o.instanceVariables.forEach { (name, value) -> values.put(name, value) }
                })
                .put("behaviors", JSONArray().also { behaviors ->
                    o.behaviors.forEach { b ->
                        behaviors.put(JSONObject().put("kind", b.kind.name)
                            .put("speed", b.speed).put("gravity", b.gravity).put("jumpStrength", b.jumpStrength)
                            .put("cameraZoom", b.cameraZoom))
                    }
                })
                .put("animationFrames", JSONArray().also { frames ->
                    o.animationFrames.forEach { frame ->
                        frames.put(JSONObject().put("assetId", frame.assetId)
                            .put("durationSeconds", frame.durationSeconds))
                    }
                }))
        }
        val events = JSONArray()
        project.events.forEach { events.put(serializeEvent(it)) }
        val layers = JSONArray()
        project.layers.forEach { layer ->
            layers.put(JSONObject().put("id", layer.id).put("name", layer.name)
            .put("layoutId", layer.layoutId)
                .put("visible", layer.visible).put("opacity", layer.opacity)
                .put("scrollX", layer.scrollX).put("scrollY", layer.scrollY))
        }
        val variables = JSONObject()
        project.globalVariables.forEach { (key, value) -> variables.put(key, value) }
        val assets = JSONArray()
        project.assets.forEach { asset ->
            assets.put(JSONObject().put("id", asset.id).put("name", asset.name)
                .put("mimeType", asset.mimeType).put("base64", asset.base64))
        }
        val layouts = JSONArray()
        project.layouts.forEach { layout ->
            layouts.put(JSONObject()
                .put("id", layout.id).put("name", layout.name)
                .put("width", layout.width).put("height", layout.height)
                .put("eventSheetId", layout.eventSheetId))
        }
        return JSONObject()
            .put("format", "ConstructClassicAndroid")
            .put("formatVersion", 2)
            .put("originalCapCompatible", false)
            .put("name", project.name)
            .put("width", project.width).put("height", project.height)
            .put("layouts", layouts)
            .put("startLayoutId", project.startLayoutId)
            .put("objectTypes", JSONArray(project.objectTypes))
            .put("layers", layers).put("objects", objects).put("events", events)
            .put("globalVariables", variables).put("assets", assets)
            .toString(2)
    }

    private fun parseNativeProject(json: String): ProjectSnapshot {
        val root = JSONObject(json)
        val layouts = root.optJSONArray("layouts")?.let { values ->
            List(values.length()) { index ->
                val value = values.opt(index)
                if (value is JSONObject) {
                    LayoutInfo(
                        id = value.optInt("id", index + 1),
                        name = value.optString("name", "Cena ${index + 1}"),
                        width = value.optInt("width", 640),
                        height = value.optInt("height", 480),
                        eventSheetId = value.optInt("eventSheetId", index + 1)
                    )
                } else {
                    LayoutInfo(index + 1, value?.toString() ?: "Cena ${index + 1}", 640, 480, index + 1)
                }
            }.ifEmpty { listOf(LayoutInfo(1, "Cena 1", 640, 480, 1)) }
        } ?: listOf(LayoutInfo(1, "Cena 1", 640, 480, 1))
        val objectTypes = root.optJSONArray("objectTypes")?.let { values ->
            List(values.length()) { values.getString(it) }
        } ?: listOf("Imagem", "Texto")
        val loadedObjects = mutableListOf<LayoutObject>()
        val array = root.optJSONArray("objects") ?: JSONArray()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            loadedObjects += LayoutObject(o.optInt("id"), o.optString("name"), o.optString("plugin"),
                o.optDouble("x").toFloat(), o.optDouble("y").toFloat(),
                o.optDouble("w").toFloat(), o.optDouble("h").toFloat(),
                layoutId = o.optInt("layoutId", 1),
                text = o.optString("text", if (o.optString("plugin") == "Text") o.optString("name") else ""),
                rotation = o.optDouble("rotation").toFloat(),
                layerId = o.optInt("layerId", 1),
                visible = o.optBoolean("visible", true),
                assetId = o.optInt("assetId", 0),
                animationName = o.optString("animationName", "Parado"),
                behaviors = readBehaviors(o.optJSONArray("behaviors")),
                frameDuration = o.optDouble("frameDuration", 0.15).toFloat(),
                animationFrames = readAnimationFrames(o.optJSONArray("animationFrames")),
                instanceVariables = o.optJSONObject("instanceVariables")?.let { values ->
                    buildMap { values.keys().forEach { name -> put(name, values.optDouble(name).toFloat()) } }
                } ?: emptyMap())
        }
        val loadedEvents = mutableListOf<EventRow>()
        val eventArray = root.optJSONArray("events") ?: JSONArray()
        for (i in 0 until eventArray.length()) {
            val event = eventArray.getJSONObject(i)
            loadedEvents += readEvent(event)
        }
        val loadedLayers = root.optJSONArray("layers")?.let { values ->
            List(values.length()) { index ->
                val layer = values.getJSONObject(index)
                LayerModel(
                    id = layer.optInt("id", index + 1),
                    name = layer.optString("name", "Camada ${index + 1}"),
                    layoutId = layer.optInt("layoutId", 1),
                    visible = layer.optBoolean("visible", true),
                    opacity = layer.optDouble("opacity", 1.0).toFloat(),
                    scrollX = layer.optDouble("scrollX", 1.0).toFloat(),
                    scrollY = layer.optDouble("scrollY", 1.0).toFloat()
                )
            }.ifEmpty { listOf(LayerModel(1, "Principal")) }
        } ?: listOf(LayerModel(1, "Principal"))
        val layersWithLayouts = loadedLayers.toMutableList()
        layouts.forEach { layout ->
            if (layersWithLayouts.none { it.layoutId == layout.id }) {
                layersWithLayouts += LayerModel(
                    id = (layersWithLayouts.maxOfOrNull { it.id } ?: 0) + 1,
                    name = "Principal",
                    layoutId = layout.id
                )
            }
        }
        val variablesJson = root.optJSONObject("globalVariables") ?: JSONObject()
        val variables = buildMap {
            variablesJson.keys().forEach { key -> put(key, variablesJson.optDouble(key).toFloat()) }
        }.ifEmpty { mapOf("pontuacao" to 0f) }
        val loadedAssets = root.optJSONArray("assets")?.let { values ->
            List(values.length()) { index ->
                val asset = values.getJSONObject(index)
                ImageAsset(asset.optInt("id"), asset.optString("name"),
                    asset.optString("mimeType", "image/png"), asset.optString("base64"))
            }
        } ?: emptyList()
        return ProjectSnapshot(
            name = root.optString("name", "Projeto importado"),
            width = root.optInt("width", 640),
            height = root.optInt("height", 480),
            layouts = layouts,
            startLayoutId = root.optInt("startLayoutId", layouts.firstOrNull()?.id ?: 1),
            objectTypes = objectTypes,
            layers = layersWithLayouts,
            objects = loadedObjects,
            events = loadedEvents,
            globalVariables = variables,
            assets = loadedAssets
        )
    }

    private fun loadNativeProject(project: ProjectSnapshot) {
        state.projectName = project.name
        state.width = project.width
        state.height = project.height
        state.layouts = project.layouts
        state.activeLayoutId = project.layouts.firstOrNull()?.id ?: 1
        state.startLayoutId = project.startLayoutId
        state.width = project.layouts.firstOrNull()?.width ?: project.width
        state.height = project.layouts.firstOrNull()?.height ?: project.height
        state.objectTypes = project.objectTypes
        state.layers = project.layers
        state.objects = project.objects
        state.events = project.events
        state.globalVariables = project.globalVariables
        state.assets = project.assets
        state.selectedId = state.objects.firstOrNull()?.id ?: 0
        state.nextEventId = findMaximumEventId(state.events) + 1
        state.markObjectsChanged()
        state.notice = "Projeto Android aberto"
        state.modified = false
    }

    private sealed interface OpenResult {
        data class Cap(val name: String) : OpenResult
        data class AndroidProject(val project: ProjectSnapshot) : OpenResult
        data object Unsupported : OpenResult
    }
}

@Composable
fun ConstructTheme(content: @Composable () -> Unit) {
    val colors = lightColorScheme(
        primary = Color(0xFF2F6B9A),
        secondary = Color(0xFFF0A12A),
        tertiary = Color(0xFF4B8BBE),
        surface = Color(0xFFF6F8FA)
    )
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ConstructApp(
    state: EditorState,
    onOpen: () -> Unit,
    onSave: () -> Unit,
    onImport: () -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    var panel by remember { mutableStateOf("Objetos") }
    var showDesktopLeftPanel by remember { mutableStateOf(false) }
    var showMobilePanel by remember { mutableStateOf(false) }
    var showDesktopPanel by remember { mutableStateOf(true) }
    var navigationExpanded by remember { mutableStateOf(true) }
    var showGame by remember { mutableStateOf(false) }
    var debugGame by remember { mutableStateOf(false) }
    var runLayoutId by remember { mutableIntStateOf(state.activeLayoutId) }
    val compactScreen = LocalConfiguration.current.screenWidthDp < 600
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.projectName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${if (state.modified) "*" else ""}${state.notice}",
                            style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        debugGame = false
                        runLayoutId = state.activeLayoutId
                        showGame = true
                    }) { Icon(Icons.Default.PlayArrow, contentDescription = "Executar cena") }
                    if (tab == 0) {
                        if (!compactScreen) {
                            IconButton(onClick = { showDesktopLeftPanel = !showDesktopLeftPanel }) {
                                Icon(Icons.Default.AccountTree, contentDescription = "Abrir Project Bar")
                            }
                        }
                        IconButton(onClick = {
                            if (compactScreen) showMobilePanel = true else showDesktopPanel = !showDesktopPanel
                        }) {
                            Icon(Icons.Default.Tune, contentDescription = "Abrir painel")
                        }
                    }
                    IconButton(onClick = onOpen) { Icon(Icons.Default.FolderOpen, contentDescription = "Abrir projeto") }
                    IconButton(onClick = onSave) { Icon(Icons.Default.Save, contentDescription = "Salvar projeto") }
                }
            )
        },
        bottomBar = {
            Column {
                Box(
                    Modifier.fillMaxWidth().height(18.dp).background(MaterialTheme.colorScheme.surfaceVariant)
                        .pointerInput(navigationExpanded) {
                            var dragDistance = 0f
                            detectVerticalDragGestures(
                                onVerticalDrag = { change, amount ->
                                    dragDistance += amount
                                    if (dragDistance > 24f && navigationExpanded) {
                                        navigationExpanded = false
                                        dragDistance = 0f
                                    } else if (dragDistance < -24f && !navigationExpanded) {
                                        navigationExpanded = true
                                        dragDistance = 0f
                                    }
                                    change.consume()
                                },
                                onDragEnd = { dragDistance = 0f }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (navigationExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        contentDescription = if (navigationExpanded) "Recolher ferramentas" else "Expandir ferramentas",
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (navigationExpanded) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = tab == 0, onClick = { tab = 0 },
                            icon = { Icon(Icons.Default.GridOn, null) }, label = { Text("Cena") })
                        NavigationBarItem(
                            selected = tab == 1, onClick = { tab = 1; showMobilePanel = false },
                            icon = { Icon(Icons.Default.AccountTree, null) }, label = { Text("Eventos") })
                        NavigationBarItem(
                            selected = tab == 2, onClick = { tab = 2; showMobilePanel = false },
                            icon = { Icon(Icons.Default.Movie, null) }, label = { Text("Animações") })
                        NavigationBarItem(
                            selected = tab == 3, onClick = { tab = 3; showMobilePanel = false },
                            icon = { Icon(Icons.Default.PhotoLibrary, null) }, label = { Text("Projeto") })
                    }
                }
            }
        }
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            val compact = maxWidth < 600.dp
            val panelWidth = if (maxWidth >= 1000.dp) 280.dp else 240.dp
            if (tab == 0) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (!compact && showDesktopLeftPanel) {
                        ProjectBar(
                            state = state,
                            onImport = onImport,
                            modifier = Modifier.width(panelWidth)
                        )
                    }
                    if (!compact) {
                        PanelDragHandle(
                            isOpen = showDesktopLeftPanel,
                            opensTowardRight = true,
                            onToggle = { showDesktopLeftPanel = !showDesktopLeftPanel }
                        )
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        LayoutEditor(state) {
                            panel = "Propriedades"
                            if (compact) showMobilePanel = true else showDesktopPanel = true
                        }
                    }
                    if (!compact) {
                        PanelDragHandle(
                            isOpen = showDesktopPanel,
                            opensTowardRight = false,
                            onToggle = { showDesktopPanel = !showDesktopPanel }
                        )
                    }
                    if (!compact && showDesktopPanel) {
                        PropertiesPanel(
                            state = state,
                            panel = panel,
                            onPanelChange = { panel = it },
                            onImport = onImport,
                            modifier = Modifier.width(panelWidth)
                        )
                    }
                }
            } else {
                when (tab) {
                    1 -> EventEditor(state, Modifier.fillMaxSize())
                    2 -> AnimationEditor(state, onImport, Modifier.fillMaxSize())
                    3 -> ProjectManager(
                        state, Modifier.fillMaxSize(),
                        onRunProject = { debugGame = false; runLayoutId = state.startLayoutId; showGame = true },
                        onDebugLayout = { debugGame = true; runLayoutId = state.activeLayoutId; showGame = true },
                        onNewProject = {
                            state.createNewProject()
                            tab = 0
                        }
                    )
                }
            }
            if (showMobilePanel && compact && tab == 0) {
                ModalBottomSheet(
                    onDismissRequest = { showMobilePanel = false },
                    sheetState = sheetState
                ) {
                    PropertiesPanel(
                        state = state,
                        panel = panel,
                        onPanelChange = { panel = it },
                        onImport = onImport,
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.82f)
                    )
                }
            }
            if (showGame) {
                val activeLayout = state.layouts.firstOrNull { it.id == runLayoutId } ?: state.layouts.firstOrNull()
                val runtimeWidth = activeLayout?.width ?: state.width
                val runtimeHeight = activeLayout?.height ?: state.height
                val runtimeObjects = state.objects.filter { it.layoutId == (activeLayout?.id ?: 1) }
                val runtimeEvents = state.events.filter {
                    it.eventSheetId == (activeLayout?.eventSheetId ?: 1)
                }
                val runtime = remember(
                    runtimeWidth, runtimeHeight, runtimeObjects, state.layers, runtimeEvents, state.globalVariables
                ) {
                    GameRuntime(
                        RuntimeProject(
                            runtimeWidth, runtimeHeight,
                            runtimeObjects.map { it.copy(behaviors = it.behaviors.toList(), animationFrames = it.animationFrames.toList()) },
                            state.layers.toList(), runtimeEvents.toList(), state.globalVariables.toMap(),
                            activeLayoutId = activeLayout?.id ?: 1,
                            activeEventSheetId = activeLayout?.eventSheetId ?: 1
                        )
                    )
                }
                Dialog(
                    onDismissRequest = { showGame = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
                ) {
                    Surface(Modifier.fillMaxSize(), color = Color(0xFF10151A)) {
                        GamePreviewScreen(
                            runtime = runtime,
                            assets = state.assets,
                            debug = debugGame,
                            onExit = { showGame = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelDragHandle(
    isOpen: Boolean,
    opensTowardRight: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        Modifier.width(18.dp).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            Modifier.fillMaxSize().pointerInput(isOpen, opensTowardRight) {
                var dragDistance = 0f
                var toggled = false
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, amount ->
                        dragDistance += amount
                        val shouldOpen = if (opensTowardRight) dragDistance > 24f else dragDistance < -24f
                        val shouldClose = if (opensTowardRight) dragDistance < -24f else dragDistance > 24f
                        if (!toggled && ((!isOpen && shouldOpen) || (isOpen && shouldClose))) {
                            onToggle()
                            toggled = true
                        }
                        change.consume()
                    },
                    onDragEnd = {
                        dragDistance = 0f
                        toggled = false
                    },
                    onDragCancel = {
                        dragDistance = 0f
                        toggled = false
                    }
                )
            },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.DragHandle, contentDescription = "Arraste para abrir ou fechar painel")
        }
    }
}

@Composable
private fun ProjectBar(state: EditorState, onImport: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxHeight(), tonalElevation = 2.dp) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)) {
            Text("Projeto", style = MaterialTheme.typography.titleMedium)
            Text("Cenas", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            state.layouts.forEach { layout ->
                TextButton(
                    onClick = {
                        state.activeLayoutId = layout.id
                        state.width = layout.width
                        state.height = layout.height
                        state.selectedId = state.objects.firstOrNull { it.layoutId == layout.id }?.id ?: 0
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        if (state.activeLayoutId == layout.id) Icons.Default.GridOn else Icons.Default.GridView,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(layout.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            HorizontalDivider()
            Text("Instâncias", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            state.objects.filter { it.layoutId == state.activeLayoutId }.forEach { obj ->
                TextButton(
                    onClick = { state.selectedId = obj.id },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Category, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(obj.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recursos", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onImport) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Importar imagem")
                }
            }
            state.assets.forEach { asset ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = asset.name,
                        onValueChange = { name ->
                            asset.name = name
                            state.assets = state.assets.toList()
                            state.markProjectChanged()
                        },
                        singleLine = true,
                        label = { Text("Recurso") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        state.assets = state.assets.filterNot { it.id == asset.id }
                        state.objects.forEach { obj ->
                            if (obj.assetId == asset.id) obj.assetId = 0
                            obj.animationFrames = obj.animationFrames.filterNot { it.assetId == asset.id }
                        }
                        state.markObjectsChanged()
                    }) { Icon(Icons.Default.Delete, contentDescription = "Excluir recurso ${asset.name}") }
                }
            }
            if (state.assets.isEmpty()) Text("Nenhum recurso importado.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PropertiesPanel(
            state: EditorState,
            panel: String,
            onPanelChange: (String) -> Unit,
            onImport: () -> Unit,
            modifier: Modifier = Modifier
        ) {
            val selected = remember(state.objects, state.selectedId, state.objectRevision) {
                state.objects.firstOrNull { it.id == state.selectedId }
            }
            var instanceVariableName by remember(selected?.id) { mutableStateOf("") }
            var instanceVariableValue by remember(selected?.id) { mutableStateOf("0") }
            var instanceVariableError by remember(selected?.id) { mutableStateOf<String?>(null) }
            Surface(
                modifier = modifier.fillMaxHeight(),
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row {
                        listOf("Objetos", "Propriedades", "Camadas").forEach { label ->
                            FilterChip(
                                selected = panel == label,
                                onClick = { onPanelChange(label) },
                                label = { Text(label) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                    if (panel == "Objetos") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Tipos de objetos", style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                val id = (state.objects.maxOfOrNull { it.id } ?: 0) + 1
                                val obj = LayoutObject(
                                    id, "Imagem $id", "Sprite", 200f, 160f, 96f, 96f,
                                    layoutId = state.activeLayoutId, layerId = state.activeLayerId()
                                )
                                state.objects = state.objects + obj
                                state.selectedId = id
                                state.markObjectsChanged()
                            }) { Icon(Icons.Default.Add, null) }
                            IconButton(onClick = onImport) { Icon(Icons.Default.Image, null) }
                        }
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(state.objectTypes) { type ->
                                if (type in setOf("Imagem", "Sprite", "Texto", "Text")) {
                                    TextButton(onClick = {
                                        val id = (state.objects.maxOfOrNull { it.id } ?: 0) + 1
                                        val plugin = pluginForType(type)
                                        val obj = LayoutObject(
                                            id, "$type $id", plugin, 180f, 140f, 128f, 96f,
                                            layoutId = state.activeLayoutId,
                                            layerId = state.activeLayerId(),
                                            text = if (plugin == "Text") "Texto" else ""
                                        )
                                        state.objects = state.objects + obj
                                        state.selectedId = id
                                        state.markObjectsChanged()
                                    }, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Category, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(type, modifier = Modifier.weight(1f))
                                    }
                                } else {
                                    Text(
                                        "$type — não compatível para criação; dados existentes são preservados",
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else if (panel == "Camadas") {
                        LayerPanel(state)
                    } else {
                        Column(
                            modifier = Modifier.weight(1f).fillMaxWidth()
                                .verticalScroll(rememberScrollState()).imePadding()
                        ) {
                            Text("Propriedades", style = MaterialTheme.typography.titleSmall)
                            if (selected == null) {
                                Text("Selecione um objeto", color = Color.Gray)
                            } else {
                                OutlinedTextField(
                                    value = selected.name,
                                    onValueChange = {
                                        selected.name = it
                                        state.markObjectsChanged()
                                    },
                                    label = { Text("Nome") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (selected.plugin == "Text") {
                                    OutlinedTextField(
                                        value = selected.text,
                                        onValueChange = {
                                            selected.text = it
                                            state.markObjectsChanged()
                                        },
                                        label = { Text("Texto exibido") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = selected.plugin,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Plug-in: ${pluginDisplay(selected.plugin)}") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row {
                                    PropertyNumber("X", selected.x, Modifier.weight(1f)) { selected.x = it; state.markObjectsChanged() }
                                    PropertyNumber("Y", selected.y, Modifier.weight(1f)) { selected.y = it; state.markObjectsChanged() }
                                }
                                Row {
                                    PropertyNumber("L", selected.w, Modifier.weight(1f)) { selected.w = it.coerceAtLeast(8f); state.markObjectsChanged() }
                                    PropertyNumber("A", selected.h, Modifier.weight(1f)) { selected.h = it.coerceAtLeast(8f); state.markObjectsChanged() }
                                }
                                PropertyNumber("Rotação", selected.rotation) {
                                    selected.rotation = it % 360f
                                    state.markObjectsChanged()
                                }
                                Text("Camada", style = MaterialTheme.typography.labelLarge)
                                EnumChoice(
                                    label = state.layers.firstOrNull {
                                        it.id == selected.layerId && it.layoutId == state.activeLayoutId
                                    }?.name ?: "Principal",
                                    options = state.layers.filter { it.layoutId == state.activeLayoutId }.map { it.name }
                                ) { name ->
                                    selected.layerId = state.layers.firstOrNull {
                                        it.layoutId == state.activeLayoutId && it.name == name
                                    }?.id ?: selected.layerId
                                    state.markObjectsChanged()
                                }
                                Text(
                                    "Variáveis da instância",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                selected.instanceVariables.toSortedMap().forEach { (name, value) ->
                                    var editedValue by remember(selected.id, name, value) {
                                        mutableStateOf(value.toString())
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(name, Modifier.weight(1f))
                                        OutlinedTextField(
                                            value = editedValue,
                                            onValueChange = { text ->
                                                editedValue = text
                                                text.toFloatOrNull()?.let { number ->
                                                    selected.instanceVariables =
                                                        selected.instanceVariables + (name to number)
                                                    state.markObjectsChanged()
                                                }
                                            },
                                            label = { Text("Valor") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = {
                                            selected.instanceVariables = selected.instanceVariables - name
                                            state.markObjectsChanged()
                                        }) { Icon(Icons.Default.Delete, contentDescription = "Excluir variável $name") }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = instanceVariableName,
                                        onValueChange = {
                                            instanceVariableName = it
                                            instanceVariableError = null
                                        },
                                        label = { Text("Nova variável") },
                                        modifier = Modifier.weight(1f),
                                        isError = instanceVariableError != null
                                    )
                                    OutlinedTextField(
                                        value = instanceVariableValue,
                                        onValueChange = {
                                            instanceVariableValue = it
                                            instanceVariableError = null
                                        },
                                        label = { Text("Valor") },
                                        modifier = Modifier.weight(1f),
                                        isError = instanceVariableError != null
                                    )
                                    IconButton(onClick = {
                                        val number = instanceVariableValue.toFloatOrNull()
                                        instanceVariableError = when {
                                            instanceVariableName.isBlank() -> "Informe o nome."
                                            number == null -> "Informe um número."
                                            else -> null
                                        }
                                        if (instanceVariableError == null && number != null) {
                                            selected.instanceVariables =
                                                selected.instanceVariables + (instanceVariableName to number)
                                            instanceVariableName = ""
                                            instanceVariableValue = "0"
                                            state.markObjectsChanged()
                                        }
                                    }) { Icon(Icons.Default.Add, contentDescription = "Adicionar variável da instância") }
                                }
                                instanceVariableError?.let {
                                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }
                                Text("Comportamentos", style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(top = 8.dp))
                                BehaviorKind.entries.forEach { kind ->
                                    val present = selected.behaviors.any { it.kind == kind }
                                    FilterChip(
                                        selected = present,
                                        onClick = {
                                            val next = if (present) selected.behaviors.filterNot { it.kind == kind }
                                            else selected.behaviors + BehaviorSpec(kind)
                                            selected.behaviors = next
                                            state.markObjectsChanged()
                                        },
                                        label = { Text(behaviorLabel(kind)) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                selected.behaviors.firstOrNull {
                                    it.kind in setOf(BehaviorKind.EIGHT_DIRECTION, BehaviorKind.PLATFORM, BehaviorKind.BULLET)
                                }?.let { movement ->
                                    var speedText by remember(selected.id, movement.kind, movement.speed) {
                                        mutableStateOf(movement.speed.toInt().toString())
                                    }
                                    OutlinedTextField(
                                        value = speedText,
                                        onValueChange = { text ->
                                            speedText = text
                                            text.toFloatOrNull()?.let { speed ->
                                                selected.behaviors = selected.behaviors.map {
                                                    if (it.kind == movement.kind) it.copy(speed = speed.coerceAtLeast(0f)) else it
                                                }
                                                state.markObjectsChanged()
                                            }
                                        },
                                        label = { Text("Velocidade") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (selected.behaviors.any { it.kind == BehaviorKind.PLATFORM || it.kind == BehaviorKind.PHYSICS }) {
                                    val gravityBehavior = selected.behaviors.first {
                                        it.kind == BehaviorKind.PLATFORM || it.kind == BehaviorKind.PHYSICS
                                    }
                                    var gravityText by remember(selected.id, gravityBehavior.gravity) {
                                        mutableStateOf(gravityBehavior.gravity.toInt().toString())
                                    }
                                    OutlinedTextField(
                                        value = gravityText,
                                        onValueChange = { text ->
                                            gravityText = text
                                            text.toFloatOrNull()?.let { gravity ->
                                                selected.behaviors = selected.behaviors.map {
                                                    if (it.kind == gravityBehavior.kind) it.copy(gravity = gravity.coerceAtLeast(0f)) else it
                                                }
                                                state.markObjectsChanged()
                                            }
                                        },
                                        label = { Text("Gravidade") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                selected.behaviors.firstOrNull { it.kind == BehaviorKind.SCROLL_TO }?.let { camera ->
                                    var zoomText by remember(selected.id, camera.cameraZoom) {
                                        mutableStateOf(camera.cameraZoom.toString())
                                    }
                                    OutlinedTextField(
                                        value = zoomText,
                                        onValueChange = { text ->
                                            zoomText = text
                                            text.toFloatOrNull()?.let { zoom ->
                                                selected.behaviors = selected.behaviors.map {
                                                    if (it.kind == BehaviorKind.SCROLL_TO) {
                                                        it.copy(cameraZoom = zoom.coerceIn(0.25f, 4f))
                                                    } else it
                                                }
                                                state.markObjectsChanged()
                                            }
                                        },
                                        label = { Text("Zoom da câmera (0,25–4)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                OutlinedButton(onClick = {
                                    state.objects = state.objects.filter { it.id != selected.id }
                                    state.selectedId = state.objects.firstOrNull()?.id ?: 0
                                    state.markObjectsChanged()
                                }) { Icon(Icons.Default.Delete, null); Spacer(Modifier.width(6.dp)); Text("Excluir") }
                            }
                        }
                    }
                }
            }
        }

        @Composable
        private fun LayerPanel(state: EditorState) {
            val sceneLayers = state.layers.filter { it.layoutId == state.activeLayoutId }
            Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Camadas da cena", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        val nextId = (state.layers.maxOfOrNull { it.id } ?: 0) + 1
                        state.layers = state.layers + LayerModel(
                            id = nextId,
                            name = "Camada $nextId",
                            layoutId = state.activeLayoutId
                        )
                        state.markProjectChanged()
                    }) { Icon(Icons.Default.Add, contentDescription = "Adicionar camada") }
                }
                LazyColumn(Modifier.weight(1f)) {
                    items(sceneLayers, key = { it.id }) { layer ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    state.layers = state.layers.map {
                                        if (it.id == layer.id) it.copy(visible = !it.visible) else it
                                    }
                                    state.markProjectChanged()
                                }) {
                                    Icon(
                                        if (layer.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (layer.visible) "Ocultar camada" else "Mostrar camada"
                                    )
                                }
                                var layerName by remember(layer.id, layer.name) { mutableStateOf(layer.name) }
                                OutlinedTextField(
                                    value = layerName,
                                    onValueChange = { name ->
                                        layerName = name
                                        state.layers = state.layers.map {
                                            if (it.id == layer.id) it.copy(name = name) else it
                                        }
                                        state.markProjectChanged()
                                    },
                                    singleLine = true,
                                    label = { Text("Camada") },
                                    modifier = Modifier.weight(1f)
                                )
                                Text("${state.objects.count {
                                    it.layoutId == state.activeLayoutId && it.layerId == layer.id
                                }}")
                                IconButton(
                                    enabled = sceneLayers.size > 1,
                                    onClick = {
                                        val fallback = sceneLayers.first { it.id != layer.id }
                                        state.objects.forEach { obj ->
                                            if (obj.layoutId == state.activeLayoutId && obj.layerId == layer.id) {
                                                obj.layerId = fallback.id
                                            }
                                        }
                                        state.layers = state.layers.filterNot { it.id == layer.id }
                                        state.markObjectsChanged()
                                    }
                                ) { Icon(Icons.Default.Delete, contentDescription = "Excluir camada") }
                            }
                            Row(Modifier.fillMaxWidth()) {
                                PropertyNumber("Parallax X", layer.scrollX, Modifier.weight(1f)) { value ->
                                    state.layers = state.layers.map {
                                        if (it.id == layer.id) it.copy(scrollX = value.coerceIn(0f, 4f)) else it
                                    }
                                    state.markProjectChanged()
                                }
                                PropertyNumber("Parallax Y", layer.scrollY, Modifier.weight(1f)) { value ->
                                    state.layers = state.layers.map {
                                        if (it.id == layer.id) it.copy(scrollY = value.coerceIn(0f, 4f)) else it
                                    }
                                    state.markProjectChanged()
                                }
                            }
                        }
                    }
                }
            }
        }

        @Composable
        private fun EnumChoice(label: String, options: List<String>, onSelected: (String) -> Unit) {
            var expanded by remember { mutableStateOf(false) }
            var query by remember { mutableStateOf("") }
            Box {
                OutlinedButton(
                    onClick = { query = ""; expanded = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    Column(
                        Modifier.widthIn(min = 220.dp, max = 360.dp).heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState()).padding(horizontal = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Pesquisar opções") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        val filtered = options.filter { it.contains(query, ignoreCase = true) }
                        if (filtered.isEmpty()) {
                            Text("Nenhuma opção encontrada.", Modifier.padding(12.dp))
                        } else {
                            filtered.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = { onSelected(option); expanded = false }
                                )
                            }
                        }
                    }
                }
            }
        }

        private fun behaviorLabel(kind: BehaviorKind) = when (kind) {
            BehaviorKind.EIGHT_DIRECTION -> "8 direções"
            BehaviorKind.PLATFORM -> "Plataforma"
            BehaviorKind.BULLET -> "Projétil"
            BehaviorKind.SOLID -> "Sólido"
            BehaviorKind.SCROLL_TO -> "Seguir câmera"
            BehaviorKind.BOUND_TO_LAYOUT -> "Limitar à cena"
            BehaviorKind.PHYSICS -> "Física (gravidade simples)"
        }

@Composable
private fun PropertyNumber(label: String, value: Float, modifier: Modifier = Modifier, onChange: (Float) -> Unit) {
            var text by remember(value) { mutableStateOf(value.toInt().toString()) }
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    it.toFloatOrNull()?.let(onChange)
                },
                label = { Text(label) },
                modifier = modifier.padding(end = 4.dp, bottom = 4.dp)
            )
}

@Composable
private fun EventEditor(state: EditorState, modifier: Modifier = Modifier) {
    var variableName by remember { mutableStateOf("") }
    var variableValue by remember { mutableStateOf("0") }
    var variableNameError by remember { mutableStateOf<String?>(null) }
    var variableValueError by remember { mutableStateOf<String?>(null) }
    val activeLayout = state.layouts.firstOrNull { it.id == state.activeLayoutId } ?: state.layouts.firstOrNull()
    val activeSheetId = activeLayout?.eventSheetId ?: 1
    val sheetEvents = state.events.filter { it.eventSheetId == activeSheetId }
    Column(modifier.fillMaxSize()) {
        Text(
            "Folha de eventos — ${activeLayout?.name ?: "Cena"}",
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EventAddButton("Evento") {
                state.events = state.events + EventRow(
                    id = state.nextEventId++,
                    eventSheetId = activeSheetId,
                    conditions = listOf(EventCondition()),
                    actions = listOf(EventAction())
                )
                state.markProjectChanged()
            }
            EventAddButton("Grupo") {
                state.events = state.events + EventRow(
                    id = state.nextEventId++, eventSheetId = activeSheetId, blockKind = EventBlockKind.GROUP, name = "Novo grupo"
                )
                state.markProjectChanged()
            }
            EventAddButton("Comentário") {
                state.events = state.events + EventRow(
                    id = state.nextEventId++, eventSheetId = activeSheetId, blockKind = EventBlockKind.COMMENT, name = "Comentário"
                )
                state.markProjectChanged()
            }
            EventAddButton("Função") {
                state.events = state.events + EventRow(
                    id = state.nextEventId++, eventSheetId = activeSheetId, blockKind = EventBlockKind.FUNCTION, name = "Nova função"
                )
                state.markProjectChanged()
            }
            EventAddButton("Subevento") {
                val parent = flattenEventTree(sheetEvents).lastOrNull {
                    it.first.blockKind == EventBlockKind.EVENT ||
                        it.first.blockKind == EventBlockKind.GROUP ||
                        it.first.blockKind == EventBlockKind.FUNCTION
                }?.first
                if (parent != null) {
                    val child = EventRow(state.nextEventId++, eventSheetId = activeSheetId, conditions = listOf(EventCondition()),
                        actions = listOf(EventAction()))
                    state.events = updateEventTree(state.events, parent.id) {
                        it.copy(children = it.children + child)
                    }
                    state.markProjectChanged()
                }
            }
        }
        HorizontalDivider()
        LazyColumn(Modifier.weight(1f).fillMaxWidth().imePadding().padding(horizontal = 8.dp)) {
            items(flattenEventTree(sheetEvents), key = { it.first.id }) { (event, depth) ->
                EventBlockEditor(
                    event = event,
                    depth = depth,
                    objectNames = state.objects.filter { it.layoutId == state.activeLayoutId }.map { it.name },
                    onDuplicate = {
                        val copy = duplicateEventTree(event) { state.nextEventId++ }
                        state.events = insertEventAfter(state.events, event.id, copy)
                        state.markProjectChanged()
                    },
                    onChange = { updated ->
                        state.events = updateEventTree(state.events, event.id) { updated }
                        state.markProjectChanged()
                    },
                    onDelete = {
                        state.events = removeEventTree(state.events, event.id)
                        state.markProjectChanged()
                    }
                )
            }
        }
        Card(Modifier.fillMaxWidth().padding(8.dp)) {
            Column(Modifier.padding(8.dp)) {
                Text("Variáveis globais", style = MaterialTheme.typography.titleSmall)
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 120.dp).verticalScroll(rememberScrollState())
                ) {
                    state.globalVariables.forEach { (name, value) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$name = $value", Modifier.weight(1f))
                            IconButton(onClick = {
                                state.globalVariables = state.globalVariables - name
                                state.markProjectChanged()
                            }) { Icon(Icons.Default.Delete, contentDescription = "Excluir variável $name") }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = variableName,
                        onValueChange = {
                            variableName = it
                            variableNameError = null
                        },
                        label = { Text("Nome") },
                        isError = variableNameError != null,
                        supportingText = { variableNameError?.let { Text(it) } },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    OutlinedTextField(
                        value = variableValue,
                        onValueChange = {
                            variableValue = it
                            variableValueError = null
                        },
                        label = { Text("Valor numérico") },
                        isError = variableValueError != null,
                        supportingText = { variableValueError?.let { Text(it) } },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        val value = variableValue.toFloatOrNull()
                        variableNameError = if (variableName.isBlank()) "Digite o nome da variável." else null
                        variableValueError = if (value == null) "Digite um número válido." else null
                        if (variableNameError == null && variableValueError == null && value != null) {
                            state.globalVariables = state.globalVariables + (variableName to value)
                            state.markProjectChanged()
                            variableName = ""
                            variableValue = "0"
                            variableNameError = null
                            variableValueError = null
                        }
                    }) { Icon(Icons.Default.Add, contentDescription = "Adicionar variável global") }
                }
            }
        }
    }
}

@Composable
private fun EventAddButton(label: String, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.width(4.dp))
        Text(label)
    }
}

@Composable
private fun EventBlockEditor(
    event: EventRow,
    depth: Int,
    objectNames: List<String>,
    onDuplicate: () -> Unit,
    onChange: (EventRow) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().padding(start = (depth * 12).dp, top = 4.dp, bottom = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (event.blockKind) {
                EventBlockKind.GROUP -> Color(0xFFE9EFF7)
                EventBlockKind.COMMENT -> Color(0xFFFFF3D9)
                EventBlockKind.FUNCTION -> Color(0xFFE8F4EA)
                EventBlockKind.EVENT -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when (event.blockKind) {
                        EventBlockKind.EVENT -> "Evento"
                        EventBlockKind.GROUP -> "Grupo"
                        EventBlockKind.COMMENT -> "Comentário"
                        EventBlockKind.FUNCTION -> "Função"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                if (event.blockKind == EventBlockKind.EVENT || event.blockKind == EventBlockKind.GROUP) {
                    Switch(checked = event.enabled, onCheckedChange = { onChange(event.copy(enabled = it)) })
                }
                IconButton(onClick = onDuplicate) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicar bloco")
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Excluir bloco") }
            }
            if (event.blockKind != EventBlockKind.EVENT) {
                OutlinedTextField(
                    value = event.name,
                    onValueChange = { onChange(event.copy(name = it)) },
                    label = {
                        Text(when (event.blockKind) {
                            EventBlockKind.GROUP -> "Nome do grupo"
                            EventBlockKind.FUNCTION -> "Nome da função"
                            else -> "Comentário"
                        })
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (event.blockKind == EventBlockKind.EVENT) {
                if (event.conditions.size > 1) {
                    EnumChoice(
                        if (event.conditionJoin == ConditionJoin.AND) "Todas as condições (E)" else "Qualquer condição (OU)",
                        ConditionJoin.entries.map { if (it == ConditionJoin.AND) "Todas as condições (E)" else "Qualquer condição (OU)" }
                    ) { label ->
                        onChange(event.copy(conditionJoin = if (label.startsWith("Todas")) ConditionJoin.AND else ConditionJoin.OR))
                    }
                }
                event.conditions.forEachIndexed { index, condition ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ConditionEditor(condition, objectNames, Modifier.weight(1f)) { changed ->
                            onChange(event.copy(conditions = event.conditions.toMutableList().also { it[index] = changed }))
                        }
                        if (event.conditions.size > 1) {
                            IconButton(onClick = {
                                onChange(event.copy(conditions = event.conditions.toMutableList().also { it.removeAt(index) }))
                            }) { Icon(Icons.Default.Delete, contentDescription = "Remover condição") }
                        }
                    }
                    if (index < event.conditions.lastIndex) {
                        Text(
                            if (event.conditionJoin == ConditionJoin.AND) "E" else "OU",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Row {
                    TextButton(onClick = {
                        onChange(event.copy(conditions = event.conditions + EventCondition()))
                    }) { Text("+ Condição") }
                    TextButton(onClick = {
                        onChange(event.copy(actions = event.actions + EventAction()))
                    }) { Text("+ Ação") }
                }
                HorizontalDivider()
                event.actions.forEachIndexed { index, action ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ActionEditor(action, objectNames, Modifier.weight(1f)) { changed ->
                            onChange(event.copy(actions = event.actions.toMutableList().also { it[index] = changed }))
                        }
                        if (event.actions.size > 1) {
                            IconButton(onClick = {
                                onChange(event.copy(actions = event.actions.toMutableList().also { it.removeAt(index) }))
                            }) { Icon(Icons.Default.Delete, contentDescription = "Remover ação") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConditionEditor(
    condition: EventCondition,
    objectNames: List<String>,
    modifier: Modifier = Modifier,
    onChange: (EventCondition) -> Unit
) {
    Column(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        EnumChoice(conditionLabel(condition.kind), ConditionKind.entries.map(::conditionLabel)) { label ->
            ConditionKind.entries.firstOrNull { conditionLabel(it) == label }
                ?.let { onChange(condition.copy(kind = it)) }
        }
        when (condition.kind) {
            ConditionKind.KEY_DOWN -> EnumChoice(condition.key, listOf("ESQUERDA", "DIREITA", "CIMA", "BAIXO", "PULAR")) {
                onChange(condition.copy(key = it))
            }
            ConditionKind.TOUCH -> Text("Verdadeira durante um toque na área do jogo.")
            ConditionKind.TOUCH_ON_OBJECT -> {
                if (objectNames.isEmpty()) {
                    Text("Crie um objeto na cena para configurar esta condição.")
                } else {
                    EnumChoice(condition.firstObject.ifBlank { "Selecione um objeto" }, objectNames) {
                        onChange(condition.copy(firstObject = it))
                    }
                }
                Text("Verdadeira quando a área tocada estiver sobre uma instância.")
            }
            ConditionKind.VARIABLE_COMPARE -> {
                OutlinedTextField(condition.variable, { onChange(condition.copy(variable = it)) },
                    label = { Text("Variável (global:, local: ou instance:)") }, modifier = Modifier.fillMaxWidth())
                if (condition.variable.startsWith("instance:")) {
                    if (objectNames.isEmpty()) {
                        Text("Crie um objeto na cena para comparar uma variável da instância.")
                    } else {
                        EnumChoice(condition.firstObject.ifBlank { "Todas as instâncias" },
                            listOf("Todas as instâncias") + objectNames) { selected ->
                            onChange(condition.copy(firstObject = if (selected == "Todas as instâncias") "" else selected))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EnumChoice(condition.compareLabel(), CompareKind.entries.map { it.label() }) { label ->
                        CompareKind.entries.firstOrNull { it.label() == label }
                            ?.let { onChange(condition.copy(compare = it)) }
                    }
                    OutlinedTextField(condition.valueExpression.ifBlank { condition.value.toString() }, {
                        onChange(condition.copy(valueExpression = it))
                    }, label = { Text("Expressão") }, modifier = Modifier.weight(1f))
                }
            }
            ConditionKind.OVERLAP -> {
                EnumChoice(condition.firstObject.ifBlank { "Objeto A" }, objectNames) {
                    onChange(condition.copy(firstObject = it))
                }
                EnumChoice(condition.secondObject.ifBlank { "Objeto B" }, objectNames) {
                    onChange(condition.copy(secondObject = it))
                }
            }
            ConditionKind.EVERY_X_SECONDS -> OutlinedTextField(
                condition.valueExpression.ifBlank { condition.value.toString() },
                { onChange(condition.copy(valueExpression = it)) },
                label = { Text("Intervalo em segundos") },
                modifier = Modifier.fillMaxWidth()
            )
            ConditionKind.ALWAYS, ConditionKind.START_OF_LAYOUT, ConditionKind.ELSE, ConditionKind.UNSUPPORTED -> Unit
        }
        if (condition.kind != ConditionKind.ALWAYS && condition.kind != ConditionKind.UNSUPPORTED) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Inverter condição", Modifier.weight(1f))
                Switch(
                    checked = condition.inverted,
                    onCheckedChange = { onChange(condition.copy(inverted = it)) }
                )
            }
        }
    }
}

@Composable
private fun ActionEditor(
    action: EventAction,
    objectNames: List<String>,
    modifier: Modifier = Modifier,
    onChange: (EventAction) -> Unit
) {
    Column(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        EnumChoice(actionLabel(action.kind), ActionKind.entries.map(::actionLabel)) { label ->
            ActionKind.entries.firstOrNull { actionLabel(it) == label }
                ?.let { onChange(action.copy(kind = it)) }
        }
        when (action.kind) {
            ActionKind.MOVE_BY, ActionKind.SET_POSITION, ActionKind.CREATE_INSTANCE -> {
                val options = if (action.kind == ActionKind.CREATE_INSTANCE) {
                    listOf("Selecione uma instância-modelo") + objectNames
                } else {
                    listOf("Todos os objetos") + objectNames
                }
                val selected = action.targetName.ifBlank { options.first() }
                EnumChoice(selected, options) {
                    onChange(action.copy(targetName = if (it == options.first()) "" else it))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(action.valueExpression.ifBlank { action.value.toString() }, {
                        onChange(action.copy(valueExpression = it))
                    }, label = { Text(if (action.kind == ActionKind.CREATE_INSTANCE) "X" else "X / ΔX") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(action.valueYExpression.ifBlank { action.valueY.toString() }, {
                        onChange(action.copy(valueYExpression = it))
                    }, label = { Text(if (action.kind == ActionKind.CREATE_INSTANCE) "Y" else "Y / ΔY") }, modifier = Modifier.weight(1f))
                }
            }
            ActionKind.SET_VARIABLE, ActionKind.ADD_VARIABLE -> {
                OutlinedTextField(action.variable, { onChange(action.copy(variable = it)) },
                    label = { Text("Variável (global:, local: ou instance:)") }, modifier = Modifier.fillMaxWidth())
                if (action.variable.startsWith("instance:")) {
                    EnumChoice(action.targetName.ifBlank { "Todas as instâncias" },
                        listOf("Todas as instâncias") + objectNames) { selected ->
                        onChange(action.copy(targetName = if (selected == "Todas as instâncias") "" else selected))
                    }
                }
                OutlinedTextField(action.valueExpression.ifBlank { action.value.toString() }, {
                    onChange(action.copy(valueExpression = it))
                }, label = { Text("Expressão") }, modifier = Modifier.fillMaxWidth())
            }
            ActionKind.DESTROY_INSTANCE, ActionKind.ROTATE -> {
                EnumChoice(action.targetName.ifBlank { "Todos os objetos" }, listOf("Todos os objetos") + objectNames) {
                    onChange(action.copy(targetName = if (it == "Todos os objetos") "" else it))
                }
                if (action.kind == ActionKind.ROTATE) {
                    OutlinedTextField(action.valueExpression.ifBlank { action.value.toString() }, {
                        onChange(action.copy(valueExpression = it))
                    }, label = { Text("Ângulo") }, modifier = Modifier.fillMaxWidth())
                }
            }
            ActionKind.CALL_FUNCTION -> OutlinedTextField(
                action.functionName, { onChange(action.copy(functionName = it)) },
                label = { Text("Função") }, modifier = Modifier.fillMaxWidth()
            )
            ActionKind.UNSUPPORTED -> Text(
                "Este bloco veio de uma versão anterior sem semântica conhecida e não será executado.",
                color = Color(0xFF8A5A00)
            )
        }
    }
}

private fun conditionLabel(kind: ConditionKind) = when (kind) {
    ConditionKind.ALWAYS -> "Sempre"
    ConditionKind.START_OF_LAYOUT -> "Ao iniciar a cena"
    ConditionKind.EVERY_X_SECONDS -> "A cada intervalo"
    ConditionKind.KEY_DOWN -> "Tecla/controle pressionado"
    ConditionKind.TOUCH -> "Toque na cena"
    ConditionKind.TOUCH_ON_OBJECT -> "Toque em objeto"
    ConditionKind.VARIABLE_COMPARE -> "Comparar variável"
    ConditionKind.OVERLAP -> "Objetos se sobrepõem"
    ConditionKind.ELSE -> "Senão (evento anterior não atendido)"
    ConditionKind.UNSUPPORTED -> "Condição desconhecida (preservada)"
}

private fun actionLabel(kind: ActionKind) = when (kind) {
    ActionKind.MOVE_BY -> "Mover objeto"
    ActionKind.SET_POSITION -> "Definir posição"
    ActionKind.SET_VARIABLE -> "Definir variável"
    ActionKind.ADD_VARIABLE -> "Somar à variável"
    ActionKind.DESTROY_INSTANCE -> "Destruir objeto"
    ActionKind.CREATE_INSTANCE -> "Criar instância"
    ActionKind.ROTATE -> "Definir rotação"
    ActionKind.CALL_FUNCTION -> "Chamar função"
    ActionKind.UNSUPPORTED -> "Ação desconhecida (preservada)"
}

private fun CompareKind.label() = when (this) {
    CompareKind.EQUAL -> "="
    CompareKind.NOT_EQUAL -> "≠"
    CompareKind.GREATER -> ">"
    CompareKind.LESS -> "<"
}

private fun EventCondition.compareLabel() = compare.label()

private fun flattenEventTree(events: List<EventRow>, depth: Int = 0): List<Pair<EventRow, Int>> =
    events.flatMap { listOf(it to depth) + flattenEventTree(it.children, depth + 1) }

internal fun duplicateEventTree(event: EventRow, nextId: () -> Long): EventRow =
    event.copy(id = nextId(), children = event.children.map { duplicateEventTree(it, nextId) })

private fun insertEventAfter(events: List<EventRow>, targetId: Long, insertion: EventRow): List<EventRow> =
    events.flatMap { event ->
        if (event.id == targetId) {
            listOf(event, insertion)
        } else {
            listOf(event.copy(children = insertEventAfter(event.children, targetId, insertion)))
        }
    }

private fun updateEventTree(
    events: List<EventRow>,
    id: Long,
    transform: (EventRow) -> EventRow
): List<EventRow> = events.map { event ->
    if (event.id == id) transform(event)
    else event.copy(children = updateEventTree(event.children, id, transform))
}

private fun removeEventTree(events: List<EventRow>, id: Long): List<EventRow> =
    events.filterNot { it.id == id }.map { it.copy(children = removeEventTree(it.children, id)) }

private fun findMaximumEventId(events: List<EventRow>): Long =
    events.maxOfOrNull { maxOf(it.id, findMaximumEventId(it.children)) } ?: 0L

private inline fun <reified T : Enum<T>> enumValue(value: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: fallback

@Composable
private fun AnimationEditor(state: EditorState, onImport: () -> Unit, modifier: Modifier = Modifier) {
    val objectRevision = state.objectRevision
    val layoutObjects = state.objects.filter { it.layoutId == state.activeLayoutId }
    var selectedId by remember(state.objects, state.activeLayoutId) {
        mutableIntStateOf(state.selectedId.takeIf { selected -> layoutObjects.any { it.id == selected } }
            ?: layoutObjects.firstOrNull()?.id ?: 0)
    }
    val selectedObject = state.objects.firstOrNull { it.id == selectedId }
    val frames = selectedObject?.animationFrames.orEmpty()
    val images = rememberLoadedImages(state.assets) {}
    var previewPlaying by remember(selectedId) { mutableStateOf(false) }
    var previewFrame by remember(selectedId) { mutableIntStateOf(0) }
    LaunchedEffect(previewPlaying, selectedId, objectRevision) {
        while (previewPlaying && frames.isNotEmpty()) {
            delay((frames[previewFrame.coerceIn(frames.indices)] .durationSeconds * 1000).toLong().coerceAtLeast(20L))
            previewFrame = (previewFrame + 1) % frames.size
        }
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(12.dp)) {
        Text("Editor de animações", style = MaterialTheme.typography.titleLarge)
        if (layoutObjects.isEmpty()) {
            Text("Adicione uma imagem à cena para começar a animação.")
        } else {
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(selectedObject?.let { "${it.name} (#${it.id})" } ?: "Selecione um objeto")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    layoutObjects.forEach { obj ->
                        DropdownMenuItem(
                            text = { Text("${obj.name} (#${obj.id})") },
                            onClick = {
                                selectedId = obj.id
                                state.animationTargetId = obj.id
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        selectedObject?.let { obj ->
            OutlinedTextField(
                value = obj.animationName,
                onValueChange = { obj.animationName = it; state.markObjectsChanged() },
                label = { Text("Nome da animação") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(12.dp))
        val preview = frames.getOrNull(previewFrame)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Card(Modifier.weight(1f).height(160.dp)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    preview?.let { frame ->
                        images[frame.assetId]?.let { Image(it, "Pré-visualização do quadro") }
                    } ?: Text("Sem quadros para pré-visualizar")
                }
            }
            Column {
                IconButton(
                    enabled = frames.isNotEmpty(),
                    onClick = { previewPlaying = !previewPlaying }
                ) {
                    Icon(
                        if (previewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (previewPlaying) "Pausar animação" else "Reproduzir animação"
                    )
                }
                Text("${frames.size} quadros", style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (selectedObject != null) {
            frames.forEachIndexed { index, frame ->
                val asset = state.assets.firstOrNull { it.id == frame.assetId }
                Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        images[frame.assetId]?.let { bitmap ->
                            Image(bitmap, asset?.name ?: "Quadro ${index + 1}", Modifier.size(64.dp))
                        } ?: Icon(Icons.Default.BrokenImage, contentDescription = "Imagem ausente", modifier = Modifier.size(48.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(asset?.name ?: "Recurso ${frame.assetId}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            var duration by remember(frame.durationSeconds) {
                                mutableStateOf(frame.durationSeconds.toString())
                            }
                            OutlinedTextField(
                                value = duration,
                                onValueChange = { text ->
                                    duration = text
                                    text.toFloatOrNull()?.takeIf { it > 0f }?.let { seconds ->
                                        selectedObject.animationFrames = selectedObject.animationFrames.toMutableList()
                                            .also { it[index] = frame.copy(durationSeconds = seconds) }
                                        state.markObjectsChanged()
                                    }
                                },
                                label = { Text("Duração (s)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Column {
                            IconButton(onClick = {
                                selectedObject.animationFrames = selectedObject.animationFrames.toMutableList()
                                    .also { it.add(index + 1, frame.copy()) }
                                state.markObjectsChanged()
                            }) { Icon(Icons.Default.ContentCopy, contentDescription = "Duplicar quadro") }
                            IconButton(
                                enabled = index > 0,
                                onClick = {
                                    selectedObject.animationFrames = selectedObject.animationFrames.toMutableList().also {
                                        val current = it[index]
                                        it[index] = it[index - 1]
                                        it[index - 1] = current
                                    }
                                    state.markObjectsChanged()
                                }
                            ) { Icon(Icons.Default.ArrowUpward, contentDescription = "Mover quadro para cima") }
                            IconButton(
                                enabled = index < frames.lastIndex,
                                onClick = {
                                    selectedObject.animationFrames = selectedObject.animationFrames.toMutableList().also {
                                        val current = it[index]
                                        it[index] = it[index + 1]
                                        it[index + 1] = current
                                    }
                                    state.markObjectsChanged()
                                }
                            ) { Icon(Icons.Default.ArrowDownward, contentDescription = "Mover quadro para baixo") }
                            IconButton(onClick = {
                                selectedObject.animationFrames = selectedObject.animationFrames.toMutableList()
                                    .also { it.removeAt(index) }
                                state.markObjectsChanged()
                            }) { Icon(Icons.Default.Delete, contentDescription = "Excluir quadro") }
                        }
                    }
                }
            }
            if (frames.isEmpty()) Text("Nenhum quadro. Importe imagens para criar a sequência.")
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (selectedObject == null) state.notice = "Selecione ou crie um objeto antes de adicionar quadros."
                else {
                    state.animationTargetId = selectedObject.id
                    onImport()
                }
            },
            enabled = selectedObject != null,
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Icon(Icons.Default.AddPhotoAlternate, null); Spacer(Modifier.width(6.dp))
            Text("Importar quadro")
        }
    }
}

@Composable
private fun GamePreviewScreen(
    runtime: GameRuntime,
    assets: List<ImageAsset>,
    debug: Boolean,
    onExit: () -> Unit
) {
    val images = rememberLoadedImages(assets) {}
    val focusRequester = remember { FocusRequester() }
    var left by remember { mutableStateOf(false) }
    var right by remember { mutableStateOf(false) }
    var up by remember { mutableStateOf(false) }
    var down by remember { mutableStateOf(false) }
    var jump by remember { mutableStateOf(false) }
    var touchPressed by remember { mutableStateOf(false) }
    var touchX by remember { mutableFloatStateOf(0f) }
    var touchY by remember { mutableFloatStateOf(0f) }
    val latestInput by rememberUpdatedState(RuntimeInput(left, right, up, down, jump, touchPressed, touchX, touchY))
    var frameTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    LaunchedEffect(touchPressed) {
        if (touchPressed) {
            delay(80)
            touchPressed = false
        }
    }

    LaunchedEffect(runtime) {
        var previousFrame = 0L
        while (true) {
            val now = withFrameNanos { it }
            val delta = if (previousFrame == 0L) 1f / 60f else (now - previousFrame) / 1_000_000_000f
            previousFrame = now
            runtime.step(delta, latestInput)
            frameTick = runtime.revision
        }
    }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().focusRequester(focusRequester).focusable()
            .onPreviewKeyEvent { event ->
                val pressed = event.type == KeyEventType.KeyDown
                val handled = when (event.key) {
                    Key.DirectionLeft, Key.A -> { left = pressed; true }
                    Key.DirectionRight, Key.D -> { right = pressed; true }
                    Key.DirectionUp, Key.W -> { up = pressed; true }
                    Key.DirectionDown, Key.S -> { down = pressed; true }
                    Key.Spacebar -> { jump = pressed; true }
                    else -> false
                }
                handled
            }
    ) {
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF202830)).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (debug) "Depuração da cena" else "Executar cena", Modifier.weight(1f),
                color = Color.White, style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onExit) { Text("Fechar", color = Color.White) }
        }
        Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF111820))) {
            Canvas(
                Modifier.fillMaxSize().pointerInput(runtime) {
                    detectTapGestures { offset ->
                        val scale = min(
                            size.width / runtime.width.toFloat(),
                            size.height / runtime.height.toFloat()
                        ) * runtime.cameraZoom
                        if (scale > 0f) {
                            touchX = runtime.cameraX + (offset.x - size.width / 2f) / scale
                            touchY = runtime.cameraY + (offset.y - size.height / 2f) / scale
                        }
                        touchPressed = true
                    }
                }
            ) {
                val currentFrame = frameTick
                val scale = min(size.width / runtime.width, size.height / runtime.height) * runtime.cameraZoom
                val sceneWidth = runtime.width * scale
                val sceneHeight = runtime.height * scale
                val viewportCenter = Offset(size.width / 2f, size.height / 2f)
                val sceneOrigin = Offset(
                    viewportCenter.x - runtime.cameraX * scale,
                    viewportCenter.y - runtime.cameraY * scale
                )
                drawRect(Color(0xFF303A43), topLeft = sceneOrigin, size = Size(sceneWidth, sceneHeight))
                runtime.objects.forEach { obj ->
                    val layer = runtime.layers[obj.layerId]
                    if (layer?.visible == false) return@forEach
                    val left = viewportCenter.x + (obj.x - runtime.cameraX * (layer?.scrollX ?: 1f)) * scale
                    val top = viewportCenter.y + (obj.y - runtime.cameraY * (layer?.scrollY ?: 1f)) * scale
                    val width = obj.w * scale
                    val height = obj.h * scale
                    val image = images[obj.assetId]
                    rotate(obj.rotation, Offset(left + width / 2f, top + height / 2f)) {
                        if (obj.plugin == "Sprite" && image != null) {
                            drawImage(
                                image,
                                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                                dstSize = IntSize(width.roundToInt().coerceAtLeast(1), height.roundToInt().coerceAtLeast(1)),
                                alpha = layer?.opacity?.coerceIn(0f, 1f) ?: 1f
                            )
                        } else if (obj.plugin == "Text" && obj.text.isNotEmpty()) {
                            drawContext.canvas.nativeCanvas.drawText(
                                obj.text,
                                left,
                                top + (18f * scale),
                                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = android.graphics.Color.WHITE
                                    textSize = 18f * scale
                                    alpha = ((layer?.opacity ?: 1f).coerceIn(0f, 1f) * 255).toInt()
                                }
                            )
                        }
                    }
                }
                if (debug) {
                    val native = drawContext.canvas.nativeCanvas
                    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.WHITE
                        textSize = 14.dp.toPx()
                    }
                    val lines = buildList {
                        add("FPS: ${runtime.fps.toInt()}  Ciclo: $currentFrame")
                        add("Instâncias: ${runtime.objects.size}")
                        add("Variáveis: ${runtime.globalVariables}")
                        add("Eventos ativos: ${runtime.eventStates.count { it.value }}")
                        runtime.eventStates.forEach { (id, passed) -> add("Evento $id: ${if (passed) "OK" else "não atendido"}") }
                        runtime.lastError?.let { add(it) }
                    }
                    drawRect(Color.Black.copy(alpha = 0.72f), Offset(8.dp.toPx(), 8.dp.toPx()),
                        Size(300.dp.toPx(), (lines.size * 20 + 12).dp.toPx()))
                    lines.forEachIndexed { index, line ->
                        textPaint.color = if (runtime.lastError != null && index == lines.lastIndex) {
                            android.graphics.Color.rgb(255, 138, 128)
                        } else android.graphics.Color.WHITE
                        native.drawText(line, 14.dp.toPx(), (24 + index * 20).dp.toPx(), textPaint)
                    }
                }
            }
        }
        Text(
            "Toque na cena para acionar eventos de toque. Teclado: setas/WASD e espaço.",
            Modifier.fillMaxWidth().background(Color(0xFF202830)).padding(horizontal = 12.dp, vertical = 6.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun ProjectManager(
    state: EditorState,
    modifier: Modifier = Modifier,
    onRunProject: () -> Unit,
    onDebugLayout: () -> Unit,
    onNewProject: () -> Unit
) {
    var name by remember(state.projectName) { mutableStateOf(state.projectName) }
    var confirmNewProject by remember { mutableStateOf(false) }
    if (confirmNewProject) {
        AlertDialog(
            onDismissRequest = { confirmNewProject = false },
            title = { Text("Criar projeto novo?") },
            text = { Text("O projeto aberto será substituído. Salve as alterações antes de continuar.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmNewProject = false
                    onNewProject()
                }) { Text("Criar projeto") }
            },
            dismissButton = {
                TextButton(onClick = { confirmNewProject = false }) { Text("Cancelar") }
            }
        )
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(12.dp)) {
        Text("Gerenciador de projetos", style = MaterialTheme.typography.titleLarge)
        OutlinedButton(onClick = { confirmNewProject = true }, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(6.dp))
            Text("Novo projeto")
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; state.projectName = it; state.modified = true },
            label = { Text("Nome do projeto") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Text("Cenas", style = MaterialTheme.typography.titleSmall)
        EnumChoice(
            state.layouts.firstOrNull { it.id == state.startLayoutId }?.name ?: "Cena inicial",
            state.layouts.map { it.name }
        ) { name ->
            state.startLayoutId = state.layouts.firstOrNull { it.name == name }?.id ?: state.startLayoutId
            state.markProjectChanged()
        }
        state.layouts.forEach { layout ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = state.activeLayoutId == layout.id,
                            onClick = {
                                state.activeLayoutId = layout.id
                                state.width = layout.width
                                state.height = layout.height
                                state.selectedId = state.objects.firstOrNull { it.layoutId == layout.id }?.id ?: 0
                            },
                            label = { Text(layout.name) },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            if (state.layouts.size > 1) {
                                state.objects = state.objects.filterNot { it.layoutId == layout.id }
                                state.events = state.events.filterNot { it.eventSheetId == layout.eventSheetId }
                                state.layers = state.layers.filterNot { it.layoutId == layout.id }
                                state.layouts = state.layouts.filterNot { it.id == layout.id }
                                val next = state.layouts.firstOrNull()
                                if (next != null) {
                                    state.activeLayoutId = next.id
                                    state.width = next.width
                                    state.height = next.height
                                    if (state.startLayoutId == layout.id) state.startLayoutId = next.id
                                }
                                state.markProjectChanged()
                            } else {
                                state.notice = "Deve existir ao menos uma cena."
                            }
                        }) { Icon(Icons.Default.Delete, contentDescription = "Excluir cena") }
                    }
                    OutlinedTextField(
                        value = layout.name,
                        onValueChange = {
                            state.layouts = state.layouts.map { current ->
                                if (current.id == layout.id) current.copy(name = it) else current
                            }
                            state.markProjectChanged()
                        },
                        label = { Text("Nome da cena") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        PropertyNumber("Largura", layout.width.toFloat(), Modifier.weight(1f)) {
                            val width = it.toInt().coerceAtLeast(64)
                            state.layouts = state.layouts.map { current ->
                                if (current.id == layout.id) current.copy(width = width) else current
                            }
                            if (state.activeLayoutId == layout.id) state.width = width
                            state.markProjectChanged()
                        }
                        PropertyNumber("Altura", layout.height.toFloat(), Modifier.weight(1f)) {
                            val height = it.toInt().coerceAtLeast(64)
                            state.layouts = state.layouts.map { current ->
                                if (current.id == layout.id) current.copy(height = height) else current
                            }
                            if (state.activeLayoutId == layout.id) state.height = height
                            state.markProjectChanged()
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = {
            val n = state.layouts.size + 1
            val id = (state.layouts.maxOfOrNull { it.id } ?: 0) + 1
            val sheetId = (state.layouts.maxOfOrNull { it.eventSheetId } ?: 0) + 1
            val layout = LayoutInfo(id, "Cena $n", state.width, state.height, sheetId)
            state.layouts = state.layouts + layout
            val layerId = (state.layers.maxOfOrNull { it.id } ?: 0) + 1
            state.layers = state.layers + LayerModel(layerId, "Principal", layoutId = id)
            state.activeLayoutId = id
            state.selectedId = 0
            state.markProjectChanged()
        }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Adicionar cena") }
        Spacer(Modifier.height(12.dp))
        Text("Área de trabalho: ${state.width} × ${state.height}")
        Text("Objetos: ${state.objects.size}  Eventos: ${state.events.size}",
            style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRunProject, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Executar projeto")
            }
            OutlinedButton(onClick = onDebugLayout, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Default.BugReport, null); Spacer(Modifier.width(6.dp)); Text("Depurar cena")
            }
        }
    }
}

@Composable
private fun LayoutEditor(state: EditorState, onObjectLongPress: () -> Unit) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val activeLayout = remember(state.layouts, state.activeLayoutId) {
        state.layouts.firstOrNull { it.id == state.activeLayoutId } ?: state.layouts.firstOrNull()
    }
    val layoutWidth = activeLayout?.width ?: state.width
    val layoutHeight = activeLayout?.height ?: state.height
    val layoutObjects = remember(state.objects, state.objectRevision, state.activeLayoutId) {
        state.objects.filter { it.layoutId == state.activeLayoutId }
    }
    val latestZoom by rememberUpdatedState(zoom)
    val latestPan by rememberUpdatedState(pan)
    val decodedImages = rememberLoadedImages(state.assets) { error ->
        state.notice = "Falha ao carregar imagem: $error"
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFFE8EDF2)).padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var layoutMenuExpanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(
                    onClick = { layoutMenuExpanded = true },
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Text(activeLayout?.name ?: "Cena")
                }
                DropdownMenu(expanded = layoutMenuExpanded, onDismissRequest = { layoutMenuExpanded = false }) {
                    state.layouts.forEach { layout ->
                        DropdownMenuItem(
                            text = { Text(layout.name) },
                            onClick = {
                                state.activeLayoutId = layout.id
                                state.width = layout.width
                                state.height = layout.height
                                state.selectedId = state.objects.firstOrNull { it.layoutId == layout.id }?.id ?: 0
                                zoom = 1f
                                pan = Offset.Zero
                                layoutMenuExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = { zoom = (zoom / 1.25f).coerceAtLeast(0.25f) }) {
                Icon(Icons.Default.ZoomOut, contentDescription = "Diminuir zoom")
            }
            Text("${(zoom * 100).toInt()}%", Modifier.padding(horizontal = 2.dp),
                style = MaterialTheme.typography.labelLarge)
            IconButton(onClick = { zoom = (zoom * 1.25f).coerceAtMost(8f) }) {
                Icon(Icons.Default.ZoomIn, contentDescription = "Aumentar zoom")
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { pan = Offset.Zero }) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = "Centralizar cena")
            }
            IconButton(onClick = { zoom = 1f; pan = Offset.Zero }) {
                Icon(Icons.Default.FitScreen, contentDescription = "Ajustar cena à tela")
            }
        }
        Canvas(
            modifier = Modifier.fillMaxSize().background(Color(0xFF252A30))
                .pointerInput(layoutWidth, layoutHeight, state.activeLayoutId) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val viewport = Size(size.width.toFloat(), size.height.toFloat())
                        var gestureZoom = latestZoom
                        var gesturePan = latestPan
                        var currentTransform = SceneTransform(layoutWidth, layoutHeight, viewport, gestureZoom, gesturePan)
                        val startPoint = down.position
                        val initialHit = layoutObjects.lastOrNull { obj ->
                            currentTransform.screenRect(obj).contains(startPoint)
                        }
                        if (initialHit != null) state.selectedId = initialHit.id
                        val startRect = initialHit?.let(currentTransform::screenRect)
                        val resizeHandle = startRect?.let {
                            val handle = 28.dp.toPx()
                            down.position.x >= it.right - handle && down.position.y >= it.bottom - handle
                        } ?: false
                        var dragStarted = false
                        var multiTouchUsed = false
                        var longPressHandled = false
                        var lastPointerPosition = startPoint
                        val activePointer = down.id
                        val touchSlop = viewConfiguration.touchSlop

                        while (true) {
                            val timeout = (down.uptimeMillis + viewConfiguration.longPressTimeoutMillis -
                                android.os.SystemClock.uptimeMillis()).coerceAtLeast(0)
                            val timedEvent = if (initialHit != null && !resizeHandle && !dragStarted &&
                                !multiTouchUsed && !longPressHandled
                            ) {
                                withTimeoutOrNull(timeout) { awaitPointerEvent() }
                            } else {
                                awaitPointerEvent()
                            }
                            if (timedEvent == null) {
                                longPressHandled = true
                                onObjectLongPress()
                                continue
                            }
                            val event = timedEvent
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) break
                            if (pressed.size >= 2) {
                                multiTouchUsed = true
                                val first = pressed[0]
                                val second = pressed[1]
                                val oldFocus = (first.previousPosition + second.previousPosition) / 2f
                                val newFocus = (first.position + second.position) / 2f
                                val oldDistance = (first.previousPosition - second.previousPosition).getDistance()
                                val newDistance = (first.position - second.position).getDistance()
                                if (oldDistance > 0f && newDistance > 0f) {
                                    val factor = (newDistance / oldDistance).coerceIn(0.75f, 1.35f)
                                    val nextZoom = (gestureZoom * factor).coerceIn(0.25f, 8f)
                                    val appliedFactor = nextZoom / gestureZoom
                                    gesturePan = zoomPanAround(gesturePan, viewport, oldFocus, newFocus, appliedFactor)
                                    gestureZoom = nextZoom
                                    pan = gesturePan
                                    zoom = gestureZoom
                                } else {
                                    gesturePan += newFocus - oldFocus
                                    pan = gesturePan
                                }
                                event.changes.forEach { if (it.pressed) it.consume() }
                                continue
                            }
                            val change = pressed.firstOrNull { it.id == activePointer }
                            if (change != null && !multiTouchUsed) {
                                if (!dragStarted && (change.position - startPoint).getDistance() > touchSlop) {
                                    dragStarted = true
                                }
                                if (dragStarted) {
                                    currentTransform = SceneTransform(layoutWidth, layoutHeight, viewport, gestureZoom, gesturePan)
                                    val delta = if (lastPointerPosition == startPoint) {
                                        change.position - startPoint
                                    } else {
                                        change.position - lastPointerPosition
                                    }
                                    if (initialHit == null) {
                                        gesturePan += delta
                                        pan = gesturePan
                                    } else if (resizeHandle) {
                                        initialHit.w = (initialHit.w + delta.x / currentTransform.scale).coerceAtLeast(8f)
                                        initialHit.h = (initialHit.h + delta.y / currentTransform.scale).coerceAtLeast(8f)
                                        state.markObjectsChanged()
                                    } else {
                                        initialHit.x += delta.x / currentTransform.scale
                                        initialHit.y += delta.y / currentTransform.scale
                                        state.markObjectsChanged()
                                    }
                                    lastPointerPosition = change.position
                                }
                                change.consume()
                            }
                        }
                    }
                }
        ) {
            state.objectRevision
            val transform = SceneTransform(layoutWidth, layoutHeight, Size(size.width, size.height), zoom, pan)
            drawRect(Color(0xFFF9FBFC), topLeft = transform.origin, size = transform.sceneSize)
            for (obj in layoutObjects) {
                if (!obj.visible || state.layers.none {
                        it.id == obj.layerId && it.layoutId == state.activeLayoutId && it.visible
                    }) continue
                val rect = transform.screenRect(obj)
                rotate(obj.rotation, rect.center) {
                    val image = decodedImages[obj.assetId]
                    if (image != null) {
                        drawImage(
                            image,
                            dstOffset = IntOffset(rect.left.toInt(), rect.top.toInt()),
                            dstSize = IntSize(rect.width.toInt().coerceAtLeast(1), rect.height.toInt().coerceAtLeast(1))
                        )
                    } else {
                        drawRect(
                            color = if (obj.id == state.selectedId) Color(0xFF2F88C8) else Color(0xFF8CBBD9),
                            topLeft = Offset(rect.left, rect.top), size = Size(rect.width, rect.height)
                        )
                        if (obj.plugin == "Text") {
                            drawContext.canvas.nativeCanvas.drawText(
                                obj.text,
                                rect.left + 4.dp.toPx(),
                                rect.top + 20.dp.toPx(),
                                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = android.graphics.Color.DKGRAY
                                    textSize = 14.dp.toPx()
                                }
                            )
                        }
                    }
                    if (obj.id == state.selectedId) {
                        drawRect(
                            color = Color(0xFFFFA000),
                            topLeft = Offset(rect.left, rect.top),
                            size = Size(rect.width, rect.height),
                            style = Stroke(2.dp.toPx())
                        )
                        drawRect(
                            color = Color(0xFFFFA000),
                            topLeft = Offset(rect.right - 8.dp.toPx(), rect.bottom - 8.dp.toPx()),
                            size = Size(16.dp.toPx(), 16.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberLoadedImages(
    assets: List<ImageAsset>,
    onError: (String) -> Unit
): Map<Int, ImageBitmap> {
    var images by remember { mutableStateOf<Map<Int, ImageBitmap>>(emptyMap()) }
    var imageSources by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    val imageContent = remember(assets) { assets.map { it.id to it.base64 } }
    val latestImages by rememberUpdatedState(images)
    LaunchedEffect(imageContent) {
        val requestedSources = imageContent.toMap()
        val retained = images.filter { (id, _) -> imageSources[id] == requestedSources[id] }
        images.filterKeys { it !in retained }.values.forEach { it.asAndroidBitmap().recycle() }
        val missing = assets.filterNot { it.id in retained }
        val result = withContext(Dispatchers.IO) {
            val decoded = mutableMapOf<Int, ImageBitmap>()
            val errors = mutableListOf<String>()
            missing.forEach { asset ->
                try {
                    val bytes = Base64.decode(asset.base64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap == null) errors += asset.name
                    else decoded[asset.id] = bitmap.asImageBitmap()
                } catch (_: IllegalArgumentException) {
                    errors += asset.name
                }
            }
            decoded to errors
        }
        images = retained + result.first
        imageSources = requestedSources
        result.second.firstOrNull()?.let(onError)
    }
    DisposableEffect(Unit) {
        onDispose {
            latestImages.values.forEach { it.asAndroidBitmap().recycle() }
        }
    }
    return images
}

internal data class SceneTransform(
    val sceneWidth: Int,
    val sceneHeight: Int,
    val viewport: Size,
    val zoom: Float,
    val pan: Offset
) {
    val scale: Float = min(viewport.width / sceneWidth, viewport.height / sceneHeight) * zoom
    val sceneSize = Size(sceneWidth * scale, sceneHeight * scale)
    val origin = Offset(
        (viewport.width - sceneSize.width) / 2f + pan.x,
        (viewport.height - sceneSize.height) / 2f + pan.y
    )

    fun screenFromWorld(point: Offset) = origin + point * scale

    fun screenRect(obj: LayoutObject) = androidx.compose.ui.geometry.Rect(
        origin.x + obj.x * scale,
        origin.y + obj.y * scale,
        origin.x + (obj.x + obj.w) * scale,
        origin.y + (obj.y + obj.h) * scale
    )
}

internal fun pluginForType(type: String) = when (type) {
    "Imagem", "Sprite" -> "Sprite"
    "Fundo quadriculado", "Tiled Background" -> "Tiled Background"
    "Texto", "Text" -> "Text"
    else -> type
}

private fun pluginDisplay(plugin: String) = when (plugin) {
    "Sprite" -> "Imagem (Sprite)"
    "Tiled Background" -> "Fundo quadriculado"
    "Text" -> "Texto"
    else -> plugin
}

internal fun zoomPanAround(
    pan: Offset,
    viewport: Size,
    oldFocus: Offset,
    newFocus: Offset,
    scaleFactor: Float
): Offset {
    val center = Offset(viewport.width / 2f, viewport.height / 2f)
    return newFocus - center + (center + pan - oldFocus) * scaleFactor
}
