package com.nolan.gaminghub

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import java.text.DateFormat
import java.util.Date

private fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()

fun launchGame(ctx: Context, lib: Library, g: Game): String? {
    return try {
        if (g.type == GameType.ANDROID) {
            val i = ctx.packageManager.getLaunchIntentForPackage(g.source)
                ?: return "App haijawekwa tena: ${g.title}"
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i)
        } else {
            val view = Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(g.source), "*/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val chooser = Intent.createChooser(view, "Fungua ${g.title} na emulator")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(chooser)
        }
        lib.markPlayed(g.id)
        null
    } catch (e: Exception) {
        "Haikufunguka: ${e.message}"
    }
}

@Composable
fun Cover(g: Game, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val bmp = remember(g.source) {
        if (g.type == GameType.ANDROID)
            runCatching { ctx.packageManager.getApplicationIcon(g.source).toBitmap(128, 128).asImageBitmap() }.getOrNull()
        else null
    }
    val bg = when (g.type) {
        GameType.ANDROID -> Color(0xFF1F7A5A)
        GameType.PSP -> Color(0xFF2D4FBF)
        GameType.PS2 -> Color(0xFF6B3FD4)
    }
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) {
        if (bmp != null) Image(bmp, g.title, Modifier.fillMaxSize(0.7f))
        else Text(g.title.take(1).uppercase(), fontSize = 32.sp, color = Color.White)
    }
}

private enum class Filter(val label: String) { ALL("All"), ANDROID("Android"), PSP("PSP"), PS2("PS2"), FAV("Favorites") }

private fun displayName(ctx: Context, uri: Uri): String =
    ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { if (it.moveToFirst()) it.getString(0) else null } ?: "Game"

private fun launcherApps(ctx: Context): List<Triple<String, String, Boolean>> {
    val pm = ctx.packageManager
    val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(q, 0)
        .map { Triple(it.activityInfo.packageName, it.loadLabel(pm).toString(),
            it.activityInfo.applicationInfo.category == ApplicationInfo.CATEGORY_GAME) }
        .filter { it.first != ctx.packageName }
        .distinctBy { it.first }.sortedBy { it.second.lowercase() }
}

@Composable
fun GamesScreen(lib: Library) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(Filter.ALL) }
    var showApps by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<Pair<Uri, String>?>(null) }
    var infoId by remember { mutableStateOf<String?>(null) }

    fun addFile(uri: Uri, name: String, type: GameType) {
        val ok = lib.add(Game(title = name.substringBeforeLast('.'), type = type, source = uri.toString()))
        toast(ctx, if (ok) "Imeongezwa: $name" else "Tayari ipo kwenye maktaba")
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
            val name = displayName(ctx, uri)
            val ext = name.substringAfterLast('.', "").lowercase()
            if (ext == "cso" || ext == "pbp") addFile(uri, name, GameType.PSP) else pending = uri to name
        }
    }

    val shown = lib.games.filter {
        (when (filter) {
            Filter.ALL -> true
            Filter.ANDROID -> it.type == GameType.ANDROID
            Filter.PSP -> it.type == GameType.PSP
            Filter.PS2 -> it.type == GameType.PS2
            Filter.FAV -> it.favorite
        }) && it.title.contains(query, ignoreCase = true)
    }.sortedBy { it.title.lowercase() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("My Games", fontSize = 26.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                var n = 0
                launcherApps(ctx).filter { it.third }.forEach {
                    if (lib.add(Game(title = it.second, type = GameType.ANDROID, source = it.first))) n++
                }
                toast(ctx, if (n > 0) "Games $n zimeongezwa" else "Hakuna game mpya iliyopatikana")
            }) { Text("Scan") }
            OutlinedButton(onClick = { showApps = true }) { Text("+ App") }
            OutlinedButton(onClick = { picker.launch(arrayOf("*/*")) }) { Text("+ File") }
        }
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text("Search") })
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Filter.values().toList()) { f ->
                FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) })
            }
        }
        if (shown.isEmpty()) {
            Text(
                if (lib.games.isEmpty()) "Maktaba ni tupu. Tumia Scan (games za simu), + App, au + File (PSP/PS2 ISO/CSO/PBP)."
                else "Hakuna game inayolingana.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyVerticalGrid(GridCells.Adaptive(150.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(shown, key = { it.id }) { g ->
                    Card(colors = CardDefaults.cardColors(containerColor = NavyCard),
                        modifier = Modifier.clickable { launchGame(ctx, lib, g)?.let { toast(ctx, it) } }) {
                        Column(Modifier.padding(10.dp)) {
                            Cover(g, Modifier.fillMaxWidth().height(100.dp))
                            Spacer(Modifier.height(6.dp))
                            Text(g.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(g.type.label, fontSize = 11.sp, color = ElectricBlue, modifier = Modifier.weight(1f))
                                IconButton({ lib.toggleFavorite(g.id) }, Modifier.size(34.dp)) {
                                    Icon(if (g.favorite) Icons.Filled.Star else Icons.Filled.StarBorder, "Favorite",
                                        tint = if (g.favorite) ElectricBlue else Color.Gray)
                                }
                                IconButton({ infoId = g.id }, Modifier.size(34.dp)) {
                                    Icon(Icons.Filled.Info, "Info", tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showApps) {
        var q by remember { mutableStateOf("") }
        val apps = remember { launcherApps(ctx) }
        AlertDialog(onDismissRequest = { showApps = false },
            confirmButton = { TextButton({ showApps = false }) { Text("Close") } },
            title = { Text("Chagua app") },
            text = {
                Column {
                    OutlinedTextField(q, { q = it }, singleLine = true, placeholder = { Text("Search") })
                    LazyColumn(Modifier.height(320.dp)) {
                        items(apps.filter { it.second.contains(q, true) }) { a ->
                            Text(a.second, Modifier.fillMaxWidth().clickable {
                                val ok = lib.add(Game(title = a.second, type = GameType.ANDROID, source = a.first))
                                toast(ctx, if (ok) "Imeongezwa: ${a.second}" else "Tayari ipo kwenye maktaba")
                                showApps = false
                            }.padding(vertical = 12.dp))
                        }
                    }
                }
            })
    }

    pending?.let { (uri, name) ->
        AlertDialog(onDismissRequest = { pending = null },
            title = { Text("Hii ni game ya aina gani?") },
            text = { Text(name) },
            confirmButton = { TextButton({ addFile(uri, name, GameType.PSP); pending = null }) { Text("PSP") } },
            dismissButton = { TextButton({ addFile(uri, name, GameType.PS2); pending = null }) { Text("PS2") } })
    }

    lib.games.firstOrNull { it.id == infoId }?.let { g ->
        AlertDialog(onDismissRequest = { infoId = null },
            title = { Text(g.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Aina: ${g.type.label}")
                    Text("Imefunguliwa: ${g.launches} mara")
                    Text("Mara ya mwisho: " + if (g.lastPlayed > 0) DateFormat.getDateTimeInstance().format(Date(g.lastPlayed)) else "bado")
                    Text(if (g.type == GameType.ANDROID) "Package: ${g.source}" else "Faili: ${Uri.parse(g.source).lastPathSegment}",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (g.type != GameType.ANDROID)
                        Text("Kufungua kunatumia chaguo la mfumo (Open with). Muunganisho kamili wa emulator unakuja Stage 4-5.",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton({ infoId = null }) { Text("Close") } },
            dismissButton = { TextButton({ lib.remove(g.id); infoId = null }) { Text("Ondoa") } })
    }
}
