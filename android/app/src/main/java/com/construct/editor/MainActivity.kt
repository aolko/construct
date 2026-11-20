package com.construct.editor

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.min

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
    var h: Float
)

data class EventRow(var condition: String, var action: String)

private data class ProjectSnapshot(
    val name: String,
    val width: Int,
    val height: Int,
    val layouts: List<String>,
    val objectTypes: List<String>,
    val objects: List<LayoutObject>,
    val events: List<EventRow>
)

class EditorState {
    var projectName by mutableStateOf("Projeto sem título")
    var width by mutableStateOf(640)
    var height by mutableStateOf(480)
    var objectTypes by mutableStateOf(listOf("Imagem", "Fundo quadriculado", "Texto"))
    var layouts by mutableStateOf(listOf("Cena 1"))
    var objects by mutableStateOf(listOf(
        LayoutObject(1, "Imagem 1", "Sprite", 160f, 120f, 96f, 96f),
        LayoutObject(2, "Texto 1", "Text", 320f, 80f, 180f, 48f)
    ))
    var events by mutableStateOf(listOf(
        EventRow("Sistema: Início da cena", "Sistema: Definir valor")
    ))
    var selectedId by mutableStateOf(1)
    var modified by mutableStateOf(false)
    var notice by mutableStateOf("Novo projeto")
    var objectRevision by mutableIntStateOf(0)

    fun markObjectsChanged() {
        objectRevision++
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
    private val importImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val objectId = (state.objects.maxOfOrNull { o -> o.id } ?: 0) + 1
            state.objects = state.objects + LayoutObject(objectId, "Imagem $objectId", "Sprite", 220f, 180f, 128f, 128f)
            state.selectedId = objectId
            state.modified = true
            state.notice = "Imagem importada: ${it.lastPathSegment}"
        }
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
        objectTypes = state.objectTypes.toList(),
        objects = state.objects.map { it.copy() },
        events = state.events.map { it.copy() }
    )

    private fun projectJson(project: ProjectSnapshot): String {
        val objects = JSONArray()
        project.objects.forEach { o ->
            objects.put(JSONObject()
                .put("id", o.id).put("name", o.name).put("plugin", o.plugin)
                .put("x", o.x).put("y", o.y).put("w", o.w).put("h", o.h))
        }
        val events = JSONArray()
        project.events.forEach { e -> events.put(JSONObject().put("condition", e.condition).put("action", e.action)) }
        return JSONObject()
            .put("format", "ConstructClassicAndroid")
            .put("formatVersion", 1)
            .put("originalCapCompatible", false)
            .put("name", project.name)
            .put("width", project.width).put("height", project.height)
            .put("layouts", JSONArray(project.layouts))
            .put("objectTypes", JSONArray(project.objectTypes))
            .put("objects", objects).put("events", events)
            .toString(2)
    }

    private fun parseNativeProject(json: String): ProjectSnapshot {
        val root = JSONObject(json)
        val layouts = root.optJSONArray("layouts")?.let { values ->
            List(values.length()) { values.getString(it) }
        } ?: listOf("Cena 1")
        val objectTypes = root.optJSONArray("objectTypes")?.let { values ->
            List(values.length()) { values.getString(it) }
        } ?: listOf("Imagem", "Fundo quadriculado", "Texto")
        val loadedObjects = mutableListOf<LayoutObject>()
        val array = root.optJSONArray("objects") ?: JSONArray()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            loadedObjects += LayoutObject(o.optInt("id"), o.optString("name"), o.optString("plugin"),
                o.optDouble("x").toFloat(), o.optDouble("y").toFloat(),
                o.optDouble("w").toFloat(), o.optDouble("h").toFloat())
        }
        val loadedEvents = mutableListOf<EventRow>()
        val eventArray = root.optJSONArray("events") ?: JSONArray()
        for (i in 0 until eventArray.length()) {
            val event = eventArray.getJSONObject(i)
            loadedEvents += EventRow(event.optString("condition"), event.optString("action"))
        }
        return ProjectSnapshot(
            name = root.optString("name", "Projeto importado"),
            width = root.optInt("width", 640),
            height = root.optInt("height", 480),
            layouts = layouts,
            objectTypes = objectTypes,
            objects = loadedObjects,
            events = loadedEvents
        )
    }

    private fun loadNativeProject(project: ProjectSnapshot) {
        state.projectName = project.name
        state.width = project.width
        state.height = project.height
        state.layouts = project.layouts
        state.objectTypes = project.objectTypes
        state.objects = project.objects
        state.events = project.events
        state.selectedId = state.objects.firstOrNull()?.id ?: 0
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
    var showMobilePanel by remember { mutableStateOf(false) }
    var showDesktopPanel by remember { mutableStateOf(true) }
    val compactScreen = LocalConfiguration.current.screenWidthDp < 840
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
                    if (tab == 0) {
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
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            val compact = maxWidth < 840.dp
            val panelWidth = if (maxWidth >= 1000.dp) 300.dp else 250.dp
            if (tab == 0) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        LayoutEditor(state) {
                            panel = "Propriedades"
                            if (compact) showMobilePanel = true else showDesktopPanel = true
                        }
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
                    3 -> ProjectManager(state, Modifier.fillMaxSize())
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
            Surface(
                modifier = modifier.fillMaxHeight(),
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row {
                        listOf("Objetos", "Propriedades").forEach { label ->
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
                                val obj = LayoutObject(id, "Imagem $id", "Sprite", 200f, 160f, 96f, 96f)
                                state.objects = state.objects + obj
                                state.selectedId = id
                                state.markObjectsChanged()
                            }) { Icon(Icons.Default.Add, null) }
                            IconButton(onClick = onImport) { Icon(Icons.Default.Image, null) }
                        }
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(state.objectTypes) { type ->
                                TextButton(onClick = {
                                    val id = (state.objects.maxOfOrNull { it.id } ?: 0) + 1
                                    val obj = LayoutObject(id, "$type $id", pluginForType(type), 180f, 140f, 128f, 96f)
                                    state.objects = state.objects + obj
                                    state.selectedId = id
                                    state.markObjectsChanged()
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.Category, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(type, modifier = Modifier.weight(1f))
                                }
                            }
                        }
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
    var condition by remember { mutableStateOf("Sistema: A cada quadro") }
    var action by remember { mutableStateOf("Sistema: Somar 1 ao contador") }
    LazyColumn(modifier.fillMaxSize().imePadding().padding(horizontal = 12.dp)) {
        item {
            Text("Editor de eventos", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 12.dp))
        }
        items(state.events) { event ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Condição", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(event.condition)
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    Text("Ação", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(event.action)
                }
            }
        }
        item {
            OutlinedTextField(value = condition, onValueChange = { condition = it },
                label = { Text("Condição") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(value = action, onValueChange = { action = it },
                label = { Text("Ação") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Button(onClick = {
                state.events = state.events + EventRow(condition, action)
                state.modified = true
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Adicionar evento")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AnimationEditor(state: EditorState, onImport: () -> Unit, modifier: Modifier = Modifier) {
    var frameName by remember { mutableStateOf("Parado") }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(12.dp)) {
        Text("Editor de animações", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = frameName, onValueChange = { frameName = it },
            label = { Text("Nome da animação") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            listOf("Quadro 1", "Quadro 2", "Quadro 3").forEach { frame ->
                Card(modifier = Modifier.size(120.dp).padding(4.dp)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(frame)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onImport, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.Default.AddPhotoAlternate, null); Spacer(Modifier.width(6.dp))
            Text("Importar quadro")
        }
        Spacer(Modifier.height(8.dp))
        Text("A importação de imagem está disponível. A reprodução de animações no runtime ainda não foi portada.",
            color = Color(0xFF8A5A00))
    }
}

@Composable
private fun ProjectManager(state: EditorState, modifier: Modifier = Modifier) {
    var name by remember(state.projectName) { mutableStateOf(state.projectName) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(12.dp)) {
        Text("Gerenciador de projetos", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; state.projectName = it; state.modified = true },
            label = { Text("Nome do projeto") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Text("Cenas", style = MaterialTheme.typography.titleSmall)
        state.layouts.forEach { layout ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Layers, null)
                    Spacer(Modifier.width(8.dp))
                    Text(layout)
                }
            }
        }
        OutlinedButton(onClick = {
            val n = state.layouts.size + 1
            state.layouts = state.layouts + "Cena $n"
            state.modified = true
        }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Adicionar cena") }
        Spacer(Modifier.height(12.dp))
        Text("Área de trabalho: ${state.width} × ${state.height}")
        Text("Objetos: ${state.objects.size}  Eventos: ${state.events.size}",
            style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LayoutEditor(state: EditorState, onObjectLongPress: () -> Unit) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val latestZoom by rememberUpdatedState(zoom)
    val latestPan by rememberUpdatedState(pan)
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFFE8EDF2)).padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                .pointerInput(state.width, state.height) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val viewport = Size(size.width.toFloat(), size.height.toFloat())
                        var gestureZoom = latestZoom
                        var gesturePan = latestPan
                        var currentTransform = SceneTransform(state.width, state.height, viewport, gestureZoom, gesturePan)
                        val startPoint = down.position
                        val initialHit = state.objects.lastOrNull { obj ->
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
                                    currentTransform = SceneTransform(state.width, state.height, viewport, gestureZoom, gesturePan)
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
            val transform = SceneTransform(state.width, state.height, Size(size.width, size.height), zoom, pan)
            drawRect(Color(0xFFF9FBFC), topLeft = transform.origin, size = transform.sceneSize)
            for (obj in state.objects) {
                val rect = transform.screenRect(obj)
                drawRect(
                    color = if (obj.id == state.selectedId) Color(0xFF2F88C8) else Color(0xFF8CBBD9),
                    topLeft = Offset(rect.left, rect.top), size = Size(rect.width, rect.height)
                )
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
