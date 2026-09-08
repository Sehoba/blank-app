package com.aistudio.selfappbuilder

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF070D18),
                    surface = Color(0xFF0F172A),
                    primary = Color(0xFF10B981),
                    secondary = Color(0xFFF59E0B),
                    surfaceVariant = Color(0xFF1E293B)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SelfAppBuilderScreen(this)
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfAppBuilderScreen(context: Context) {
    val defaultProject = remember { GameTemplates.getGorillaFiaGame() }

    var currentTab by remember { mutableStateOf("HTML") }
    var htmlCode by remember { mutableStateOf(defaultProject.html) }
    var cssCode by remember { mutableStateOf(defaultProject.css) }
    var jsCode by remember { mutableStateOf(defaultProject.js) }

    var previewHtml by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("Bereit. Gorilla & Fia Dschungelspiel geladen.") }

    var showGameCreatorDialog by remember { mutableStateOf(false) }
    var showFullscreenGame by remember { mutableStateOf(false) }
    var showProjectsDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var projectSaveName by remember { mutableStateOf("Gorilla & Fia") }

    val sharedPreferences = remember {
        context.getSharedPreferences("selfbuilder_projects", Context.MODE_PRIVATE)
    }

    fun buildFullDoc(): String {
        return "<!doctype html><html><head>" +
                "<meta charset=\"utf-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no\">" +
                "<style>${cssCode}</style>" +
                "</head><body>${htmlCode}" +
                "<script>${jsCode}</script>" +
                "</body></html>"
    }

    fun runCode() {
        previewHtml = buildFullDoc()
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        statusMessage = "Ausgeführt um $time Uhr."
    }

    LaunchedEffect(Unit) {
        runCode()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        // App Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Self App Builder",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "IDE v3",
                            color = Color(0xFF34D399),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "HTML · CSS · JavaScript & Game Studio",
                    color = Color(0xFF94A3B8),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp
                )
            }

            // High-priority Game Creator Button
            Button(
                onClick = { showGameCreatorDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE11D48),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_create_game")
            ) {
                Text(
                    text = "🎮 Spiel erstellen",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // Action Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .background(Color(0xFF0A1120))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { runCode() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_run")
            ) {
                Text("▶ " + stringResource(R.string.action_run))
            }

            Button(
                onClick = {
                    runCode()
                    showFullscreenGame = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_fullscreen")
            ) {
                Text("📱 " + stringResource(R.string.action_fullscreen))
            }

            Button(
                onClick = { showSaveDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_save")
            ) {
                Text("💾 " + stringResource(R.string.action_save))
            }

            Button(
                onClick = { showProjectsDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_projects")
            ) {
                Text("📁 " + stringResource(R.string.action_load))
            }

            Button(
                onClick = {
                    val p = GameTemplates.getGorillaFiaGame()
                    htmlCode = p.html
                    cssCode = p.css
                    jsCode = p.js
                    currentTab = "HTML"
                    runCode()
                    statusMessage = "Auf Vorlage zurückgesetzt."
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_reset")
            ) {
                Text("↺ " + stringResource(R.string.action_reset))
            }
        }

        // Code Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("HTML", "CSS", "JS").forEach { tab ->
                val isSelected = currentTab == tab
                Surface(
                    color = if (isSelected) Color(0xFFFBBF24) else Color(0xFF1E293B),
                    contentColor = if (isSelected) Color.Black else Color(0xFFCBD5E1),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clickable { currentTab = tab }
                        .testTag("tab_${tab.lowercase()}")
                ) {
                    Text(
                        text = when (tab) {
                            "HTML" -> "HTML 📄"
                            "CSS" -> "CSS 🎨"
                            else -> "JavaScript ⚡"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Code Editor Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF020617))
        ) {
            val currentCode = when (currentTab) {
                "HTML" -> htmlCode
                "CSS" -> cssCode
                else -> jsCode
            }
            BasicTextField(
                value = currentCode,
                onValueChange = {
                    when (currentTab) {
                        "HTML" -> htmlCode = it
                        "CSS" -> cssCode = it
                        "JS" -> jsCode = it
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .testTag("code_editor"),
                textStyle = TextStyle(
                    color = Color(0xFFE2E8F0),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            )
        }

        // Live Preview Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Live-Vorschau & Spiel",
                color = Color(0xFFF8FAFC),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Tippen zum Steuern",
                color = Color(0xFF64748B),
                fontSize = 11.sp
            )
        }

        // Embedded Preview WebView
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 10.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF000000))
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.databaseEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        webViewClient = WebViewClient()
                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                consoleMessage?.let {
                                    if (it.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                                        statusMessage = "⚠️ JS Fehler: ${it.message()}"
                                    }
                                }
                                return super.onConsoleMessage(consoleMessage)
                            }
                        }
                    }
                },
                update = { webView ->
                    webView.loadDataWithBaseURL("https://localapp/", previewHtml, "text/html", "UTF-8", null)
                }
            )
        }

        // Bottom Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B1220))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = statusMessage,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }

    // --- GAME CREATOR MODAL DIALOG ---
    if (showGameCreatorDialog) {
        Dialog(
            onDismissRequest = { showGameCreatorDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(20.dp)),
                color = Color(0xFF0F172A)
            ) {
                var selectedTab by remember { mutableStateOf(0) } // 0 = Vorlagen, 1 = Baukasten

                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎮 Spiel erstellen",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "✕",
                            color = Color.Gray,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .clickable { showGameCreatorDialog = false }
                                .padding(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color(0xFFFBBF24)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Beliebte Spiele", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Spiel-Baukasten", fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedTab == 0) {
                        // Preset Games List
                        val games = listOf(
                            GameTemplates.getGorillaFiaGame(),
                            GameTemplates.getFlappyFiaGame(),
                            GameTemplates.getSnakeGame(),
                            GameTemplates.getSpaceShooterGame()
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.heightIn(max = 380.dp)
                        ) {
                            items(games) { game ->
                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            htmlCode = game.html
                                            cssCode = game.css
                                            jsCode = game.js
                                            currentTab = "HTML"
                                            runCode()
                                            statusMessage = "Spiel '${game.title}' geladen!"
                                            showGameCreatorDialog = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(game.icon, fontSize = 32.sp)
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = game.title,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = game.description,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 12.sp,
                                                lineHeight = 15.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                htmlCode = game.html
                                                cssCode = game.css
                                                jsCode = game.js
                                                currentTab = "HTML"
                                                runCode()
                                                statusMessage = "Spiel '${game.title}' geladen!"
                                                showGameCreatorDialog = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("Laden", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Custom Game Wizard / Baukasten
                        var speedSlider by remember { mutableFloatStateOf(1.0f) }
                        var jumpSlider by remember { mutableFloatStateOf(1.0f) }
                        var soundToggle by remember { mutableStateOf(true) }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = "Konfiguriere dein Gorilla & Fia Spiel:",
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.sp
                            )

                            // Speed Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Lauf-Tempo:", color = Color.White, fontSize = 13.sp)
                                    Text("${(speedSlider * 100).toInt()}%", color = Color(0xFF34D399), fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = speedSlider,
                                    onValueChange = { speedSlider = it },
                                    valueRange = 0.6f..1.8f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF10B981),
                                        activeTrackColor = Color(0xFF10B981)
                                    )
                                )
                            }

                            // Jump Power Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Sprungkraft:", color = Color.White, fontSize = 13.sp)
                                    Text("${(jumpPowerMultiplier(jumpSlider)).toInt()}%", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = jumpSlider,
                                    onValueChange = { jumpSlider = it },
                                    valueRange = 0.7f..1.6f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFBBF24),
                                        activeTrackColor = Color(0xFFFBBF24)
                                    )
                                )
                            }

                            // Sound toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Web Audio Sound-Effekte", color = Color.White, fontSize = 13.sp)
                                Switch(
                                    checked = soundToggle,
                                    onCheckedChange = { soundToggle = it }
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Button(
                                onClick = {
                                    val customGame = GameTemplates.getGorillaFiaGame(
                                        speedMultiplier = speedSlider,
                                        jumpPowerMultiplier = jumpSlider,
                                        soundEnabled = soundToggle
                                    )
                                    htmlCode = customGame.html
                                    cssCode = customGame.css
                                    jsCode = customGame.js
                                    currentTab = "HTML"
                                    runCode()
                                    statusMessage = "Individuelles Gorilla & Fia Spiel generiert!"
                                    showGameCreatorDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    "🚀 Spiel jetzt generieren & einfügen",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- FULLSCREEN GAME DIALOG ---
    if (showFullscreenGame) {
        Dialog(
            onDismissRequest = { showFullscreenGame = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.databaseEnabled = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            webViewClient = WebViewClient()
                            webChromeClient = WebChromeClient()
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL("https://localapp/", buildFullDoc(), "text/html", "UTF-8", null)
                    }
                )

                // Top Floating Exit Control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "🎮 VOLLBILD SPIELMODUS",
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Button(
                        onClick = { showFullscreenGame = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("✕ Beenden", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // --- SAVE PROJECT DIALOG ---
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Projekt / Spiel speichern") },
            text = {
                Column {
                    Text("Gib deinem Spiel einen Namen:", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = projectSaveName,
                        onValueChange = { projectSaveName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val key = "project_${projectSaveName.trim().ifEmpty { "Unbenannt" }}"
                        val jsonObj = JSONObject().apply {
                            put("name", projectSaveName)
                            put("html", htmlCode)
                            put("css", cssCode)
                            put("js", jsCode)
                            put("savedAt", System.currentTimeMillis())
                        }
                        sharedPreferences.edit().putString(key, jsonObj.toString()).apply()

                        // Add to list
                        val listStr = sharedPreferences.getString("all_project_keys", "[]") ?: "[]"
                        val jsonArr = JSONArray(listStr)
                        var exists = false
                        for (i in 0 until jsonArr.length()) {
                            if (jsonArr.getString(i) == key) exists = true
                        }
                        if (!exists) {
                            jsonArr.put(key)
                            sharedPreferences.edit().putString("all_project_keys", jsonArr.toString()).apply()
                        }

                        statusMessage = "Spiel '$projectSaveName' gespeichert!"
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // --- PROJECTS LIST DIALOG ---
    if (showProjectsDialog) {
        val listStr = sharedPreferences.getString("all_project_keys", "[]") ?: "[]"
        val keysArr = JSONArray(listStr)
        val savedItems = remember {
            val list = mutableListOf<Triple<String, String, Long>>()
            for (i in 0 until keysArr.length()) {
                val k = keysArr.getString(i)
                val data = sharedPreferences.getString(k, null)
                if (data != null) {
                    val obj = JSONObject(data)
                    val name = obj.optString("name", "Projekt")
                    val time = obj.optLong("savedAt", 0L)
                    list.add(Triple(k, name, time))
                }
            }
            list
        }

        AlertDialog(
            onDismissRequest = { showProjectsDialog = false },
            title = { Text("📁 Gespeicherte Spiele & Projekte") },
            text = {
                if (savedItems.isEmpty()) {
                    Text("Noch keine gespeicherten Spiele vorhanden.\nNutze '💾 Speichern' um dein Projekt zu sichern.", color = Color.Gray)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 280.dp)
                    ) {
                        items(savedItems) { item ->
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val data = sharedPreferences.getString(item.first, null)
                                        if (data != null) {
                                            val obj = JSONObject(data)
                                            htmlCode = obj.optString("html", "")
                                            cssCode = obj.optString("css", "")
                                            jsCode = obj.optString("js", "")
                                            currentTab = "HTML"
                                            runCode()
                                            statusMessage = "Projekt '${item.second}' geladen."
                                            showProjectsDialog = false
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(item.second, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Geladen durch Antippen", color = Color.Gray, fontSize = 11.sp)
                                    }
                                    Text("Laden ▶", color = Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProjectsDialog = false }) {
                    Text("Schließen")
                }
            }
        )
    }
}

private fun jumpPowerMultiplier(v: Float): Float {
    return v * 100f
}
