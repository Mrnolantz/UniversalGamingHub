package com.nolan.gaminghub

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class GameType(val label: String) { ANDROID("Android"), PSP("PSP"), PS2("PS2") }

data class Game(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: GameType,
    val source: String,
    val favorite: Boolean = false,
    val lastPlayed: Long = 0L,
    val launches: Int = 0
)

class Library(ctx: Context) {
    val games = mutableStateListOf<Game>()
    private val file = File(ctx.filesDir, "library.json")

    init { load() }

    private fun load() {
        if (!file.exists()) return
        try {
            val arr = JSONArray(file.readText())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                games.add(
                    Game(
                        o.getString("id"), o.getString("title"),
                        GameType.valueOf(o.getString("type")), o.getString("source"),
                        o.optBoolean("favorite"), o.optLong("lastPlayed"), o.optInt("launches")
                    )
                )
            }
        } catch (_: Exception) { }
    }

    private fun save() {
        val arr = JSONArray()
        games.forEach { g ->
            arr.put(
                JSONObject().put("id", g.id).put("title", g.title).put("type", g.type.name)
                    .put("source", g.source).put("favorite", g.favorite)
                    .put("lastPlayed", g.lastPlayed).put("launches", g.launches)
            )
        }
        file.writeText(arr.toString())
    }

    fun add(g: Game): Boolean {
        if (games.any { it.type == g.type && it.source == g.source }) return false
        games.add(g); save(); return true
    }

    private fun replace(id: String, f: (Game) -> Game) {
        val i = games.indexOfFirst { it.id == id }
        if (i >= 0) { games[i] = f(games[i]); save() }
    }

    fun toggleFavorite(id: String) = replace(id) { it.copy(favorite = !it.favorite) }
    fun markPlayed(id: String) = replace(id) { it.copy(lastPlayed = System.currentTimeMillis(), launches = it.launches + 1) }
    fun remove(id: String) { games.removeAll { it.id == id }; save() }
}
