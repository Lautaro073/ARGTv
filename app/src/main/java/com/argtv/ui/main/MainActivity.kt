package com.argtv.ui.main

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.argtv.R
import com.argtv.ui.player.PlayerActivity
import com.argtv.ui.player.WebPlayerActivity
import com.argtv.data.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var contentView: RecyclerView
    private lateinit var loadingView: ProgressBar
    private lateinit var sidebarMenu: LinearLayout
    private lateinit var categoryTitle: TextView
    private lateinit var searchInput: EditText
    private lateinit var adapter: ContentAdapter
    private lateinit var apiClient: ApiClient

    private var currentSection = "live"
    private var searchJob: Job? = null
    private val allChannels = mutableListOf<ApiClient.ChannelApi>()
    private val allMovies = mutableListOf<ApiClient.MediaApi>()
    private val allSeries = mutableListOf<ApiClient.MediaApi>()
    private var filteredItems = mutableListOf<ContentItem>()

    private val sections = listOf(
        SectionItem("En Vivo", "live", R.drawable.ic_live),
        SectionItem("Películas", "movies", R.drawable.ic_movies),
        SectionItem("Series", "series", R.drawable.ic_series)
    )

    data class SectionItem(val name: String, val id: String, val iconRes: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        apiClient = ApiClient()
        contentView = findViewById(R.id.content_grid)
        loadingView = findViewById(R.id.loading)
        sidebarMenu = findViewById(R.id.sidebar_menu)
        categoryTitle = findViewById(R.id.category_title)
        searchInput = findViewById(R.id.search_input)

        setupSidebar()
        setupSearch()
        setupGrid()
        loadContent()
    }

    private fun setupSidebar() {
        sidebarMenu.removeAllViews()
        
        sections.forEach { section ->
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(12, 12, 12, 12)
                setBackgroundColor(if (currentSection == section.id) Color.parseColor("#1A3D1A") else Color.TRANSPARENT)
                tag = section.id
                
                val icon = ImageView(this@MainActivity).apply {
                    setImageResource(section.iconRes)
                    setColorFilter(if (currentSection == section.id) Color.parseColor("#FCD116") else Color.parseColor("#666666"))
                    layoutParams = LinearLayout.LayoutParams(28, 28)
                }
                
                val label = TextView(this@MainActivity).apply {
                    text = section.name
                    textSize = 14f
                    setTextColor(if (currentSection == section.id) Color.parseColor("#FCD116") else Color.parseColor("#666666"))
                    setPadding(12, 0, 0, 0)
                }
                
                addView(icon)
                addView(label)
                
                setOnClickListener {
                    if (currentSection != section.id) {
                        currentSection = section.id
                        searchInput.setText("")
                        setupSidebar()
                        categoryTitle.text = when (section.id) {
                            "live" -> "En Vivo"
                            "movies" -> "Películas"
                            "series" -> "Series"
                            else -> section.name
                        }
                        loadContent()
                    }
                }
            }
            sidebarMenu.addView(container)
        }
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                val query = s?.toString()?.trim() ?: ""
                if (query.isEmpty()) {
                    loadContent()
                } else {
                    searchJob = CoroutineScope(Dispatchers.Main).launch {
                        delay(300) // debounce
                        performSearch(query)
                    }
                }
            }
        })
    }

    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            loadContent()
            return
        }
        
        loadingView.visibility = View.VISIBLE
        contentView.visibility = View.GONE
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                when (currentSection) {
                    "movies" -> {
                        val results = apiClient.searchMovies(query)
                        filteredItems.clear()
                        filteredItems.addAll(results.map { ContentItem.Media(it) })
                        withContext(Dispatchers.Main) {
                            categoryTitle.text = if (results.isEmpty()) "Sin resultados: $query" else "Buscar: $query"
                            adapter.submitList(filteredItems)
                        }
                    }
                    "series" -> {
                        val results = apiClient.searchMovies(query)
                        filteredItems.clear()
                        filteredItems.addAll(results.map { ContentItem.Media(it) })
                        withContext(Dispatchers.Main) {
                            categoryTitle.text = if (results.isEmpty()) "Sin resultados: $query" else "Buscar: $query"
                            adapter.submitList(filteredItems)
                        }
                    }
                    "live" -> {
                        val results = allChannels.filter { it.name.contains(query, ignoreCase = true) }
                        filteredItems.clear()
                        filteredItems.addAll(results.map { ContentItem.Channel(it) })
                        withContext(Dispatchers.Main) {
                            categoryTitle.text = if (results.isEmpty()) "Sin resultados: $query" else "Buscar: $query"
                            adapter.submitList(filteredItems)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                loadingView.visibility = View.GONE
                contentView.visibility = View.VISIBLE
            }
        }
    }

    private fun setupGrid() {
        adapter = ContentAdapter { item ->
            when (item) {
                is ContentItem.Channel -> openPlayer(item)
                is ContentItem.Media -> openPlayer(item)
            }
        }
        contentView.layoutManager = GridLayoutManager(this, 4)
        contentView.adapter = adapter
    }

    private fun loadContent() {
        val query = searchInput.text.toString().trim()
        if (query.isNotEmpty()) {
            performSearch(query)
            return
        }
        
        loadingView.visibility = View.VISIBLE
        contentView.visibility = View.GONE

        CoroutineScope(Dispatchers.Main).launch {
            try {
                when (currentSection) {
                    "live" -> loadChannels()
                    "movies" -> loadMovies()
                    "series" -> loadSeries()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                loadingView.visibility = View.GONE
                contentView.visibility = View.VISIBLE
            }
        }
    }

    private suspend fun loadChannels() {
        val channels = apiClient.getChannels()
        allChannels.clear()
        allChannels.addAll(channels)
        
        withContext(Dispatchers.Main) {
            categoryTitle.text = "En Vivo"
            adapter.submitList(channels.map { ContentItem.Channel(it) })
        }
    }

    private suspend fun loadMovies() {
        val movies = apiClient.getMovies()
        allMovies.clear()
        allMovies.addAll(movies)
        
        withContext(Dispatchers.Main) {
            categoryTitle.text = "Películas"
            adapter.submitList(movies.map { ContentItem.Media(it) })
        }
    }

    private suspend fun loadSeries() {
        val series = apiClient.getSeries()
        allSeries.clear()
        allSeries.addAll(series)
        
        withContext(Dispatchers.Main) {
            categoryTitle.text = "Series"
            adapter.submitList(series.map { ContentItem.Media(it) })
        }
    }

    private fun openPlayer(item: ContentItem.Channel) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                loadingView.visibility = View.VISIBLE
                val stream = apiClient.getStreamUrl(item.channel.id)
                
                withContext(Dispatchers.Main) {
                    loadingView.visibility = View.GONE
                    val intent = Intent(this@MainActivity, PlayerActivity::class.java).apply {
                        putExtra("channel_name", item.channel.name)
                        putExtra("stream_url", stream.url)
                        for ((key, value) in stream.headers) {
                            putExtra("header_$key", value)
                        }
                    }
                    startActivity(intent)
                }
            } catch (e: Exception) {
                loadingView.visibility = View.GONE
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openPlayer(item: ContentItem.Media) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                loadingView.visibility = View.VISIBLE
                
                val embedUrl = if (currentSection == "movies") 
                    apiClient.getMovieEmbedUrl(item.media.id)
                else 
                    apiClient.getSeriesEmbedUrl(item.media.id)
                
                withContext(Dispatchers.Main) {
                    loadingView.visibility = View.GONE
                    if (embedUrl.isNotEmpty()) {
                        startActivity(Intent(this@MainActivity, WebPlayerActivity::class.java).apply {
                            putExtra("embed_url", embedUrl)
                            putExtra("title", item.media.title)
                        })
                    } else {
                        Toast.makeText(this@MainActivity, "No disponible: ${item.media.title}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                loadingView.visibility = View.GONE
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    sealed class ContentItem {
        data class Channel(val channel: ApiClient.ChannelApi) : ContentItem()
        data class Media(val media: ApiClient.MediaApi) : ContentItem()
    }

    inner class ContentAdapter(
        private val onClick: (ContentItem) -> Unit
    ) : RecyclerView.Adapter<ContentAdapter.ViewHolder>() {

        private var items = mutableListOf<ContentItem>()

        fun submitList(list: List<ContentItem>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_content, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(item: ContentItem) {
                val posterView = itemView.findViewById<ImageView>(R.id.item_poster)
                val initialView = itemView.findViewById<TextView>(R.id.item_initial)
                val titleView = itemView.findViewById<TextView>(R.id.item_title)
                val subtitleView = itemView.findViewById<TextView>(R.id.item_subtitle)

                when (item) {
                    is ContentItem.Channel -> {
                        titleView.text = item.channel.name
                        subtitleView.text = item.channel.category.replaceFirstChar { it.uppercase() }
                        initialView.text = item.channel.name.firstOrNull()?.uppercase() ?: "?"
                        initialView.visibility = View.VISIBLE
                        if (item.channel.logo.isNotEmpty()) {
                            posterView.load(item.channel.logo) {
                                listener(onSuccess = { _, _ -> initialView.visibility = View.GONE },
                                    onError = { _, _ -> initialView.visibility = View.VISIBLE })
                            }
                        } else {
                            posterView.setImageDrawable(null)
                        }
                    }
                    is ContentItem.Media -> {
                        titleView.text = item.media.title
                        subtitleView.text = item.media.year
                        initialView.text = item.media.title.firstOrNull()?.uppercase() ?: "?"
                        initialView.visibility = View.VISIBLE
                        if (item.media.poster.isNotEmpty()) {
                            posterView.load(item.media.poster) {
                                listener(onSuccess = { _, _ -> initialView.visibility = View.GONE },
                                    onError = { _, _ -> initialView.visibility = View.VISIBLE })
                            }
                        } else {
                            posterView.setImageDrawable(null)
                        }
                    }
                }

                itemView.setOnClickListener { onClick(item) }
            }
        }
    }
}