package com.aistudio.selfappbuilder

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DarkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

private const val DEFAULT_PROFILE = "https://www.tiktok.com/@astralwesennummerdrei?_r=1&_t=ZG-99YyjTnhbXs"
private const val PREFS = "tiktok_agent"
private const val PREF_VIDEOS = "videos"

data class VideoItem(
    val id: String,
    val source: String,
    val url: String,
    val title: String,
    val author: String,
    val durationSec: Double,
    val width: Int,
    val height: Int,
    val openingEnergy: Int,
    val hashtags: List<String>
)

data class OEmbedMeta(
    val title: String,
    val author: String,
    val authorUrl: String,
    val type: String,
    val thumbnail: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            TikTokAgentTheme {
                TikTokAgentScreen()
            }
        }
    }
}

@Composable
private fun TikTokAgentTheme(content: @Composable () -> Unit) {
    val scheme: DarkColorScheme = darkColorScheme()
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
private fun TikTokAgentScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val videos = remember { mutableStateListOf<VideoItem>() }

    var profileUrl by rememberSaveable { mutableStateOf(DEFAULT_PROFILE) }
    var videoUrl by rememberSaveable { mutableStateOf("") }
    var profileInfo by remember { mutableStateOf("Profil noch nicht geprüft.") }
    var status by remember { mutableStateOf("Bereit.") }

    LaunchedEffect(Unit) {
        videos.clear()
        videos.addAll(loadVideos(context))
    }

    val importVideo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                status = "Lokales Video wird analysiert …"
                try {
                    try {
                        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (_: Exception) {
                    }
                    val item = withContext(Dispatchers.IO) { analyzeLocalVideo(context, uri) }
                    videos.add(0, item)
                    saveVideos(context, videos)
                    status = "Lokales Video analysiert: ${item.durationSec.format1()} s, Opening-Energie ${item.openingEnergy}/100."
                } catch (e: Exception) {
                    status = "Videoanalyse fehlgeschlagen: ${e.message ?: "unbekannter Fehler"}"
                }
            }
        }
    }

    val jsonExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            runCatching { writeText(context, uri, videosToJson(videos).toString(2)) }
                .onSuccess { status = "JSON exportiert." }
                .onFailure { status = "JSON-Export fehlgeschlagen: ${it.message}" }
        }
    }

    val csvExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) {
            runCatching { writeText(context, uri, videosToCsv(videos)) }
                .onSuccess { status = "CSV exportiert." }
                .onFailure { status = "CSV-Export fehlgeschlagen: ${it.message}" }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("TikTok Video Analyse Agent", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Öffentliche Links + lokale Videoanalyse · Android 15", style = MaterialTheme.typography.bodyMedium)
            }

            item {
                SectionCard("Startprojekt · @astralwesennummerdrei") {
                    OutlinedTextField(
                        value = profileUrl,
                        onValueChange = { profileUrl = it },
                        label = { Text("TikTok-Profil-Link") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                status = "Profil-Metadaten werden geladen …"
                                try {
                                    val meta = withContext(Dispatchers.IO) { fetchOEmbed(profileUrl.trim()) }
                                    profileInfo = "${meta.author}\n${meta.title}\nTyp: ${meta.type}\n${meta.authorUrl}"
                                    status = "Profil geprüft. TikToks oEmbed liefert Profil-Metadaten, aber keine vollständige Video-Liste."
                                } catch (e: Exception) {
                                    profileInfo = "Profil konnte nicht automatisch gelesen werden."
                                    status = "Profilprüfung fehlgeschlagen: ${e.message ?: "TikTok blockiert den Abruf"}"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Profil prüfen") }
                    Spacer(Modifier.height(8.dp))
                    Text(profileInfo, style = MaterialTheme.typography.bodySmall)
                }
            }

            item {
                SectionCard("Video hinzufügen") {
                    OutlinedTextField(
                        value = videoUrl,
                        onValueChange = { videoUrl = it },
                        label = { Text("TikTok-Video-Link") },
                        placeholder = { Text("https://www.tiktok.com/@name/video/…") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val input = videoUrl.trim()
                                if (!input.contains("tiktok.com", ignoreCase = true)) {
                                    status = "Bitte einen TikTok-Link einfügen."
                                } else {
                                    scope.launch {
                                        status = "TikTok-Metadaten werden geladen …"
                                        try {
                                            val meta = withContext(Dispatchers.IO) { fetchOEmbed(input) }
                                            val item = VideoItem(
                                                id = UUID.randomUUID().toString(),
                                                source = "TikTok oEmbed",
                                                url = input,
                                                title = meta.title.ifBlank { "TikTok-Video" },
                                                author = meta.author,
                                                durationSec = 0.0,
                                                width = 0,
                                                height = 0,
                                                openingEnergy = hookSignal(meta.title),
                                                hashtags = extractHashtags(meta.title)
                                            )
                                            videos.add(0, item)
                                            saveVideos(context, videos)
                                            videoUrl = ""
                                            status = "Video hinzugefügt. Hook-Signal ${item.openingEnergy}/100 (Text-Heuristik)."
                                        } catch (e: Exception) {
                                            status = "TikTok-Link konnte nicht gelesen werden: ${e.message ?: "Abruf blockiert"}"
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Link analysieren") }
                        OutlinedButton(
                            onClick = { importVideo.launch(arrayOf("video/*")) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Video importieren") }
                    }
                }
            }

            item {
                SectionCard("Agent-Bericht") {
                    Text(buildReport(videos), style = MaterialTheme.typography.bodyMedium)
                }
            }

            item {
                SectionCard("Export") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { jsonExport.launch("tiktok-analyse.json") }, modifier = Modifier.weight(1f)) {
                            Text("JSON")
                        }
                        OutlinedButton(onClick = { csvExport.launch("tiktok-analyse.csv") }, modifier = Modifier.weight(1f)) {
                            Text("CSV")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            videos.clear()
                            saveVideos(context, videos)
                            status = "Projektliste geleert."
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Analyseliste leeren") }
                }
            }

            item {
                Text("Analysierte Videos (${videos.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            if (videos.isEmpty()) {
                item {
                    Text("Noch keine Videos. Ein TikTok-Link oder eine lokale Datei reicht zum Start.")
                }
            }

            items(videos, key = { it.id }) { item ->
                VideoCard(item = item, onDelete = {
                    videos.remove(item)
                    saveVideos(context, videos)
                    status = "Eintrag entfernt."
                })
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(status, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun VideoCard(item: VideoItem, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (item.author.isNotBlank()) Text("Creator: ${item.author}", style = MaterialTheme.typography.bodySmall)
            Text("Quelle: ${item.source}", style = MaterialTheme.typography.bodySmall)
            if (item.durationSec > 0) {
                Text("${item.durationSec.format1()} s · ${item.width}×${item.height} · Opening-Energie ${item.openingEnergy}/100", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Hook-Signal ${item.openingEnergy}/100 · Text-Heuristik", style = MaterialTheme.typography.bodySmall)
            }
            if (item.hashtags.isNotEmpty()) Text(item.hashtags.joinToString("  ") { "#$it" }, style = MaterialTheme.typography.bodySmall)
            if (item.url.isNotBlank()) Text(item.url, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("Entfernen") }
        }
    }
}

private fun fetchOEmbed(rawUrl: String): OEmbedMeta {
    if (rawUrl.isBlank()) throw IllegalArgumentException("Link fehlt")
    val encoded = URLEncoder.encode(rawUrl, Charsets.UTF_8.name())
    val connection = URL("https://www.tiktok.com/oembed?url=$encoded").openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 12_000
    connection.readTimeout = 12_000
    connection.instanceFollowRedirects = true
    connection.setRequestProperty("User-Agent", "Mozilla/5.0 Android TikTokVideoAnalyseAgent/1.0")
    try {
        val code = connection.responseCode
        val body = if (code in 200..299) connection.inputStream.bufferedReader().use { it.readText() }
        else connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) throw IOException("TikTok HTTP $code${if (body.isNotBlank()) ": ${body.take(120)}" else ""}")
        val json = JSONObject(body)
        return OEmbedMeta(
            title = json.optString("title"),
            author = json.optString("author_name"),
            authorUrl = json.optString("author_url"),
            type = json.optString("type"),
            thumbnail = json.optString("thumbnail_url")
        )
    } finally {
        connection.disconnect()
    }
}

private fun analyzeLocalVideo(context: Context, uri: Uri): VideoItem {
    val retriever = MediaMetadataRetriever()
    try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            retriever.setDataSource(pfd.fileDescriptor)
        } ?: throw IOException("Datei kann nicht geöffnet werden")

        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        val energy = calculateOpeningEnergy(retriever, durationMs)
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "Lokales Video"

        return VideoItem(
            id = UUID.randomUUID().toString(),
            source = "Lokale Datei",
            url = uri.toString(),
            title = name,
            author = "",
            durationSec = durationMs / 1000.0,
            width = width,
            height = height,
            openingEnergy = energy,
            hashtags = emptyList()
        )
    } finally {
        retriever.release()
    }
}

private fun calculateOpeningEnergy(retriever: MediaMetadataRetriever, durationMs: Long): Int {
    if (durationMs <= 0) return 0
    val endUs = minOf(durationMs * 1000L, 5_000_000L)
    val lumas = mutableListOf<Double>()
    for (i in 0..4) {
        val timeUs = endUs * i / 4L
        val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
        if (bitmap != null) lumas += averageLuma(bitmap)
    }
    if (lumas.size < 2) return 0
    val averageDelta = lumas.zipWithNext { a, b -> abs(a - b) }.average()
    return ((averageDelta / 64.0) * 100.0).roundToInt().coerceIn(0, 100)
}

private fun averageLuma(bitmap: Bitmap): Double {
    val scaled = Bitmap.createScaledBitmap(bitmap, 24, 24, true)
    val pixels = IntArray(24 * 24)
    scaled.getPixels(pixels, 0, 24, 0, 0, 24, 24)
    var total = 0.0
    for (pixel in pixels) {
        total += 0.2126 * Color.red(pixel) + 0.7152 * Color.green(pixel) + 0.0722 * Color.blue(pixel)
    }
    if (scaled !== bitmap) scaled.recycle()
    bitmap.recycle()
    return total / pixels.size
}

private fun hookSignal(title: String): Int {
    if (title.isBlank()) return 20
    val t = title.trim().lowercase(Locale.GERMAN)
    var score = 35
    if ('?' in t) score += 20
    if (Regex("\\d").containsMatchIn(t.take(24))) score += 12
    if (listOf("warum", "wie", "wenn", "du ", "dein", "stop", "achtung", "so ").any { t.startsWith(it) || " $it" in t }) score += 18
    if (title.length in 25..120) score += 10
    if (extractHashtags(title).isNotEmpty()) score += 5
    return score.coerceIn(0, 100)
}

private fun extractHashtags(text: String): List<String> =
    Regex("""#([\p{L}\p{N}_]+)""").findAll(text).map { it.groupValues[1].lowercase(Locale.GERMAN) }.distinct().toList()

private fun buildReport(videos: List<VideoItem>): String {
    if (videos.isEmpty()) return "Noch keine Datengrundlage. Füge Video-Links hinzu oder importiere Dateien."

    val linked = videos.count { it.source == "TikTok oEmbed" }
    val local = videos.count { it.source == "Lokale Datei" }
    val durations = videos.map { it.durationSec }.filter { it > 0 }
    val averageDuration = durations.takeIf { it.isNotEmpty() }?.average()
    val averageSignal = videos.map { it.openingEnergy }.average().roundToInt()
    val tags = videos.flatMap { it.hashtags }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(5)
    val creators = videos.map { it.author }.filter { it.isNotBlank() }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(3)
    val topHooks = videos.sortedByDescending { it.openingEnergy }.take(3).joinToString("\n") { "• ${it.title.take(72)} (${it.openingEnergy}/100)" }

    return buildString {
        append("Datensatz: ${videos.size} Videos ($linked TikTok-Links, $local lokal).\n")
        if (averageDuration != null) append("Ø lokale Videolänge: ${averageDuration.format1()} s.\n")
        append("Ø Opening-/Hook-Signal: $averageSignal/100. Bei lokalen Dateien basiert es auf Bildwechseln der ersten 5 Sekunden, bei TikTok-Links auf einer Text-Heuristik der Caption.\n")
        if (tags.isNotEmpty()) append("Häufige Hashtags: ${tags.joinToString { "#${it.key} (${it.value})" }}.\n")
        if (creators.isNotEmpty()) append("Creator im Datensatz: ${creators.joinToString { "${it.key} (${it.value})" }}.\n")
        append("\nAuffällige Hooks nach Signal:\n$topHooks\n")
        append("\nGrenze der Daten: TikTok-oEmbed liefert Titel/Creator/Embed-Daten, aber keine vollständige Account-Videoliste und keine verlässlichen View-/Like-/Kommentarzahlen. Für die offizielle Account-Videoliste ist TikTok-OAuth des Kontoinhabers nötig.")
    }
}

private fun saveVideos(context: Context, videos: List<VideoItem>) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(PREF_VIDEOS, videosToJson(videos).toString())
        .apply()
}

private fun loadVideos(context: Context): List<VideoItem> {
    val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(PREF_VIDEOS, null) ?: return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val tagArray = o.optJSONArray("hashtags") ?: JSONArray()
                val tags = buildList { for (j in 0 until tagArray.length()) add(tagArray.optString(j)) }
                add(
                    VideoItem(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        source = o.optString("source"),
                        url = o.optString("url"),
                        title = o.optString("title"),
                        author = o.optString("author"),
                        durationSec = o.optDouble("durationSec", 0.0),
                        width = o.optInt("width", 0),
                        height = o.optInt("height", 0),
                        openingEnergy = o.optInt("openingEnergy", 0),
                        hashtags = tags
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}

private fun videosToJson(videos: List<VideoItem>): JSONArray {
    val array = JSONArray()
    videos.forEach { v ->
        array.put(JSONObject().apply {
            put("id", v.id)
            put("source", v.source)
            put("url", v.url)
            put("title", v.title)
            put("author", v.author)
            put("durationSec", v.durationSec)
            put("width", v.width)
            put("height", v.height)
            put("openingEnergy", v.openingEnergy)
            put("hashtags", JSONArray(v.hashtags))
        })
    }
    return array
}

private fun videosToCsv(videos: List<VideoItem>): String = buildString {
    appendLine("source,title,author,url,duration_seconds,width,height,opening_energy,hashtags")
    videos.forEach { v ->
        appendLine(
            listOf(
                v.source, v.title, v.author, v.url, v.durationSec.format1(), v.width.toString(), v.height.toString(),
                v.openingEnergy.toString(), v.hashtags.joinToString(" ") { "#$it" }
            ).joinToString(",") { csv(it) }
        )
    }
}

private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""

private fun writeText(context: Context, uri: Uri, text: String) {
    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text) }
        ?: throw IOException("Zieldatei kann nicht geöffnet werden")
}

private fun Double.format1(): String = String.format(Locale.GERMAN, "%.1f", this)
