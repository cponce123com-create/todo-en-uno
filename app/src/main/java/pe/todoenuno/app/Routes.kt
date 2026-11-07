package pe.todoenuno.app

import kotlinx.serialization.Serializable

/** Destinos tipados. Cada herramienta admite sus propias subpantallas. */
@Serializable object HomeRoute
@Serializable object CategoriesRoute
@Serializable object AllToolsRoute
@Serializable object FavoritesRoute
@Serializable object SettingsRoute

/** Deep link: todoenuno://tool/flashlight */
@Serializable data class ToolRoute(val id: String)

/** Ejemplo de pantalla hija real: regresar desde aquí vuelve a la herramienta. */
@Serializable data class ToolGuideRoute(val id: String)
