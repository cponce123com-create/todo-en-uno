package pe.todoenuno.app

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado de presentación fuera de Compose:
 * SavedStateHandle restaura búsqueda y categoría si Android recrea el proceso.
 * Los favoritos viven en DataStore y permanecen entre instalaciones/rotaciones.
 */
class HomeViewModel(
    private val savedState: SavedStateHandle,
    private val store: FavoritesStore
) : ViewModel() {
    val query: StateFlow<String> = savedState.getStateFlow("query", "")
    val category: StateFlow<String> = savedState.getStateFlow("category", "Todas")

    val favorites: StateFlow<Set<String>> = store.favorites.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        FavoritesStore.DEFAULT_FAVORITES
    )

    fun onQueryChanged(text: String) {
        savedState["query"] = text.take(120)
    }

    fun onCategorySelected(categoryName: String) {
        savedState["category"] = categoryName
    }

    fun clearSearch() {
        savedState["query"] = ""
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch { store.toggle(id) }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    createSavedStateHandle(),
                    FavoritesStore(context.applicationContext)
                )
            }
        }
    }
}
