package pe.todoenuno.app

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.toolDataStore by preferencesDataStore(name = "todo_en_uno")

class FavoritesStore(private val context: Context) {
    private val key = stringSetPreferencesKey("favorites")
    val favorites = context.toolDataStore.data.map { it[key] ?: DEFAULT_FAVORITES }

    suspend fun toggle(toolId: String) {
        context.toolDataStore.edit { prefs ->
            val updated = (prefs[key] ?: DEFAULT_FAVORITES).toMutableSet()
            if (!updated.add(toolId)) updated.remove(toolId)
            prefs[key] = updated
        }
    }

    companion object {
        val DEFAULT_FAVORITES = setOf("flashlight", "calculator", "converter")
    }
}
