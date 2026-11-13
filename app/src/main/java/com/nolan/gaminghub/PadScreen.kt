package com.nolan.gaminghub

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

private fun Context.findActivity(): Activity? {
    var c = this
    while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }
    return null
}

private val PadBase = Color(0xFF16254F)

@Composable
fun PadScreen(store: PadStore) {
    val ctx = LocalContext.current
    val density = LocalDensity.current.density
    var editing by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<String?>(null) }
    var landscape by remember { mutableStateOf(false) }
    var naming by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var areaW by remember { mutableStateOf(1) }
    val dim = remember { floatArrayOf(1f, 1f) }
    val pressed = remember { mutableStateMapOf<String, Boolean>() }
    val axes = remember { mutableStateMapOf<String, Offset>() }
    val prof = store.current
    val sel = prof.controls.firstOrNull { it.id == selected }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Virtual Pad", fontSize = 24.sp, modifier = Modifier.weight(1f))
            TextButton({
                landscape = !landscape
                ctx.findActivity()?.requestedOrientation =
                    if (landscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }) { Text(if (landscape) "Portrait" else "Landscape") }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            store.profiles.forEachIndexed { i, p ->
                FilterChip(selected = i == store.active, onClick = { store.select(i); selected = null }, label = { Text(p.name) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !editing, onClick = { editing = false }, label = { Text("Test") })
            FilterChip(selected = editing, onClick = { editing = true }, label = { Text("Edit") })
        }

        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp)).background(Color(0xFF020611))
                .border(1.dp, if (editing) ElectricBlue else Color(0xFF1B2A55), RoundedCornerShape(16.dp))
                .onSizeChanged { areaW = it.width; dim[0] = it.width.toFloat(); dim[1] = it.height.toFloat() }
        ) {
            val scale = areaW / (640f * density)
            prof.controls.forEach { c ->
                key(c.id) {
                    PadControl(c, scale, density, dim, prof.opacity, editing, c.id == selected,
                        pressed, axes, store) { selected = c.id }
                }
            }
        }

        if (editing) {
            if (sel == null) {
                Text("Gusa kitufe kukichagua, kisha kiburute kukihamisha.", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("Size: ${sel.id}", fontSize = 13.sp)
                Slider(sel.size, { store.resize(sel.id, it) }, valueRange = 30f..170f,
                    onValueChangeFinished = { store.persist() })
            }
            Text("Transparency", fontSize = 13.sp)
            Slider(prof.opacity, { store.setOpacity(it) }, valueRange = 0.2f..1f,
                onValueChangeFinished = { store.persist() })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ store.resetActive(); selected = null }) { Text("Reset") }
                OutlinedButton({ newName = ""; naming = true }) { Text("Save as new") }
                if (store.profiles.size > 1)
                    OutlinedButton({ store.deleteActive(); selected = null }) { Text("Delete") }
            }
        } else {
            val on = pressed.filter { it.value }.keys.sorted().joinToString(" + ")
            val l = axes["LS"] ?: Offset.Zero
            val r = axes["RS"] ?: Offset.Zero
            Text("Pressed: " + on.ifEmpty { "-" })
            Text("LS: %.2f, %.2f    RS: %.2f, %.2f".format(l.x, l.y, r.x, r.y), fontSize = 13.sp)
            Text("Hii ni jaribio la pad ndani ya app. Kutuma input kwa emulator kunakuja Stage 4.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (naming) {
        AlertDialog(onDismissRequest = { naming = false },
            title = { Text("Jina la profile") },
            text = { OutlinedTextField(newName, { newName = it }, singleLine = true) },
            confirmButton = { TextButton({ store.addProfile(newName); naming = false }) { Text("Save") } },
            dismissButton = { TextButton({ naming = false }) { Text("Cancel") } })
    }
}

@Composable
private fun PadControl(
    c: Control, scale: Float, density: Float, dim: FloatArray, opacity: Float,
    editing: Boolean, selected: Boolean,
    pressed: SnapshotStateMap<String, Boolean>, axes: SnapshotStateMap<String, Offset>,
    store: PadStore, onSelect: () -> Unit
) {
    val wf = when (c.kind) { Kind.SHOULDER -> 1.5f; Kind.MENU -> 1.6f; else -> 1f }
    val hf = when (c.kind) { Kind.SHOULDER -> 0.6f; Kind.MENU -> 0.5f; else -> 1f }
    val wDp = c.size * scale * wf
    val hDp = c.size * scale * hf
    val px = c.x * dim[0] - wDp * density / 2f
    val py = c.y * dim[1] - hDp * density / 2f
    val on = pressed[c.id] == true
    val edge = if (selected && editing) Color.White else ElectricBlue

    val base = Modifier.offset { IntOffset(px.roundToInt(), py.roundToInt()) }
        .size(wDp.dp, hDp.dp).alpha(opacity)

    val m: Modifier = if (editing) {
        base.pointerInput(c.id) {
            detectDragGestures(onDragStart = { onSelect() }, onDragEnd = { store.persist() }) { ch, d ->
                ch.consume(); store.moveBy(c.id, d.x / dim[0], d.y / dim[1])
            }
        }.pointerInput(c.id) { detectTapGestures(onTap = { onSelect() }) }
    } else when (c.kind) {
        Kind.DPAD -> base.pointerInput(c.id) {
            awaitEachGesture {
                val dirs = listOf("UP", "DOWN", "LEFT", "RIGHT")
                fun upd(p: Offset) {
                    val dx = p.x - size.width / 2f
                    val dy = p.y - size.height / 2f
                    val dead = size.width * 0.1f
                    val h = abs(dx) > abs(dy)
                    pressed["LEFT"] = h && dx < -dead
                    pressed["RIGHT"] = h && dx > dead
                    pressed["UP"] = !h && dy < -dead
                    pressed["DOWN"] = !h && dy > dead
                }
                val down = awaitFirstDown(); down.consume(); upd(down.position)
                do {
                    val e = awaitPointerEvent(); e.changes.forEach { it.consume() }
                    upd(e.changes.first().position)
                } while (e.changes.any { it.pressed })
                dirs.forEach { pressed[it] = false }
            }
        }
        Kind.STICK -> base.pointerInput(c.id) {
            awaitEachGesture {
                fun upd(p: Offset) {
                    val cx = size.width / 2f
                    var v = Offset((p.x - cx) / cx, (p.y - cx) / cx)
                    val len = v.getDistance()
                    if (len > 1f) v = v / len
                    axes[c.id] = v
                }
                val down = awaitFirstDown(); down.consume(); pressed[c.id] = true; upd(down.position)
                do {
                    val e = awaitPointerEvent(); e.changes.forEach { it.consume() }
                    upd(e.changes.first().position)
                } while (e.changes.any { it.pressed })
                axes[c.id] = Offset.Zero; pressed[c.id] = false
            }
        }
        else -> base.pointerInput(c.id) {
            awaitEachGesture {
                val down = awaitFirstDown(); down.consume(); pressed[c.id] = true
                do {
                    val e = awaitPointerEvent(); e.changes.forEach { it.consume() }
                } while (e.changes.any { it.pressed })
                pressed[c.id] = false
            }
        }
    }

    when (c.kind) {
        Kind.DPAD -> Canvas(m) {
            val a = size.width / 3f
            fun arm(id: String, x: Float, y: Float) = drawRoundRect(
                if (pressed[id] == true) ElectricBlue else PadBase,
                Offset(x, y), Size(a, a), CornerRadius(12f))
            arm("UP", a, 0f); arm("DOWN", a, 2 * a); arm("LEFT", 0f, a); arm("RIGHT", 2 * a, a)
            drawRoundRect(PadBase, Offset(a, a), Size(a, a), CornerRadius(12f))
        }
        Kind.STICK -> {
            val off = axes[c.id] ?: Offset.Zero
            val reach = wDp * density * 0.3f
            Box(m.clip(CircleShape).background(PadBase).border(2.dp, edge, CircleShape), Alignment.Center) {
                Box(Modifier.size((wDp * 0.4f).dp)
                    .offset { IntOffset((off.x * reach).roundToInt(), (off.y * reach).roundToInt()) }
                    .clip(CircleShape).background(ElectricBlue))
            }
        }
        Kind.FACE -> Box(m.clip(CircleShape).background(if (on) ElectricBlue else PadBase)
            .border(2.dp, edge, CircleShape), Alignment.Center) {
            Text(c.label, color = Color.White, fontSize = (wDp * 0.4f).sp)
        }
        else -> Box(m.clip(RoundedCornerShape(50)).background(if (on) ElectricBlue else PadBase)
            .border(2.dp, edge, RoundedCornerShape(50)), Alignment.Center) {
            Text(c.label, color = Color.White, fontSize = (hDp * 0.45f).sp)
        }
    }
}
