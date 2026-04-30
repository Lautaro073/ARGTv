# CONTENT-API-MIGRATION.md
# Migración: Cuevana3 API → TMDB + VidSrc

> **Tarea**: Reemplazar completamente la integración con `api.cuevana3.io` (caída) por el stack TMDB + VidSrc.mov
> **Agente**: Backend / Android-Developer
> **DependsOn**: T2-DATA-MODEL, T1b-DI-SETUP

---

## Contexto

`api.cuevana3.io` era una API privada sin documentación que dejó de funcionar. Se reemplaza por:

| Rol | API | Auth | Documentación |
|-----|-----|------|---------------|
| Catálogo + metadata + posters | TMDB (The Movie Database) | API Key gratuita | https://developer.themoviedb.org/docs |
| Stream de películas y series | VidSrc.mov | Sin key | https://vidsrc.mov |

---

## AcceptanceCriteria

- [ ] `CuevanaClient.kt` y `CuevanaModels.kt` eliminados del proyecto
- [ ] `TmdbService.kt` implementado con todos los endpoints requeridos
- [ ] `TmdbModels.kt` con todos los data classes necesarios
- [ ] `VidSrcUrlBuilder.kt` implementado
- [ ] `ContentRepositoryImpl` actualizado para usar `TmdbService`
- [ ] `Constants.kt` actualizado con las nuevas URLs (Cuevana removido)
- [ ] `PlayerActivity` reproduce contenido desde VidSrc (WebView)
- [ ] Posters se cargan desde CDN de TMDB con Coil
- [ ] Sinopsis y títulos en español latinoamericano (`es-419`)
- [ ] `AppModule.kt` (Koin) actualizado con el nuevo cliente

---

## Paso 1 — Dependencias (sin cambios en build.gradle)

No se agrega ninguna librería nueva. El stack existente cubre todo:
- Retrofit → llamadas a TMDB
- Moshi → parseo JSON
- Coil → imágenes desde CDN TMDB
- WebView del sistema → player VidSrc

---

## Paso 2 — Constants.kt

Reemplazar el bloque de Cuevana por el nuevo:

```kotlin
// util/Constants.kt
object Constants {

    // TMDB — registrar cuenta gratis en https://www.themoviedb.org/settings/api
    // Ir a Settings → API → Developer → generar API Key (v3 auth)
    const val TMDB_API_KEY   = "REEMPLAZAR_CON_TU_API_KEY"
    const val TMDB_BASE_URL  = "https://api.themoviedb.org/3/"
    const val TMDB_IMAGE_URL = "https://image.tmdb.org/t/p/w500"    // posters
    const val TMDB_BACKDROP_URL = "https://image.tmdb.org/t/p/w1280" // fondos

    // VidSrc — sin registro, sin API key
    const val VIDSRC_BASE    = "https://vidsrc.mov"

    // IPTV (sin cambios)
    const val IPTV_ARGENTINA = "https://iptv-org.github.io/iptv/countries/ar.m3u"
    const val IPTV_CAT       = "https://iptvcat.org/argentina/m3u"
}
```

---

## Paso 3 — TmdbModels.kt

Crear en `data/api/tmdb/TmdbModels.kt`:

```kotlin
package com.argtv.data.api.tmdb

import com.squareup.moshi.JsonClass

// ── Respuestas paginadas ──────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class TmdbMovieResponse(
    val results: List<TmdbMovie>,
    val page: Int,
    val total_pages: Int,
    val total_results: Int
)

@JsonClass(generateAdapter = true)
data class TmdbSeriesResponse(
    val results: List<TmdbSeries>,
    val page: Int,
    val total_pages: Int,
    val total_results: Int
)

@JsonClass(generateAdapter = true)
data class TmdbSearchResponse(
    val results: List<TmdbSearchResult>,
    val total_results: Int
)

// ── Película ──────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class TmdbMovie(
    val id: Int,
    val title: String,
    val original_title: String,
    val overview: String,            // sinopsis en es-419 si language=es-419
    val poster_path: String?,
    val backdrop_path: String?,
    val release_date: String?,       // "2024-03-15"
    val vote_average: Double,
    val vote_count: Int,
    val genre_ids: List<Int>,
    val adult: Boolean = false
)

@JsonClass(generateAdapter = true)
data class TmdbMovieDetail(
    val id: Int,
    val title: String,
    val overview: String,
    val poster_path: String?,
    val backdrop_path: String?,
    val release_date: String?,
    val runtime: Int?,               // minutos
    val vote_average: Double,
    val genres: List<TmdbGenre>,
    val imdb_id: String?             // "tt1234567" — útil como ID alternativo
)

// ── Serie ─────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class TmdbSeries(
    val id: Int,
    val name: String,
    val original_name: String,
    val overview: String,
    val poster_path: String?,
    val backdrop_path: String?,
    val first_air_date: String?,
    val vote_average: Double,
    val genre_ids: List<Int>
)

@JsonClass(generateAdapter = true)
data class TmdbSeriesDetail(
    val id: Int,
    val name: String,
    val overview: String,
    val poster_path: String?,
    val backdrop_path: String?,
    val first_air_date: String?,
    val vote_average: Double,
    val genres: List<TmdbGenre>,
    val number_of_seasons: Int,
    val number_of_episodes: Int,
    val seasons: List<TmdbSeason>
)

@JsonClass(generateAdapter = true)
data class TmdbSeason(
    val id: Int,
    val season_number: Int,
    val name: String,
    val episode_count: Int,
    val poster_path: String?
)

@JsonClass(generateAdapter = true)
data class TmdbSeasonDetail(
    val season_number: Int,
    val episodes: List<TmdbEpisode>
)

@JsonClass(generateAdapter = true)
data class TmdbEpisode(
    val id: Int,
    val name: String,
    val overview: String,
    val episode_number: Int,
    val season_number: Int,
    val runtime: Int?,
    val still_path: String?
)

// ── Búsqueda multi ────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class TmdbSearchResult(
    val id: Int,
    val media_type: String,   // "movie" | "tv" | "person"
    val title: String?,       // solo en películas
    val name: String?,        // solo en series
    val overview: String?,
    val poster_path: String?,
    val vote_average: Double?
)

// ── Común ─────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class TmdbGenre(
    val id: Int,
    val name: String
)

// ── VidSrc feed models ────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class VidSrcFeedResponse(
    val status: Int,
    val type: String,
    val result: List<VidSrcFeedItem>
)

@JsonClass(generateAdapter = true)
data class VidSrcFeedItem(
    val imdb_id: String?,
    val tmdb_id: String?,
    val title: String?,
    val quality: String?
)
```

---

## Paso 4 — TmdbService.kt

Crear en `data/api/tmdb/TmdbService.kt`:

```kotlin
package com.argtv.data.api.tmdb

import com.argtv.util.Constants
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbService {

    // ── Películas ─────────────────────────────────────────────────────────────

    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String = Constants.TMDB_API_KEY,
        @Query("language") language: String = "es-419",
        @Query("page") page: Int = 1
    ): TmdbMovieResponse

    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("api_key") apiKey: String = Constants.TMDB_API_KEY,
        @Query("language") language: String = "es-419",
        @Query("page") page: Int = 1
    ): TmdbMovieResponse

    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("api_key") apiKey: String = Constants.TMDB_API_KEY,
        @Query("language") language: String = "es-419",
        @Query("page") page: Int = 1
    ): TmdbMovieResponse

    @GET("discover/movie")
    suspend fun getMoviesByGenre(
        @Query("api_key") apiKey: String = Constants.TMDB_API_KEY,
        @Query("language") language: String = "es-419",
        @Query("with_genres") genreId: Int,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("page") page: Int = 1
    ): TmdbMovieResponse

    @GET("movie/{id}")
    suspend fun getMovieDetail(
        @Path("id") tmdbId: Int,
        @Query("api_key") apiKey: String = Constants.TMDB_API_KEY,
        @Query("language") language: String = "es-419"
    ): TmdbMovieDetail

    // ── Series ────────────────────────────────────────────────────────────────

    @GET("tv/popular")
    suspend fun getPopularSeries(
        @Query("api_key") apiKey: String = Constants.TMDB_API_KEY,
        @Query("language") language: String = "es-419",
        @Query("page") page: Int = 1
    ): TmdbSeriesResponse

    @GET("tv/top_rated")
    suspend fun getTopRatedSeries(
        @Query("api_key") apiKey: String = Constants.TMDB_API_KEY,
        @Query("language") language: String = "es-419",
        @Query("page") page: Int = 1
    ): TmdbSeriesResponse

    @GET("tv/{id}")
    suspend fun getSeriesDetail(
        @Path("id") tmdbId: Int,
        @Query("api_key") apiKey: String = Constants.TMDB_API_KEY,
        @Query("language") language: String = "es-419"
    ): TmdbSeriesDetail

    @GET("tv/{id}/season/{season_number}")
    suspend fun getSeasonDetail(
        @Path("id") tmdbId: Int,
        @Path("season_number") season: Int,
        @Query("api_key") apiKey: String = Constants.TMDB_API_KEY,
        @Query("language") language: String = "es-419"
    ): TmdbSeasonDetail

    // ── Búsqueda ──────────────────────────────────────────────────────────────

    @GET("search/multi")
    suspend fun search(
        @Query("api_key") apiKey: String = Constants.TMDB_API_KEY,
        @Query("query") query: String,
        @Query("language") language: String = "es-419",
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    // ── Géneros disponibles ───────────────────────────────────────────────────

    @GET("genre/movie/list")
    suspend fun getMovieGenres(
        @Query("api_key") apiKey: String = Constants.TMDB_API_KEY,
        @Query("language") language: String = "es-419"
    ): GenreListResponse

    @GET("genre/tv/list")
    suspend fun getTvGenres(
        @Query("api_key") apiKey: String = Constants.TMDB_API_KEY,
        @Query("language") language: String = "es-419"
    ): GenreListResponse
}

@JsonClass(generateAdapter = true)
data class GenreListResponse(val genres: List<TmdbGenre>)
```

---

## Paso 5 — VidSrcUrlBuilder.kt

Crear en `util/VidSrcUrlBuilder.kt`:

```kotlin
package com.argtv.util

object VidSrcUrlBuilder {

    private val BASE = Constants.VIDSRC_BASE

    /**
     * URL del player embebido para una película.
     * Cargar esta URL en el WebView del PlayerActivity.
     */
    fun movieUrl(tmdbId: Int): String =
        "$BASE/embed/movie/$tmdbId"

    /**
     * URL del player embebido para un episodio de serie.
     */
    fun episodeUrl(seriesTmdbId: Int, season: Int, episode: Int): String =
        "$BASE/embed/tv/$seriesTmdbId/$season/$episode"

    /**
     * Feed JSON de películas recientes agregadas a VidSrc.
     * Útil para poblar la sección "Recién agregadas" sin llamar a TMDB.
     * Respuesta: VidSrcFeedResponse
     */
    fun latestMoviesFeed(page: Int = 1): String =
        "$BASE/vapi/movie/new/$page"

    /**
     * Feed JSON de series recientes.
     */
    fun latestSeriesFeed(page: Int = 1): String =
        "$BASE/vapi/tv/new/$page"
}
```

---

## Paso 6 — PlayerActivity.kt

Reemplazar el reproductor existente con soporte WebView para VidSrc:

```kotlin
package com.argtv.ui.player

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.*
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.argtv.util.VidSrcUrlBuilder

class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TMDB_ID  = "tmdb_id"
        const val EXTRA_IS_MOVIE = "is_movie"    // true = película, false = serie
        const val EXTRA_SEASON   = "season"
        const val EXTRA_EPISODE  = "episode"
    }

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = FrameLayout(this)
        webView = WebView(this)
        container.addView(webView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        setContentView(container)

        configureWebView()
        loadStream()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false   // autoplay en TVBox
            allowFileAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView, request: WebResourceRequest, error: WebResourceError
            ) {
                // Mostrar pantalla de error si el stream falla
            }
        }
    }

    private fun loadStream() {
        val tmdbId   = intent.getIntExtra(EXTRA_TMDB_ID, -1)
        val isMovie  = intent.getBooleanExtra(EXTRA_IS_MOVIE, true)
        val season   = intent.getIntExtra(EXTRA_SEASON, 1)
        val episode  = intent.getIntExtra(EXTRA_EPISODE, 1)

        if (tmdbId == -1) { finish(); return }

        val url = if (isMovie)
            VidSrcUrlBuilder.movieUrl(tmdbId)
        else
            VidSrcUrlBuilder.episodeUrl(tmdbId, season, episode)

        webView.loadUrl(url)
    }

    // D-pad: Back cierra el player
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) webView.goBack()
            else finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPause()  { super.onPause();  webView.onPause() }
    override fun onResume() { super.onResume(); webView.onResume() }

    override fun onDestroy() {
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }
}
```

**Cómo abrir el PlayerActivity desde cualquier Fragment:**

```kotlin
// Película
val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
    putExtra(PlayerActivity.EXTRA_TMDB_ID,  movie.id)
    putExtra(PlayerActivity.EXTRA_IS_MOVIE, true)
}
startActivity(intent)

// Episodio de serie
val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
    putExtra(PlayerActivity.EXTRA_TMDB_ID,  series.id)
    putExtra(PlayerActivity.EXTRA_IS_MOVIE, false)
    putExtra(PlayerActivity.EXTRA_SEASON,   seasonNumber)
    putExtra(PlayerActivity.EXTRA_EPISODE,  episodeNumber)
}
startActivity(intent)
```

---

## Paso 7 — ContentRepositoryImpl.kt (actualizado)

```kotlin
package com.argtv.data.repository

import com.argtv.data.api.tmdb.TmdbService
import com.argtv.data.model.Movie
import com.argtv.data.model.Series
import com.argtv.domain.repository.IContentRepository
import com.argtv.util.Constants

class ContentRepositoryImpl(
    private val tmdb: TmdbService
) : IContentRepository {

    // ── Películas ─────────────────────────────────────────────────────────────

    override suspend fun getPopularMovies(page: Int): List<Movie> =
        tmdb.getPopularMovies(page = page).results.map { it.toMovie() }

    override suspend fun getMoviesByGenre(genreId: Int, page: Int): List<Movie> =
        tmdb.getMoviesByGenre(genreId = genreId, page = page).results.map { it.toMovie() }

    override suspend fun getMovieDetail(tmdbId: Int): Movie {
        val detail = tmdb.getMovieDetail(tmdbId)
        return Movie(
            id       = detail.id.toString(),
            title    = detail.title,
            poster   = detail.poster_path?.let { Constants.TMDB_IMAGE_URL + it } ?: "",
            synopsis = detail.overview,
            year     = detail.release_date?.take(4)?.toIntOrNull() ?: 0,
            genre    = detail.genres.joinToString(", ") { it.name },
            rating   = detail.vote_average.toFloat(),
            streamUrl = ""   // no se hardcodea — se construye con VidSrcUrlBuilder.movieUrl(detail.id)
        )
    }

    // ── Series ────────────────────────────────────────────────────────────────

    override suspend fun getPopularSeries(page: Int): List<Series> =
        tmdb.getPopularSeries(page = page).results.map { it.toSeries() }

    override suspend fun getSeriesDetail(tmdbId: Int): Series {
        val detail = tmdb.getSeriesDetail(tmdbId)
        return Series(
            id       = detail.id.toString(),
            title    = detail.name,
            poster   = detail.poster_path?.let { Constants.TMDB_IMAGE_URL + it } ?: "",
            synopsis = detail.overview,
            seasons  = detail.seasons.map { s ->
                val seasonDetail = tmdb.getSeasonDetail(detail.id, s.season_number)
                Season(
                    number = s.season_number,
                    episodes = seasonDetail.episodes.map { e ->
                        Episode(
                            id     = e.id.toString(),
                            number = e.episode_number,
                            title  = e.name,
                            url    = ""  // construido en runtime con VidSrcUrlBuilder
                        )
                    }
                )
            }
        )
    }

    // ── Helpers de mapeo ──────────────────────────────────────────────────────

    private fun com.argtv.data.api.tmdb.TmdbMovie.toMovie() = Movie(
        id       = id.toString(),
        title    = title,
        poster   = poster_path?.let { Constants.TMDB_IMAGE_URL + it } ?: "",
        synopsis = overview,
        year     = release_date?.take(4)?.toIntOrNull() ?: 0,
        genre    = "",   // genre_ids se resuelven con el mapa de géneros cacheado
        rating   = vote_average.toFloat(),
        streamUrl = ""
    )

    private fun com.argtv.data.api.tmdb.TmdbSeries.toSeries() = Series(
        id       = id.toString(),
        title    = name,
        poster   = poster_path?.let { Constants.TMDB_IMAGE_URL + it } ?: "",
        synopsis = overview,
        seasons  = emptyList()  // se carga bajo demanda en getSeriesDetail()
    )
}
```

---

## Paso 8 — AppModule.kt (Koin — actualizado)

```kotlin
package com.argtv.di

import com.argtv.data.api.tmdb.TmdbService
import com.argtv.data.repository.ContentRepositoryImpl
import com.argtv.data.repository.IPTVRepositoryImpl
import com.argtv.data.repository.PreferencesRepository
import com.argtv.domain.repository.IContentRepository
import com.argtv.domain.repository.IIPTVRepository
import com.argtv.util.Constants
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

val appModule = module {

    // ── OkHttp ────────────────────────────────────────────────────────────────
    single {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // ── Moshi ─────────────────────────────────────────────────────────────────
    single {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    // ── Retrofit TMDB ─────────────────────────────────────────────────────────
    single {
        Retrofit.Builder()
            .baseUrl(Constants.TMDB_BASE_URL)
            .client(get())
            .addConverterFactory(MoshiConverterFactory.create(get()))
            .build()
            .create(TmdbService::class.java)
    }

    // ── Repositories ─────────────────────────────────────────────────────────
    single<IContentRepository> { ContentRepositoryImpl(tmdb = get()) }
    single<IIPTVRepository>    { IPTVRepositoryImpl(client = get()) }
    single                     { PreferencesRepository(context = get()) }
}
```

---

## Paso 9 — AndroidManifest.xml (agregar permiso WebView)

Agregar dentro de `<application>`:

```xml
<!-- Necesario para que WebView cargue el player de VidSrc -->
<uses-permission android:name="android.permission.INTERNET" />

<!-- Dentro de <application>: -->
<activity
    android:name=".ui.player.PlayerActivity"
    android:screenOrientation="landscape"
    android:configChanges="orientation|keyboardHidden|screenSize"
    android:exported="false" />
```

---

## IDs de géneros TMDB (referencia rápida)

Usar estos IDs en `getMoviesByGenre()`:

| Género | ID Películas | ID Series |
|--------|-------------|-----------|
| Acción | 28 | 10759 |
| Comedia | 35 | 35 |
| Drama | 18 | 18 |
| Terror | 27 | — |
| Animación | 16 | 16 |
| Ciencia Ficción | 878 | 10765 |
| Crimen | 80 | 80 |
| Documental | 99 | 99 |
| Familia | 10751 | 10751 |
| Romance | 10749 | — |
| Thriller | 53 | — |

---

## Archivos a eliminar

```
data/api/cuevana/CuevanaClient.kt     ← ELIMINAR
data/api/cuevana/CuevanaModels.kt     ← ELIMINAR
```

## Archivos a crear

```
data/api/tmdb/TmdbService.kt          ← CREAR (Paso 4)
data/api/tmdb/TmdbModels.kt           ← CREAR (Paso 3)
util/VidSrcUrlBuilder.kt              ← CREAR (Paso 5)
```

## Archivos a modificar

```
util/Constants.kt                     ← MODIFICAR (Paso 2)
data/repository/ContentRepositoryImpl.kt ← MODIFICAR (Paso 7)
di/AppModule.kt                       ← MODIFICAR (Paso 8)
ui/player/PlayerActivity.kt           ← MODIFICAR (Paso 6)
AndroidManifest.xml                   ← MODIFICAR (Paso 9)
```

---

## Notas para el agente

- El campo `streamUrl` en los modelos `Movie` y `Episode` queda vacío en el repositorio. El URL real se construye en el ViewModel justo antes de abrir el `PlayerActivity`, usando `VidSrcUrlBuilder.movieUrl(tmdbId.toInt())`. No hardcodear stream URLs.
- `language = "es-419"` da metadata en español latinoamericano (sinopsis, títulos, géneros). Si TMDB no tiene traducción para un título, devuelve el título original automáticamente.
- La imagen de poster se construye concatenando `Constants.TMDB_IMAGE_URL + poster_path`. Ejemplo: `https://image.tmdb.org/t/p/w500/sCzcYW9h55Wecs.jpg`. Usar Coil para cargarla.
- VidSrc no requiere ninguna API key ni header especial. La URL embed funciona directamente en WebView.
- Si VidSrc está caído (raro pero posible), el fallback es `https://vidsrc.icu/embed/movie/{tmdbId}` — mismo formato de URL, distinto dominio.