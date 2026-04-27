# PLAN — ARGTv: App de Streaming para TVBox Argentina

> **Versión**: 2.0.0
> **Fecha**: 2026-04-27
> **Estado**: EN REVISIÓN
> **CorrelationId**: argtv-v2-20260427
> **Basado en**: v1.0.0 (revisado y corregido)

---

## Changelog v1 → v2

| # | Tipo | Descripción |
|---|------|-------------|
| 1 | 🐛 Fix crítico | `annotationProcessor` → `kapt` para Room y Moshi (código Kotlin, no Java) |
| 2 | 🐛 Fix crítico | Koin agregado a dependencias build.gradle y a Stack Determinado (era referenciado en di/ y §2.3 pero nunca declarado) |
| 3 | 🐛 Fix crítico | Grafo de dependencias corregido: T3 y T4 dependen de T2, no directamente de T1 |
| 4 | 🐛 Fix | Canal 13 y Telefe eliminados de la categoría Entretenimiento (duplicados, ya están en Noticias) |
| 5 | 🐛 Fix | Netflix y HBO removidos de sección "EN VIVO" (no son canales IPTV) |
| 6 | 🐛 Fix | Tabla de Riesgos tenía columna extra sin header — corregida |
| 7 | 🐛 Fix | Tabla de Funcionalidades Opcionales desalineada — corregida |
| 8 | 🐛 Fix | IDs de test `T1-T8` renombrados a `TC1-TC8` para evitar colisión con IDs de tareas |
| 9 | 🐛 Fix | `Settings` en §7.1 eliminada: contradecía §6.3 ("Configuración avanzada — no incluida") |
| 10 | 🐛 Fix | Typo "sipnosis" → "sinopsis" en §5.2 |
| 11 | 🐛 Fix | Carácter corrupto `���` corregido en árbol de paquetes |
| 12 | ➕ Agregado | Tarea `T1b-DI-SETUP`: Koin AppModule (faltaba tarea para el módulo ya presente en la estructura) |
| 13 | ➕ Agregado | Tarea `T2b-DOMAIN-LAYER`: UseCases e interfaces de repositorio (existían en §3.2 pero sin tarea asignada) |
| 14 | ➕ Agregado | ViewModels integrados en cada tarea de UI (mencionados en arquitectura sin tarea que los creara) |
| 15 | ⚠️ Aclaración | §4.3 aclara la estrategia de contenido: lista hardcodeada de IDs + fetch en runtime desde Cuevana3 |

---

## 1. Contexto y Goals

### 1.1 What — Qué estamos construyendo

Una aplicación de streaming para ver TV en vivo (canales argentinos), películas y series en español, optimizada para funcionar en cualquier TVBox Android básico (CPU low-end, Android 5.0+).

### 1.2 Why — Por qué lo construimos

El usuario necesita una alternativa liviana a las apps de streaming existentes (TVBox, Kuit, etc.) que son demasiado pesadas para TVBoxes básicos. El objetivo es que funcione en cualquier "papa" (dispositivo low-end) y sea extremadamente fácil de usar para personas con poco conocimiento técnico.

### 1.3 Goals Principales

| Goal | Métrica de Éxito |
|------|------------------|
| G1. La app inicia y reproduce canales en TVBoxes con Android 5.0, 1GB RAM | Reproducción fluida en ≤10 segundos |
| G2. Navegación 100% con control remoto (D-pad) | Usuario no necesita mouse/teclado |
| G3. UI simple y limpia | Máximo 3 clics para ver contenido |
| G4. APK liviano | < 20MB |
| G5. Contenido estable | Canales vivos (no se caen cada semana) |

### 1.4 Goals Secundarios

| Goal | Descripción |
|------|-------------|
| G6. Favoritos | Usuario puede guardar canales preferidos |
| G7. Reanudar última posición | Películas/series arrancan donde se dejaron |
| G8. Recuperar último canal visto | Al abrir la app, inicia en el último canal |

---

## 2. Tech Stack

### 2.1 Stack Determinado

| Componente | Tecnología | Justificación |
|-----------|------------|---------------|
| **Lenguaje** | Kotlin 1.9.x | Android nativo, sin overhead de runtime |
| **UI Framework** | Leanback (AndroidX) | Hecho para Android TV, liviano |
| **Min SDK** | 21 (Android 5.0) | Llega a cualquier TVBox básico |
| **Target SDK** | 33 | Balance compatibilidad/funcionalidad (no se publica en Play Store) |
| **Video Player** | Media3 ExoPlayer | Soporta M3U, HLS, DASH |
| **Imágenes** | Coil 2.x | Mucho menor que Glide (~500KB vs ~2MB) |
| **Red** | OkHttp + Retrofit | HTTP client estándar |
| **JSON** | Moshi | Ligero, sin reflection overhead |
| **DB Local** | Room | Solo para favoritos y progreso |
| **DI** | Koin 3.x | Inyección de dependencias liviana (~150KB) |
| **Build** | Gradle (Kotlin DSL) | Build system moderno |
| **ProGuard/R8** | Habilitado | Reducción de APK |

### 2.2 Dependencias Externas Requeridas

```kotlin
// build.gradle.kts (project-level)
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.22" apply false  // FIX v2: requerido para Room y Moshi
}

// build.gradle.kts (app-level)
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")  // FIX v2: reemplaza annotationProcessor
}

android {
    compileSdk = 33
    defaultConfig {
        minSdk = 21
        targetSdk = 33
    }
}

dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // Leanback (Android TV)
    implementation("androidx.leanback:leanback:1.0.0")

    // Media3 ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.media3:media3-common:1.2.1")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.15.0")  // FIX v2: era annotationProcessor

    // Images
    implementation("io.coil-kt:coil:2.5.0")

    // Database (Room)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")  // FIX v2: era annotationProcessor

    // DI — Koin (FIX v2: faltaba, ya era referenciado en di/AppModule.kt y §2.3)
    implementation("io.insert-koin:koin-android:3.5.0")
    implementation("io.insert-koin:koin-androidx-viewmodel:3.5.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

### 2.3 Stack NO Usado (Por qué)

| Librería | Razón |
|----------|-------|
| Jetpack Compose | Too heavy (~5MB), incompatible con Android TV básico |
| Material Design 3 | Compose-dependent |
| Navigation Component | Overhead innecesario para app simple |
| Hilt/Dagger | Koin es más ligero (~150KB vs ~3MB) |
| Firebase | No tiene GMS en la mayoría de TVBoxes |

---

## 3. Arquitectura

### 3.1 Pattern: Clean Architecture (Lite)

```
┌─────────────────────────────────────────────────────────────┐
│                      PRESENTATION                           │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Activities, Fragments, ViewModels                   │   │
│  │  (Leanback UI components)                            │   │
│  └──────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│                      DOMAIN                                 │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  UseCases, Repository Interfaces                     │   │
│  └──────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│                      DATA                                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Repository Impl, API Clients, Local DB              │   │
│  │  (OkHttp, Room, M3U Parser)                          │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 Estructura de Paquetes

```
app/src/main/java/com/argtv/
├── di/                              # Koin DI
│   └── AppModule.kt
├── data/
│   ├── api/
│   │   ├── iptv/
│   │   │   ├── M3UParser.kt
│   │   │   └── M3UClient.kt
│   │   └── cuevana/
│   │       ├── CuevanaClient.kt
│   │       └── CuevanaModels.kt
│   ├── db/
│   │   ├── AppDatabase.kt
│   │   ├── FavoriteDao.kt
│   │   └── ProgressDao.kt
│   ├── model/
│   │   ├── Channel.kt
│   │   ├── Movie.kt
│   │   └── Series.kt
│   └── repository/
│       ├── IPTVRepositoryImpl.kt
│       ├── ContentRepositoryImpl.kt
│       └── PreferencesRepository.kt
├── domain/
│   ├── model/
│   │   └── DomainModels.kt
│   ├── repository/                  # Interfaces (implementadas en data/)
│   │   ├── IIPTVRepository.kt
│   │   └── IContentRepository.kt
│   └── usecase/
│       ├── GetChannelsUseCase.kt    # FIX v2: corregido carácter corrupto
│       ├── GetMoviesUseCase.kt
│       ├── GetSeriesUseCase.kt
│       └── ToggleFavoriteUseCase.kt
├── ui/
│   ├── main/
│   │   ├── MainActivity.kt
│   │   └── MainFragment.kt
│   ├── live/
│   │   ├── LiveFragment.kt
│   │   ├── LiveViewModel.kt         # FIX v2: agregado
│   │   └── LiveRowPresenter.kt
│   ├── movies/
│   │   ├── MoviesFragment.kt
│   │   ├── MoviesViewModel.kt       # FIX v2: agregado
│   │   └── MovieDetailFragment.kt
│   ├── series/
│   │   ├── SeriesFragment.kt
│   │   ├── SeriesViewModel.kt       # FIX v2: agregado
│   │   └── SeriesDetailFragment.kt
│   ├── player/
│   │   └── PlayerActivity.kt
│   ├── favorites/
│   │   ├── FavoritesFragment.kt
│   │   └── FavoritesViewModel.kt   # FIX v2: agregado
│   └── common/
│       ├── BaseFragment.kt
│       └── ErrorFragment.kt
├── player/
│   └── TvPlayer.kt
├── util/
│   ├── Constants.kt
│   └── Extensions.kt
└── ARGTvApplication.kt
```

### 3.3 Modelo de Datos

```kotlin
// data/model/Channel.kt
data class Channel(
    val id: String,
    val name: String,
    val logo: String?,
    val url: String,
    val category: String,  // "noticias", "deportes", "entretenimiento", etc.
    val quality: String?   // "SD", "HD", "FHD"
)

// data/model/Movie.kt
data class Movie(
    val id: String,
    val title: String,
    val poster: String,
    val synopsis: String,
    val year: Int,
    val genre: String,
    val rating: Float,
    val url: String
)

// data/model/Series.kt
data class Series(
    val id: String,
    val title: String,
    val poster: String,
    val synopsis: String,
    val seasons: List<Season>
)

data class Season(
    val number: Int,
    val episodes: List<Episode>
)

data class Episode(
    val id: String,
    val number: Int,
    val title: String,
    val url: String
)
```

---

## 4. Fuentes de Contenido

### 4.1 IPTV — Canales en Vivo (Argentina)

| Fuente | URL | Estado | Actualización |
|--------|-----|--------|---------------|
| IPTV-org Argentina | `https://iptv-org.github.io/iptv/countries/ar.m3u` | Activa | Daily |
| IPTV Cat Argentina | `https://iptvcat.org/argentina/m3u` | Activa | Daily |

**Canales esperados**: ~252 canales argentinos

### 4.2 Secciones de Canales (Categorización)

```
📺 EN VIVO
├── 📰 NOTICIAS
│   ├── Canal 13
│   ├── TN
│   ├── Telefe
│   ├── C5N
│   ├── A24
│   └── América TV
├── ⚽ DEPORTES
│   ├── ESPN
│   ├── Fox Sports
│   ├── TyC Sports
│   ├── DirecTV Sports
│   └── ESPN 3
├── 🎬 ENTRETENIMIENTO   ← FIX v2: Canal 13 y Telefe removidos (duplicados con Noticias)
│   ├── El Nueve
│   ├── Canal 26
│   ├── La Nación+
│   └── TV Pública
├── 🏠 INTERIOR
│   ├── Canal 2 Misiones
│   ├── Canal 3 La Pampa
│   ├── Canal 4 Jujuy
│   ├── Canal 9 Televida Mendoza
│   └── (canales locales de provincias)
├── 🎠 INFANTILES
│   ├── Cartoon Network
│   ├── Nickelodeon
│   ├── Disney Channel
│   ├── Disney Junior
│   └── Baby TV
├── 🎵 MÚSICA
│   ├── VTV
│   ├── MTV
│   ├── Telehit
│   └── Warner
├── ✝️ RELIGIOSO
│   ├── TV María
│   ├── Esperanza TV
│   └── otros
└── 🔎 OTROS
```

> **FIX v2**: Netflix y HBO fueron removidos de la categoría Entretenimiento — no son canales IPTV.

### 4.3 Películas y Series

| Fuente | Tipo | Formato |
|--------|------|---------|
| Cuevana3 | API REST | JSON |

**Estrategia de contenido** (FIX v2 — aclaración de ambigüedad del plan original):

La app mantiene una **lista hardcodeada de IDs/slugs** de las películas y series más populares (estabilidad ante cambios de API). En runtime, hace fetch a Cuevana3 para obtener los stream URLs actualizados de esos IDs. Esto combina estabilidad de catálogo con URLs siempre vigentes.

- Si Cuevana3 falla → se muestra el ítem con estado "No disponible" sin crashear
- Los stream URLs nunca se hardcodean

**Géneros previstos**: Acción, Comedia, Terror/Suspenso, Drama, Romance, Documentales, Infantiles

### 4.4 URLs de Contenido (Hardcodeadas en App)

```kotlin
// util/Constants.kt
object ContentUrls {
    const val IPTV_ARGENTINA = "https://iptv-org.github.io/iptv/countries/ar.m3u"
    const val IPTV_CAT = "https://iptvcat.org/argentina/m3u"
    const val CUEVANA_API = "https://api.cuevana3.io"
}
```

---

## 5. User Workflows

### 5.1 Flujo Principal — Ver TV en Vivo

```
[1] Usuario enciende TVBox
    │
    ↓
[2] App ARGTv abre automáticamente
    │
    ↓
[3] Home Screen muestra 3 secciones:
    │   ┌────────────────────────────────────────┐
    │   │ 🔴 EN VIVO  │ 🎬 PELÍCULAS │ 📺 SERIES │
    │   └────────────────────────────────────────┘
    │
    ↓
[4] Usuario navega con D-pad (← →) a "EN VIVO"
    │
    ↓
[5] Grid de canales muestra secciones por categoría
    │
    ↓
[6] Usuario selecciona canal
    │
    ↓
[7] Reproductor ExoPlayer inicia
    │
    ↓
[8] Controles de reproducción (OK = mostrar/ocultar controles)
    │
    ↓
[9] Back = volver al grid de canales
```

### 5.2 Flujo — Ver Películas

```
[1] Desde Home, navegar a "PELÍCULAS"
    │
    ↓
[2] Grid de películas por género
    │
    ↓
[3] Seleccionar película
    │
    ↓
[4] Detalle: póster, título, año, género, rating, sinopsis  ← FIX v2: "sipnosis"
    │
    ↓
    [Opción A] Seleccionar "REPRODUCIR" → Reproductor
    [Opción B] Seleccionar "FAVORITO" ☆ → Guarda en favoritos
```

### 5.3 Flujo — Reanudar Contenido

```
[1] Abrir app
    │
    ↓
[2] Si hay progreso guardado:
    │   - Película/serie → mostrar banner "Continuar viendo"
    │   - Canal → iniciar en último canal
    │
    ↓
[3] Usuario selecciona "Continuar"
    │
    ↓
[4] Reproductor inicia desde posición guardada
    │
    ↓
[5] Cada 10 segundos, guardar progreso en DB local
```

### 5.4 Flujo — Favoritos

```
[1] En cualquier contenido (canal/película/serie)
    │
    ↓
[2] Presionar botón "FAVORITO" (⭐)
    │
    ↓
[3] Guardar en Room DB
    │
    ↓
[4] En Home, sección "⭐ FAVORITOS" muestra guardados
```

---

## 6. Funcionalidades Detalladas

### 6.1 Funcionalidades Obligatorias

| ID | Función | Descripción |
|----|---------|-------------|
| F1 | Reproducción M3U | Parsea y reproduce streams M3U/HLS |
| F2 | Navegación D-pad | 100% compatible con control remoto |
| F3 | Secciones por categoría | Canales agrupados por género |
| F4 | Grid de canales | Visualización en rows/columns |
| F5 | Reproductor video | ExoPlayer con controles básicos |
| F6 | Cambio de canal | D-pad arriba/abajo cambia canal |
| F7 | Favoritos | Guardar canales preferidos |
| F8 | Reanudar | Continuar último contenido |
| F9 | Persistencia | SharedPreferences para estado |

### 6.2 Funcionalidades Opcionales (v1.x)

FIX v2: tabla tenía header con 3 columnas y datos con 4.

| ID | Función | Descripción | Prioridad |
|----|---------|-------------|-----------|
| F10 | Búsqueda | Buscar por nombre | Alta |
| F11 | EPG (básico) | Guía de programación | Baja (no en v1) |
| F12 | Subtítulos | Soporte subtitles | Baja |
| F13 | Chromecast | Enviar a otro dispositivo | Baja |

### 6.3 Funcionalidades NO Incluidas (v1)

- Login/Cuentas (100% pública)
- Auto-actualización de canales (lista fija)
- Configuración avanzada
- Analytics
- Notificaciones push

---

## 7. UI/UX Design

### 7.1 Pantallas

FIX v2: Settings eliminada — contradecía §6.3 "Configuración avanzada — no incluida".

| Pantalla | Tipo | Descripción |
|---------|------|-------------|
| Home | LeanbackFragment | 3 rows: Live, Movies, Series |
| Live Grid | BrowseSupportFragment | Grid de canales por categoría |
| Movie Grid | BrowseSupportFragment | Grid de películas por género |
| Series Grid | BrowseSupportFragment | Grid de series por género |
| Movie Detail | DetailsFragment | Detail page con acciones |
| Series Detail | DetailsFragment | Detail con episodios |
| Player | PlaybackSupportFragment | Fullscreen player |
| Favorites | BrowseSupportFragment | Lista de favoritos |

### 7.2 Color Scheme

| Elemento | Color | Hex |
|----------|-------|-----|
| Primary | Azul Argentina | #006BB4 |
| Primary Dark | Azul oscuro | #004C8C |
| Accent | Amarillo Argentina | #FCD116 |
| Background | Negro | #121212 |
| Surface | Gris oscuro | #1E1E1E |
| Text Primary | Blanco | #FFFFFF |
| Text Secondary | Gris claro | #B3B3B3 |

### 7.3 Layout Principios

- **Rows horizontales**: Contenido en filas horizontales (estilo Leanback)
- **Focus visual**: Elemento seleccionado tiene borde amarillo
- **Texto grande**: Mínimo 18sp para texto visible desde TV
- **Iconos claros**: Mínimo 48dp para controles táctiles
- **Animaciones mínimas**: Solo transitions básicas (100ms)

### 7.4 Manejo de Errores

| Escenario | UX |
|----------|----|
| Sin internet | Mensaje: "Sin conexión. Verificá tu red." + Botón "Reintentar" |
| Canal caído | Saltar al siguiente canal + toast "Canal no disponible" |
| Stream lento | Buffering indicator + "Cargando..." |
| Error genérico | Mostrar error + opción "Volver al inicio" |

---

## 8. Testing Plan

### 8.1 Device Targets

| Dispositivo | Especificación | Target |
|-------------|----------------|--------|
| Genérico TVBox | 1GB RAM, Android 9, S905 | ✅ Primary |
| Genérico TVBox | 512MB RAM, Android 7, S805 | ✅ Secondary |
| Shield TV | 3GB RAM, Android 11 | ✅ Reference |

### 8.2 Casos de Prueba

FIX v2: renombrados T1-T8 → TC1-TC8 para evitar colisión con IDs de tareas de desarrollo.

| ID | Test | Esperado |
|----|------|----------|
| TC1 | Abrir app en TVBox 1GB RAM | Inicia en ≤10 segundos |
| TC2 | Reproducir canal HD | Reproducción fluida |
| TC3 | Navegar D-pad sin lag | Respuesta instantánea |
| TC4 | Cambiar canal con D-pad | Cambio en ≤1 segundo |
| TC5 | Favoritos guardar/cargar | Persistencia correcta |
| TC6 | Reanudar película | Inicia en posición correcta |
| TC7 | Grid con 100+ canales | Scroll suave |
| TC8 | Sin red muestra error | UX clara |

---

## 9. Riesgo y Mitigación

FIX v2: tabla tenía 5 separadores pero solo 4 columnas en el header.

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|-------------|---------|------------|
| R1. Canales IPTV se caen | Alta | Alto | Lista incluye múltiples fuentes (failover) |
| R2. App lenta en low-end | Media | Alto | Optimizar imágenes, lazy load |
| R3. No funciona en cierto TVBox | Media | Alto | Min SDK 21, testing temprano |
| R4. Streams bloqueados por CORS | Baja | Medio | Proxy interno o URL directa |
| R5. API Cuevana3 cambia | Baja | Medio | Catálogo de IDs fijo como fallback |

---

## 10. Tareas y Dependencias

### 10.1 Grafo de Dependencias

FIX v2: grafo original era incorrecto — T3 y T4 dependen de T2 (necesitan los modelos de datos), no directamente de T1.

```
                    ┌─────────────────────┐
                    │  T1-PROJECT-SETUP   │
                    └─────────┬───────────┘
                              │
                              ▼
                    ┌─────────────────────┐
                    │   T1b-DI-SETUP      │
                    └─────────┬───────────┘
                              │
                              ▼
                    ┌─────────────────────┐
                    │   T2-DATA-MODEL     │
                    └─────────┬───────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
     ┌──────────────┐ ┌───────────┐  ┌────────────┐
     │T2b-DOMAIN-  │ │T3-DB-LITE │  │T4-NETWORK  │
     │   LAYER     │ └─────┬─────┘  └─────┬──────┘
     └──────┬──────┘       │               │
            │              └───────┬───────┘
            └──────────────────────┘
                                   │
                                   ▼
                         ┌─────────────────┐
                         │  T5-REP-IMPL    │
                         └────────┬────────┘
                                  │
               ┌──────────────────┼──────────────────┐
               ▼                  ▼                  ▼
      ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
      │ T6-LIVE-UI  │  │T7-MOVIES-UI  │  │T8-SERIES-UI  │
      └──────┬───────┘  └──────┬───────┘  └──────┬───────┘
             │                 │                  │
             └─────────────────┼──────────────────┘
                               │
                               ▼
                      ┌─────────────────┐
                      │   T9-PLAYER     │
                      └────────┬────────┘
                               │
                               ▼
                      ┌─────────────────┐
                      │ T10-TEST-BUILD  │
                      └─────────────────┘
```

### 10.2 Orden Recomendado

**Fase 1: Foundation**
- T1: Project Setup (build.gradle, AndroidManifest, etc.)
- T1b: Koin DI Setup (AppModule)
- T2: Modelos de datos (Channel, Movie, Series)
- T2b: Domain Layer (UseCases, Repository interfaces)
- T3: Room DB setup (favoritos, progreso)
- T4: Networking (M3U parser, API clients)

**Fase 2: Core Logic**
- T5: Repository Implementation

**Fase 3: UI**
- T6: Live TV UI + LiveViewModel
- T7: Movies UI + MoviesViewModel
- T8: Series UI + SeriesViewModel

**Fase 4: Player**
- T9: ExoPlayer Integration

**Fase 5: QA**
- T10: Testing & Build Final

### 10.3 Tareas Detalladas

---

#### T1-PROJECT-SETUP

**Descripción**: Configurar proyecto Android Studio con todas las dependencias base

**Agente**: Android-Developer

**DependsOn**: —

**AcceptanceCriteria**:
- [ ] `gradle build` compila sin errores
- [ ] `gradle assembleDebug` genera APK en `app/build/outputs/apk/debug/`
- [ ] AndroidManifest incluye permisos: `INTERNET`, `ACCESS_NETWORK_STATE`
- [ ] MinSDK=21, TargetSDK=33 configurado en build.gradle
- [ ] Plugin `kapt` declarado en project y app build.gradle
- [ ] Leanback, Media3, Room, Coil, Koin declarados como dependencias

**Estimación**: 2 horas

---

#### T1b-DI-SETUP

**Descripción**: Configurar Koin como DI container de la aplicación

**Agente**: Android-Developer

**DependsOn**: T1-PROJECT-SETUP

**AcceptanceCriteria**:
- [ ] `ARGTvApplication.kt` inicializa Koin con `startKoin { ... }`
- [ ] `di/AppModule.kt` declara módulos para repositories, usecases y viewmodels
- [ ] App arranca sin errores de inyección

**Estimación**: 1 hora

---

#### T2-DATA-MODEL

**Descripción**: Crear modelos de datos para Channel, Movie, Series, Episode

**Agente**: Android-Developer

**DependsOn**: T1b-DI-SETUP

**AcceptanceCriteria**:
- [ ] `data/model/Channel.kt` con campos: id, name, url, logo, category, quality
- [ ] `data/model/Movie.kt` con campos: id, title, poster, synopsis, year, genre, rating, url
- [ ] `data/model/Series.kt`, `Season`, `Episode` con sus campos
- [ ] Modelos anotados con `@JsonClass(generateAdapter = true)` de Moshi

**Estimación**: 1 hora

---

#### T2b-DOMAIN-LAYER

**Descripción**: Crear interfaces de repositorio y UseCases del domain layer

**Agente**: Android-Developer

**DependsOn**: T2-DATA-MODEL

**AcceptanceCriteria**:
- [ ] `domain/repository/IIPTVRepository.kt` con `getChannels(category?)`, `getCategories()`
- [ ] `domain/repository/IContentRepository.kt` con `getMovies(genre?)`, `getSeries()`, `getEpisodes(seriesId)`
- [ ] `domain/usecase/GetChannelsUseCase.kt` implementado
- [ ] `domain/usecase/GetMoviesUseCase.kt` implementado
- [ ] `domain/usecase/GetSeriesUseCase.kt` implementado
- [ ] `domain/usecase/ToggleFavoriteUseCase.kt` implementado
- [ ] UseCases registrados en Koin module

**Estimación**: 2 horas

---

#### T3-DB-LITE

**Descripción**: Configurar Room Database para favoritos y progreso

**Agente**: Android-Developer

**DependsOn**: T2-DATA-MODEL

**AcceptanceCriteria**:
- [ ] Room Database con entidades: `FavoriteChannel`, `WatchProgress`
- [ ] `FavoriteDao` con métodos: add, remove, getAll
- [ ] `ProgressDao` con métodos: save, get, update
- [ ] Instancia singleton de Database provista via Koin

**Estimación**: 2 horas

---

#### T4-NETWORK

**Descripción**: Implementar clientes HTTP y parser M3U

**Agente**: Android-Developer

**DependsOn**: T2-DATA-MODEL

**AcceptanceCriteria**:
- [ ] OkHttp client configurado con timeouts (connect: 10s, read: 15s)
- [ ] `M3UParser` parsea lista M3U a `List<Channel>`
- [ ] Fetch de IPTV-org Argentina funcionando
- [ ] `CuevanaClient` con Retrofit configurado
- [ ] Manejo de errores de red (timeout, unavailable)

**Estimación**: 3 horas

---

#### T5-REP-IMPL

**Descripción**: Implementar repositories (capa Data)

**Agente**: Android-Developer

**DependsOn**: T2b-DOMAIN-LAYER, T3-DB-LITE, T4-NETWORK

**AcceptanceCriteria**:
- [ ] `IPTVRepositoryImpl` implementa `IIPTVRepository` (getChannels, getCategories)
- [ ] `ContentRepositoryImpl` implementa `IContentRepository` (getMovies, getSeries, getEpisodes)
- [ ] `FavoritesRepository` integra Room
- [ ] `PreferencesRepository` maneja SharedPreferences (último canal, estado app)
- [ ] Todos los repositories registrados en Koin module

**Estimación**: 2 horas

---

#### T6-LIVE-UI

**Descripción**: Implementar UI de TV en Vivo + LiveViewModel

**Agente**: Android-Developer

**DependsOn**: T5-REP-IMPL

**AcceptanceCriteria**:
- [ ] `LiveViewModel` expone `StateFlow<List<Channel>>` agrupados por categoría
- [ ] `MainActivity` con Leanback LaunchPoint
- [ ] `LiveFragment` muestra grid de canales por categoría
- [ ] Secciones: Noticias, Deportes, Entretenimiento, Interior, Infantiles, Música, Religioso
- [ ] Click en canal → abre reproductor
- [ ] D-pad navigation funciona sin lag

**Estimación**: 4 horas

---

#### T7-MOVIES-UI

**Descripción**: Implementar UI de Películas + MoviesViewModel

**Agente**: Android-Developer

**DependsOn**: T5-REP-IMPL

**AcceptanceCriteria**:
- [ ] `MoviesViewModel` expone `StateFlow<List<Movie>>` filtrable por género
- [ ] `MoviesFragment` muestra grid por género
- [ ] `MovieDetailFragment` con póster, info + botón "Reproducir"
- [ ] Agregar a favoritos funcional
- [ ] Click → abre reproductor

**Estimación**: 3 horas

---

#### T8-SERIES-UI

**Descripción**: Implementar UI de Series + SeriesViewModel

**Agente**: Android-Developer

**DependsOn**: T5-REP-IMPL

**AcceptanceCriteria**:
- [ ] `SeriesViewModel` expone `StateFlow<List<Series>>`
- [ ] `SeriesFragment` muestra grid por género
- [ ] `SeriesDetailFragment` muestra temporadas/episodios
- [ ] Selector de episodio funcional con D-pad
- [ ] Click en episodio → abre reproductor

**Estimación**: 3 horas

---

#### T9-PLAYER

**Descripción**: Integrar ExoPlayer con persistencia de progreso

**Agente**: Android-Developer

**DependsOn**: T6-LIVE-UI, T7-MOVIES-UI, T8-SERIES-UI

**AcceptanceCriteria**:
- [ ] `PlayerActivity` con ExoPlayer fullscreen
- [ ] Reproduce streams M3U/HLS sin errores
- [ ] Controles: play/pause, seek, volume
- [ ] Guardar progreso cada 10 segundos en Room
- [ ] Reanudar desde última posición funciona
- [ ] Último canal visto se guarda al salir

**Estimación**: 4 horas

---

#### T10-TEST-BUILD

**Descripción**: Testing final y build release

**Agente**: Android-Developer

**DependsOn**: T9-PLAYER

**AcceptanceCriteria**:
- [ ] APK compila en modo Release
- [ ] ProGuard/R8 reduce APK a <20MB
- [ ] App inicia sin crash en emulador Android TV
- [ ] Reproducción de canal M3U funciona
- [ ] Favoritos persiste entre sesiones
- [ ] Reanudar funciona correctamente
- [ ] No hay leaks de memoria evidentes (verificado con Android Profiler)

**Estimación**: 3 horas

---

## 11. Criterios de Aprobación del Plan

```
[x] 1.1 — Goals claramente definidos y medibles
[x] 1.2 — Tech Stack justificado (por qué cada elección)
[x] 2.1 — Arquitectura documentada con diagrama
[x] 2.2 — Estructura de paquetes coherente
[x] 3.1 — Fuentes de contenido verificadas (URLs activas)
[x] 3.2 — Secciones de canales definidas
[x] 4.1 — Todos los workflows documentados
[x] 4.2 — Casos de error contemplados
[x] 5.1 — Tareas con dependencias claras
[x] 5.2 — Orden de ejecución lógico
[x] 5.3 — AcceptanceCriteria específicos
[x] 6.1 — Riesgos identificados y mitigados
[x] 6.2 — Plan auditable con esta checklist
```

---

## 12. Approvals

| Rol | Nombre | Fecha | Estado |
|-----|--------|-------|--------|
| Planner | opencode | 2026-04-27 | ✏️ EN REVISIÓN |
| Reviewer | — | — | ⏳ PENDIENTE |
| Approver | — | — | ⏳ PENDIENTE |

---

## 13. Notas Adicionales

### 13.1 Repos Externos Usados

- **IPTV-org**: `https://github.com/iptv-org/iptv` (MIT License)
- **Cuevana3 API**: Scraping público

### 13.2 Decisiones Tomadas

| Decisión | Razón |
|----------|-------|
| Sin EPG | Consumiría datos constantes, inestable para listas gratuitas |
| IDs hardcodeados + URLs en runtime | Estabilidad de catálogo sin hardcodear stream URLs |
| Min SDK 21 | Llega a TVBoxes de 5+ años |
| Sin Compose | Incompatible con Android TV básico |
| Room lite (solo 2 tablas) | Minimizar uso de DB |
| Koin sobre Hilt | ~150KB vs ~3MB; suficiente para este scope |

### 13.3 Coste Estimado

| Recurso | Estimación |
|---------|------------|
| Tiempo total | ~27 horas (24h originales + 3h tareas nuevas T1b y T2b) |
| Líneas de código | ~3,500-4,500 |
| APK final | ~15-18MB |

---

**End of Plan — v2.0.0**