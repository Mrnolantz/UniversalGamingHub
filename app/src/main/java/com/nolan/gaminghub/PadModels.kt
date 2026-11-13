package com.nolan.gaminghub

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class Kind { DPAD, STICK, FACE, SHOULDER, MENU }

data class Control(val id: String, val label: String, val kind: Kind, val x: Float, val y: Float, val size: Float)
data class PadProfile(val name: String, val opacity: Float, val controls: List<Control>)

fun defaultControls(): List<Control> = listOf(
    Control("L2", "L2", Kind.SHOULDER, 0.08f, 0.07f, 56f),
    Control("L1", "L1", Kind.SHOULDER, 0.08f, 0.19f, 56f),
    Control("R2", "R2", Kind.SHOULDER, 0.92f, 0.07f, 56f),
    Control("R1", "R1", Kind.SHOULDER, 0.92f, 0.19f, 56f),
    Control("SELECT", "Select", Kind.MENU, 0.42f, 0.10f, 44f),
    Control("START", "Start", Kind.MENU, 0.58f, 0.10f, 44f),
    Control("DPAD", "", Kind.DPAD, 0.14f, 0.46f, 110f),
    Control("LS", "L", Kind.STICK, 0.33f, 0.78f, 100f),
    Control("RS", "R", Kind.STICK, 0.67f, 0.78f, 100f),
    Control("Y", "Y", Kind.FACE, 0.88f, 0.28f, 52f),
    Control("X", "X", Kind.FACE, 0.80f, 0.44f, 52f),
    Control("B", "B", Kind.FACE, 0.94f, 0.44f, 52f),
    Control("A", "A", Kind.FACE, 0.88f, 0.60f, 52f)
)

class PadStore(ctx: Context) {
    val profiles = mutableStateListOf<PadProfile>()
    var active by mutableStateOf(0)
    private val file = File(ctx.filesDir, "pads.json")

    init {
        load()
        if (profiles.isEmpty()) profiles.add(PadProfile("Classic", 0.7f, defaultControls()))
        active = active.coerceIn(0, profiles.lastIndex)
    }

    val current: PadProfile get() = profiles[active.coerceIn(0, profiles.lastIndex)]

    private fun load() {
        if (!file.exists()) return
        try {
            val root = JSONObject(file.readText())
            val arr = root.getJSONArray("profiles")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val ca = o.getJSONArray("controls")
                val cs = (0 until ca.length()).map { j ->
                    val c = ca.getJSONObject(j)
                    Control(c.getString("id"), c.getString("label"), Kind.valueOf(c.getString("kind")),
                        c.optDouble("x").toFloat(), c.optDouble("y").toFloat(), c.optDouble("size").toFloat())
                }
                profiles.add(PadProfile(o.getString("name"), o.optDouble("opacity", 0.7).toFloat(), cs))
            }
            active = root.optInt("active", 0)
        } catch (_: Exception) { profiles.clear() }
    }

    fun persist() {
        val arr = JSONArray()
        profiles.forEach { p ->
            val ca = JSONArray()
            p.controls.forEach { c ->
                ca.put(JSONObject().put("id", c.id).put("label", c.label).put("kind", c.kind.name)
                    .put("x", c.x.toDouble()).put("y", c.y.toDouble()).put("size", c.size.toDouble()))
            }
            arr.put(JSONObject().put("name", p.name).put("opacity", p.opacity.toDouble()).put("controls", ca))
        }
        file.writeText(JSONObject().put("active", active).put("profiles", arr).toString())
    }

    private fun updateControls(f: (Control) -> Control) {
        val p = current
        profiles[active] = p.copy(controls = p.controls.map(f))
    }

    fun moveBy(id: String, dx: Float, dy: Float) = updateControls {
        if (it.id == id) it.copy(x = (it.x + dx).coerceIn(0.03f, 0.97f), y = (it.y + dy).coerceIn(0.05f, 0.95f)) else it
    }
    fun resize(id: String, size: Float) = updateControls { if (it.id == id) it.copy(size = size) else it }
    fun setOpacity(v: Float) { profiles[active] = current.copy(opacity = v) }
    fun select(i: Int) { active = i; persist() }
    fun resetActive() { profiles[active] = current.copy(controls = defaultControls()); persist() }
    fun addProfile(name: String) {
        profiles.add(current.copy(name = name.ifBlank { "Profile ${profiles.size + 1}" }))
        active = profiles.lastIndex; persist()
    }
    fun deleteActive() {
        if (profiles.size <= 1) return
        profiles.removeAt(active); active = 0; persist()
    }
}
