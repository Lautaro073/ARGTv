# STREAM-PROVIDER-MIGRATION.md
# Migración: VidSrc (WebView + ads) → CinePro + ExoPlayer nativo

> **Problema**: VidSrc carga en WebView → ads agresivos → touch redirige fuera de la app
> **Solución**: CinePro Core extrae URLs M3U8 directas → ExoPlayer las reproduce nativamente
> **Resultado**: Sin WebView, sin ads, sin redirecciones, control total del player
>
> **Agente**: Backend + Android-Developer
> **DependsOn**: T5-REP-IMPL, T9-PLAYER

---

## Arquitectura nueva

```
                    ┌──────────────────────┐
                    │      APP Android     │
                    └──────────┬───────────┘
                               │ 1. getStream(tmdbId)
                               ▼
                    ┌──────────────────────┐
                    │   CinePro Core       │  ← self-hosted (tu server/PC/NAS)
                    │   (Node.js + Docker) │
                    └──────────┬───────────┘
                               │ 2. scraping multi-provider
                               ▼
                    ┌──────────────────────┐
                    │  URLs M3U8 directas  │  ← sin WebView, sin ads
                    └──────────┬───────────┘
                               │ 3. stream URL
                               ▼
                    ┌──────────────────────┐
                    │   ExoPlayer nativo   │  ← control total, pausa funciona
                    └──────────────────────┘
```

---

## Parte 1 — Setup de CinePro Core (servidor)

CinePro Core es el backend que corre en tu máquina o en cualquier servidor.
Repo: https://github.com/cinepro-org/core
Docs: https://cinepro.mintlify.app

### Opción A — Docker (recomendada)

```bash
# En tu PC / servidor / Raspberry Pi / VPS
git clone https://github.com/cinepro-org/core.git
cd core

# Configurar variables de entorno
cp .env.example .env
```

Editar `.env`:
```env
TMDB_API_KEY=tu_api_key_de_tmdb
PORT=3000
NODE_ENV=production
```

```bash
# Levantar con Docker Compose
docker compose up -d

# Verificar que funciona
curl http://localhost:3000/health
```

### Opción B — Node.js directo

```bash
git clone https://github.com/cinepro-org/core.git
cd core
npm install
cp .env.example .env
# editar .env con tu TMDB_API_KEY
npm run build
npm start
# Escucha en http://localhost:3000
```

### Cómo acceder desde el TVBox

El TVBox y el servidor tienen que estar en la misma red local (WiFi/LAN).
Obtener la IP local del servidor:

```bash
# Windows PowerShell
ipconfig | findstr "IPv4"

# Linux / Mac
ip addr | grep "inet "
```

Anotar la IP, ejemplo: `192.168.1.100`
La app Android va a llamar a: `http://192.168.1.100:3000`

---

## Parte 2 — API de CinePro (endpoints que usa la app)

### Obtener fuentes de stream para una película

```
GET http://{SERVER_IP}:3000/stream/movie/{tmdbId}
```

Ejemplo:
```
GET http://192.168.1.100:3000/stream/movie/550
```

Respuesta (OMSS format):
```json
{
  "id": "550",
  "type": "movie",
  "sources": [
    {
      "id": "provider-vidplay",
      "url": "https://rr.vipstreams.in/hls/550/master.m3u8",
      "quality": "1080p",
      "type": "hls",
      "headers": {
        "Referer": "https://vidplay.online/"
      }
    },
    {
      "id": "provider-filemoon",
      "url": "https://mooncdn.com/hls/abc123/master.m3u8",
      "quality": "720p",
      "type": "hls",
      "headers": {}
    }
  ]
}
```

### Obtener fuentes de stream para un episodio

```
GET http://{SERVER_IP}:3000/stream/tv/{tmdbId}/{season}/{episode}
```

Ejemplo: Breaking Bad S1E1
```
GET http://192.168.1.100:3000/stream/tv/1396/1/1
```

---

## Parte 3 — Cambios en la app Android

### 3.1 — Constants.kt (agregar CinePro URL)

```kotlin
// util/Constants.kt
object Constants {
    // TMDB (sin cambios)
    const val TMDB_API_KEY      = "TU_API_KEY"
    const val TMDB_BASE_URL     = "https://api.themoviedb.org/3/"
    const val TMDB_IMAGE_URL    = "https://image.tmdb.org/t/p/w500"

    // CinePro Core — IP de tu servidor en la red local
    // IMPORTANTE: cambiar esta IP por la de tu servidor
    const val CINEPRO_BASE_URL  = "http://192.168.1.100:3000"

    // IPTV (sin cambios)
    const val IPTV_ARGENTINA    = "https://iptv-org.github.io/iptv/countries/ar.m3u"
}
```

### 3.2 — CineProModels.kt (nuevo archivo)

Crear en `data/api/cinepro/CineProModels.kt`:

```kotlin
package com.argtv.data.api.cinepro

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CineProStreamResponse(
    val id: String,
    val type: String,           // "movie" | "tv"
    val sources: List<CineProSource>
)

@JsonClass(generateAdapter = true)
data class CineProSource(
    val id: String,             // nombre del provider
    val url: String,            // URL M3U8 directa ← esto va a ExoPlayer
    val quality: String?,       // "1080p", "720p", "480p"
    val type: String,           // "hls" | "mp4"
    val headers: Map<String, String>  // headers necesarios para el stream
)
```

### 3.3 — CineProService.kt (nuevo archivo)

Crear en `data/api/cinepro/CineProService.kt`:

```kotlin
package com.argtv.data.api.cinepro

import retrofit2.http.GET
import retrofit2.http.Path

interface CineProService {

    @GET("stream/movie/{tmdbId}")
    suspend fun getMovieSources(
        @Path("tmdbId") tmdbId: Int
    ): CineProStreamResponse

    @GET("stream/tv/{tmdbId}/{season}/{episode}")
    suspend fun getEpisodeSources(
        @Path("tmdbId") tmdbId: Int,
        @Path("season") season: Int,
        @Path("episode") episode: Int
    ): CineProStreamResponse
}
```

### 3.4 — AppModule.kt (agregar Retrofit CinePro)

```kotlin
// di/AppModule.kt — agregar dentro del module { }

// Retrofit separado para CinePro (distinta base URL que TMDB)
single(named("cinepro")) {
    Retrofit.Builder()
        .baseUrl(Constants.CINEPRO_BASE_URL + "/")
        .client(get())
        .addConverterFactory(MoshiConverterFactory.create(get()))
        .build()
        .create(CineProService::class.java)
}
```

### 3.5 — PlayerActivity.kt (reemplazar WebView por ExoPlayer nativo)

Reemplazar TODO el `PlayerActivity` actual (que usaba WebView) por este:

```kotlin
package com.argtv.ui.player

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import com.argtv.data.api.cinepro.CineProService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

@UnstableApi
class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TMDB_ID  = "tmdb_id"
        const val EXTRA_IS_MOVIE = "is_movie"
        const val EXTRA_SEASON   = "season"
        const val EXTRA_EPISODE  = "episode"
    }

    private lateinit var playerView: PlayerView
    private lateinit var progressBar: ProgressBar
    private var player: ExoPlayer? = null

    // Koin inyecta el CineProService
    private val cineProService: CineProService by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Layout programático (sin XML necesario)
        val container = FrameLayout(this)
        playerView = PlayerView(this)
        progressBar = ProgressBar(this).apply {
            visibility = View.VISIBLE
        }
        container.addView(playerView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        container.addView(progressBar, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).also { it.gravity = android.view.Gravity.CENTER })
        setContentView(container)

        fetchAndPlay()
    }

    private fun fetchAndPlay() {
        val tmdbId  = intent.getIntExtra(EXTRA_TMDB_ID, -1)
        val isMovie = intent.getBooleanExtra(EXTRA_IS_MOVIE, true)
        val season  = intent.getIntExtra(EXTRA_SEASON, 1)
        val episode = intent.getIntExtra(EXTRA_EPISODE, 1)

        if (tmdbId == -1) { finish(); return }

        lifecycleScope.launch {
            try {
                // 1. Pedir fuentes a CinePro
                val response = withContext(Dispatchers.IO) {
                    if (isMovie)
                        cineProService.getMovieSources(tmdbId)
                    else
                        cineProService.getEpisodeSources(tmdbId, season, episode)
                }

                // 2. Elegir la mejor fuente disponible
                //    Prioridad: 1080p HLS > 720p HLS > primera disponible
                val source = response.sources
                    .filter { it.type == "hls" }
                    .maxByOrNull { it.quality?.replace("p", "")?.toIntOrNull() ?: 0 }
                    ?: response.sources.firstOrNull()

                if (source == null) {
                    showError("No se encontraron fuentes de stream")
                    return@launch
                }

                // 3. Reproducir con ExoPlayer nativo
                withContext(Dispatchers.Main) {
                    playStream(source.url, source.headers)
                }

            } catch (e: Exception) {
                showError("Error al conectar con el servidor: ${e.message}")
            }
        }
    }

    private fun playStream(url: String, headers: Map<String, String>) {
        progressBar.visibility = View.GONE

        // Configurar DataSource con los headers que requiere el provider
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(headers)

        val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(url))

        player = ExoPlayer.Builder(this).build().also { exo ->
            playerView.player = exo
            exo.setMediaSource(mediaSource)
            exo.playWhenReady = true
            exo.prepare()

            exo.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    // Si falla esta fuente, podría intentar con la siguiente
                    showError("Error de reproducción: ${error.message}")
                }
            })
        }
    }

    private fun showError(msg: String) {
        runOnUiThread {
            progressBar.visibility = View.GONE
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // D-pad: OK = mostrar/ocultar controles
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                playerView.showController()
                true
            }
            KeyEvent.KEYCODE_BACK -> {
                player?.stop()
                finish()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onPause()  { super.onPause();  player?.pause() }
    override fun onResume() { super.onResume(); player?.play() }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}
```

### 3.6 — Agregar CineProService en el módulo Koin del ViewModel

En cada ViewModel que necesite streams (MoviesViewModel, SeriesViewModel), inyectar `CineProService` y llamarlo justo antes de abrir `PlayerActivity`:

```kotlin
// Ejemplo en MoviesViewModel.kt
class MoviesViewModel(
    private val contentRepo: IContentRepository,
    private val cineProService: CineProService   // inyectado por Koin
) : ViewModel() {

    fun onMovieSelected(movie: Movie, context: Context) {
        viewModelScope.launch {
            // No hace falta pre-fetchear: PlayerActivity lo hace al abrirse
            val intent = Intent(context, PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_TMDB_ID,  movie.id.toInt())
                putExtra(PlayerActivity.EXTRA_IS_MOVIE, true)
            }
            context.startActivity(intent)
        }
    }
}
```

---

## Parte 4 — Fallback multi-fuente (opcional pero recomendado)

Si la primera fuente M3U8 falla, intentar con la siguiente automáticamente:

```kotlin
// En PlayerActivity.kt — reemplazar playStream() por esta versión con retry

private var sourceIndex = 0
private var availableSources: List<com.argtv.data.api.cinepro.CineProSource> = emptyList()

private fun playBestSource(sources: List<com.argtv.data.api.cinepro.CineProSource>) {
    availableSources = sources
        .filter { it.type == "hls" }
        .sortedByDescending { it.quality?.replace("p", "")?.toIntOrNull() ?: 0 }
    sourceIndex = 0
    tryNextSource()
}

private fun tryNextSource() {
    if (sourceIndex >= availableSources.size) {
        showError("Ninguna fuente disponible")
        return
    }
    val source = availableSources[sourceIndex]
    progressBar.visibility = View.GONE

    val dataSourceFactory = DefaultHttpDataSource.Factory()
        .setDefaultRequestProperties(source.headers)

    val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
        .createMediaSource(MediaItem.fromUri(source.url))

    player?.release()
    player = ExoPlayer.Builder(this).build().also { exo ->
        playerView.player = exo
        exo.setMediaSource(mediaSource)
        exo.playWhenReady = true
        exo.prepare()

        exo.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                // Fuente falló → intentar con la siguiente
                sourceIndex++
                tryNextSource()
            }
        })
    }
}
```

---

## Resumen de archivos

### Eliminar (reemplazado)
```
ui/player/PlayerActivity.kt     ← REESCRIBIR completo (sin WebView)
```

### Crear
```
data/api/cinepro/CineProService.kt      ← CREAR
data/api/cinepro/CineProModels.kt       ← CREAR
```

### Modificar
```
util/Constants.kt               ← agregar CINEPRO_BASE_URL
di/AppModule.kt                 ← agregar Retrofit CinePro (named "cinepro")
```

---

## Por qué esto resuelve el problema

| Problema con VidSrc WebView | Solución con CinePro + ExoPlayer |
|----------------------------|----------------------------------|
| Ads dentro del player HTML | Sin WebView = sin ads HTML |
| Touch redirige a publicidad | Touch maneja ExoPlayer (pausa/seek) |
| Sin control del UI | PlayerView de Media3 100% controlable |
| No podés pausar | Pausa nativa, seek, volumen, subtítulos |
| Player se cierra solo | ExoPlayer corre en proceso nativo |
| Dependés del dominio de VidSrc | CinePro es tuyo, no depende de nadie |

---

## Notas para el agente

- `CINEPRO_BASE_URL` debe ser la IP local del servidor donde corre CinePro Core. No usar `localhost` desde el TVBox (localhost del TVBox es el mismo TVBox, no el servidor).
- Si el servidor no está disponible, mostrar un Toast: "Servidor de streaming no disponible. Verificá que CinePro esté corriendo."
- Los `headers` en `CineProSource` son obligatorios para algunos providers (algunos requieren un `Referer` específico). Si no se incluyen, ExoPlayer obtiene 403.
- CinePro es alpha — puede que algunos providers fallen. El fallback multi-fuente del Paso 4 cubre esto.
- Para TVBox sin acceso a internet externo pero con red local: CinePro puede correr en el mismo router/NAS/Raspberry Pi de la red.
