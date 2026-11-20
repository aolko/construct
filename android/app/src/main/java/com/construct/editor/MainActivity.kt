package com.construct.editor

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

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

class EditorState {
    var projectName by mutableStateOf("Untitled project")
    var width by mutableStateOf(640)
    var height by mutableStateOf(480)
    var objectTypes by mutableStateOf(listOf("Sprite", "Tiled Background", "Text"))
    var layouts by mutableStateOf(listOf("Layout 1"))
    var objects by mutableStateOf(listOf(
        LayoutObject(1, "Sprite 1", "Sprite", 160f, 120f, 96f, 96f),
        LayoutObject(2, "Text 1", "Text", 320f, 80f, 180f, 48f)
    ))
    var events by mutableStateOf(listOf(
        EventRow("System: Start of layout", "System: Set value")
    ))
    var selectedId by mutableStateOf(1)
    var modified by mutableStateOf(false)
    var notice by mutableStateOf("New project")
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
            state.objects = state.objects + LayoutObject(objectId, "Image $objectId", "Sprite", 220f, 180f, 128f, 128f)
            state.selectedId = objectId
            state.modified = true
            state.notice = "Image imported: ${it.lastPathSegment}"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    private fun openUri(uri: Uri) {
        val temp = File(cacheDir, "opened.cap")
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            }
            val inspection = CoreNative.inspectCap(temp.absolutePath)
            if (!inspection.contains("\"valid\":true")) {
                val json = temp.readText(Charsets.UTF_8)
                if (json.startsWith("{")) loadNativeProject(json)
                else state.notice = "Unsupported or corrupt .cap"
                return
            }
            CoreNative.openCap(temp.absolutePath, temp.absolutePath)
            state.projectName = uri.lastPathSegment?.substringBeforeLast('.') ?: "Imported project"
            state.notice = "Opened .cap: raw CArchive payload preserved"
            state.modified = false
        } catch (error: Exception) {
            state.notice = "Open failed: ${error.message}"
        }
    }

    private fun saveUri(uri: Uri) {
        try {
            val json = projectJson()
            contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(json) }
            state.modified = false
            state.notice = "Saved project"
        } catch (error: Exception) {
            state.notice = "Save failed: ${error.message}"
        }
    }

    private fun projectJson(): String {
        val objects = JSONArray()
        state.objects.forEach { o ->
            objects.put(JSONObject()
                .put("id", o.id).put("name", o.name).put("plugin", o.plugin)
                .put("x", o.x).put("y", o.y).put("w", o.w).put("h", o.h))
        }
        val events = JSONArray()
        state.events.forEach { e -> events.put(JSONObject().put("condition", e.condition).put("action", e.action)) }
        return JSONObject()
            .put("format", "ConstructClassicAndroid")
            .put("formatVersion", 1)
            .put("originalCapCompatible", false)
            .put("name", state.projectName)
            .put("width", state.width).put("height", state.height)
            .put("layouts", JSONArray(state.layouts))
            .put("objectTypes", JSONArray(state.objectTypes))
            .put("objects", objects).put("events", events)
            .toString(2)
    }

    private fun loadNativeProject(json: String) {
        val root = JSONObject(json)
        state.projectName = root.optString("name", "Imported project")
        state.width = root.optInt("width", 640)
        state.height = root.optInt("height", 480)
        val loadedObjects = mutableListOf<LayoutObject>()
        val array = root.optJSONArray("objects") ?: JSONArray()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            loadedObjects += LayoutObject(o.optInt("id"), o.optString("name"), o.optString("plugin"),
                o.optDouble("x").toFloat(), o.optDouble("y").toFloat(),
                o.optDouble("w").toFloat(), o.optDouble("h").toFloat())
        }
        if (loadedObjects.isNotEmpty()) state.objects = loadedObjects
        state.notice = "Opened Android project"
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
    var panel by remember { mutableStateOf("Objects") }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.projectName, style = MaterialTheme.typography.titleMedium)
                        Text("${if (state.modified) "*" else ""}${state.notice}",
                            style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { tab = if (tab == 0) 1 else 0 }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Switch view")
                    }
                },
                actions = {
                    IconButton(onClick = onOpen) { Icon(Icons.Default.FolderOpen, contentDescription = "Open") }
                    IconButton(onClick = onSave) { Icon(Icons.Default.Save, contentDescription = "Save") }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.GridOn, null) }, label = { Text("Layout") })
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.AccountTree, null) }, label = { Text("Events") })
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.Movie, null) }, label = { Text("Animations") })
                NavigationBarItem(
                    selected = tab == 3, onClick = { tab = 3 },
                    icon = { Icon(Icons.Default.PhotoLibrary, null) }, label = { Text("Project") })
            }
        }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (tab == 0) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    LayoutEditor(state)
                }
                PropertiesPanel(state, panel, onPanelChange = { panel = it }, onImport)
            } else {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (tab) {
                        1 -> EventEditor(state)
                        2 -> AnimationEditor(state, onImport)
                        3 -> ProjectManager(state)
                    }
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
            onImport: () -> Unit
        ) {
            val selected = state.objects.firstOrNull { it.id == state.selectedId }
            Surface(
                modifier = Modifier.widthIn(180.dp, 320.dp).fillMaxHeight(),
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row {
                        listOf("Objects", "Properties").forEach { label ->
                            FilterChip(
                                selected = panel == label,
                                onClick = { onPanelChange(label) },
                                label = { Text(label) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                    if (panel == "Objects") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Object types", style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                val id = (state.objects.maxOfOrNull { it.id } ?: 0) + 1
                                val obj = LayoutObject(id, "Sprite $id", "Sprite", 200f, 160f, 96f, 96f)
                                state.objects = state.objects + obj
                                state.selectedId = id
                                state.modified = true
                            }) { Icon(Icons.Default.Add, null) }
                            IconButton(onClick = onImport) { Icon(Icons.Default.Image, null) }
                        }
                        LazyColumn {
                            items(state.objectTypes) { type ->
                                TextButton(onClick = {
                                    val id = (state.objects.maxOfOrNull { it.id } ?: 0) + 1
                                    val obj = LayoutObject(id, "$type $id", type, 180f, 140f, 128f, 96f)
                                    state.objects = state.objects + obj
                                    state.selectedId = id
                                    state.modified = true
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.Category, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(type, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        Text("Properties", style = MaterialTheme.typography.titleSmall)
                        if (selected == null) {
                            Text("Select an object", color = Color.Gray)
                        } else {
                            OutlinedTextField(
                                value = selected.name,
                                onValueChange = {
                                    selected.name = it
                                    state.objects = state.objects.toList()
                                    state.modified = true
                                },
                                label = { Text("Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = selected.plugin,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Plugin") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row {
                                PropertyNumber("X", selected.x) { selected.x = it; state.modified = true }
                                PropertyNumber("Y", selected.y) { selected.y = it; state.modified = true }
                            }
                            Row {
                                PropertyNumber("W", selected.w) { selected.w = it; state.modified = true }
                                PropertyNumber("H", selected.h) { selected.h = it; state.modified = true }
                            }
                            OutlinedButton(onClick = {
                                state.objects = state.objects.filter { it.id != selected.id }
                                state.selectedId = state.objects.firstOrNull()?.id ?: 0
                                state.modified = true
                            }) { Icon(Icons.Default.Delete, null); Spacer(Modifier.width(6.dp)); Text("Delete") }
                        }
                    }
                }
            }
        }

@Composable
private fun PropertyNumber(label: String, value: Float, onChange: (Float) -> Unit) {
            var text by remember(value) { mutableStateOf(value.toInt().toString()) }
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    it.toFloatOrNull()?.let(onChange)
                },
                label = { Text(label) },
                modifier = Modifier.width(116.dp).padding(end = 4.dp, bottom = 4.dp)
            )
}

@Composable
private fun EventEditor(state: EditorState) {
            var condition by remember { mutableStateOf("System: Every tick") }
            var action by remember { mutableStateOf("System: Add 1 to counter") }
            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Text("Event sheet editor", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.events) { event ->
                        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                            Row(modifier = Modifier.padding(10.dp)) {
                                Text(event.condition, modifier = Modifier.weight(1f))
                                Icon(Icons.Filled.ArrowForward, null)
                                Text(event.action, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                OutlinedTextField(value = condition, onValueChange = { condition = it },
                    label = { Text("Condition") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(value = action, onValueChange = { action = it },
                    label = { Text("Action") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Button(onClick = {
                    state.events = state.events + EventRow(condition, action)
                    state.modified = true
                }, modifier = Modifier.align(Alignment.End)) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Add event")
                }
            }
}

@Composable
private fun AnimationEditor(state: EditorState, onImport: () -> Unit) {
            var frameName by remember { mutableStateOf("Idle") }
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Text("Animation Editor", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(value = frameName, onValueChange = { frameName = it },
                    label = { Text("Animation") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Row {
                    listOf("Frame 1", "Frame 2", "Frame 3").forEach { frame ->
                        Card(modifier = Modifier.size(120.dp).padding(4.dp)) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(frame)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onImport) {
                    Icon(Icons.Default.AddPhotoAlternate, null); Spacer(Modifier.width(6.dp))
                    Text("Import frame")
                }
                Spacer(Modifier.height(8.dp))
                Text("Frame editor supports imported image resources; animated sprite playback is runtime work in progress.",
                    color = Color(0xFF8A5A00))
            }
}

@Composable
private fun ProjectManager(state: EditorState) {
            var name by remember(state.projectName) { mutableStateOf(state.projectName) }
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Text("Project Manager", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; state.projectName = it; state.modified = true },
                    label = { Text("Project name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("Layouts", style = MaterialTheme.typography.titleSmall)
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
                    state.layouts = state.layouts + "Layout $n"
                    state.modified = true
                }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Add layout") }
                Spacer(Modifier.height(12.dp))
                Text("Canvas: ${state.width} x ${state.height}")
                Text("Objects: ${state.objects.size}  Events: ${state.events.size}",
                    style = MaterialTheme.typography.bodyMedium)
            }
}

@Composable
private fun LayoutEditor(state: EditorState) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFFE8EDF2)).padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(onClick = { scale = (scale + 0.1f).coerceAtMost(3f) }, label = { Text("+") })
            Spacer(Modifier.width(4.dp))
            AssistChip(onClick = { scale = (scale - 0.1f).coerceAtLeast(0.5f) }, label = { Text("-") })
            Spacer(Modifier.width(8.dp))
            Text("${(scale * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
        }
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF252A30))
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        offset += drag
                    }
                }
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
                    .pointerInput(state.objects, state.selectedId) {
                        detectTapGestures { point ->
                            val hit = state.objects.lastOrNull { o ->
                                val x = (o.x * scale) + offset.x
                                val y = (o.y * scale) + offset.y
                                val w = o.w * scale
                                val h = o.h * scale
                                point.x >= x && point.x <= x + w && point.y >= y && point.y <= y + h
                            }
                            if (hit != null) state.selectedId = hit.id
                        }
                    }
                    .pointerInput(state.objects, state.selectedId) {
                        detectDragGestures(
                            onDragStart = { point ->
                                state.objects.lastOrNull { o ->
                                    val x = o.x * scale + offset.x
                                    val y = o.y * scale + offset.y
                                    point.x >= x && point.x <= x + o.w * scale &&
                                        point.y >= y && point.y <= y + o.h * scale
                                }?.let { state.selectedId = it.id }
                            },
                            onDrag = { _, drag ->
                                val o = state.objects.firstOrNull { it.id == state.selectedId }
                                if (o != null) {
                                    o.x += drag.x / scale
                                    o.y += drag.y / scale
                                    state.objects = state.objects.toList()
                                    state.modified = true
                                }
                            }
                        )
                    }
            ) {
                drawRect(Color(0xFFF9FBFC), topLeft = offset, size = androidx.compose.ui.geometry.Size(state.width * scale, state.height * scale))
                for (o in state.objects) {
                    val topLeft = Offset(o.x * scale + offset.x, o.y * scale + offset.y)
                    val size = androidx.compose.ui.geometry.Size(o.w * scale, o.h * scale)
                    drawRect(
                        color = if (o.id == state.selectedId) Color(0xFF2F88C8) else Color(0xFF8CBBD9),
                        topLeft = topLeft, size = size
                    )
                    drawRect(
                        color = if (o.id == state.selectedId) Color(0xFFFFA000) else Color(0xFF315D75),
                        topLeft = topLeft, size = size, style = Stroke(2.dp.toPx())
                    )
                }
            }
        }
    }
}
