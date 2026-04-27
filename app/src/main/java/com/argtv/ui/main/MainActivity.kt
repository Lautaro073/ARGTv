package com.argtv.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.argtv.R
import com.argtv.data.model.Channel
import com.argtv.data.api.M3UClient
import com.argtv.ui.player.PlayerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var channelGrid: RecyclerView
    private lateinit var searchInput: EditText
    private lateinit var categoryTabs: LinearLayout
    private lateinit var adapter: ChannelAdapter
    
    private val allChannels = mutableListOf<Channel>()
    private val filteredChannels = mutableListOf<Channel>()
    private var selectedCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        channelGrid = findViewById(R.id.channel_grid)
        searchInput = findViewById(R.id.search_input)
        categoryTabs = findViewById(R.id.category_tabs)
        
        setupGrid()
        setupSearch()
        loadChannels()
    }

    private fun setupGrid() {
        adapter = ChannelAdapter { channel -> openPlayer(channel) }
        channelGrid.layoutManager = GridLayoutManager(this, 2)
        channelGrid.adapter = adapter
    }

    private fun setupSearch() {
        searchInput.setOnEditorActionListener { _, _, _ ->
            filterChannels()
            true
        }
    }

    private fun openPlayer(channel: Channel) {
        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra("channel_id", channel.id)
            putExtra("channel_url", channel.url)
            putExtra("channel_name", channel.name)
        })
    }

    private fun filterChannels() {
        val query = searchInput.text.toString().lowercase()
        
        filteredChannels.clear()
        
        val filtered = allChannels.filter { channel ->
            val matchesQuery = query.isEmpty() || 
                channel.name.lowercase().contains(query) || 
                channel.category.lowercase().contains(query)
            val matchesCategory = selectedCategory == null || 
                channel.category == selectedCategory
            matchesQuery && matchesCategory
        }
        
        filteredChannels.addAll(filtered)
        adapter.notifyDataSetChanged()
    }

    private fun setupCategories() {
        categoryTabs.removeAllViews()
        
        // "All" tab
        addCategoryTab("Todos", null)
        
        // Category tabs
        allChannels.map { it.category }.distinct().sorted().forEach { category ->
            addCategoryTab(category.replaceFirstChar { it.uppercase() }, category)
        }
        
        filterChannels()
    }

    private fun addCategoryTab(name: String, category: String?) {
        val tab = TextView(this).apply {
            text = name
            setTextColor(if (category == selectedCategory) 0xFFFCD116.toInt() else 0xFF888888.toInt())
            textSize = 14f
            setPadding(24, 12, 24, 12)
            setOnClickListener {
                selectedCategory = category
                setupCategories() // Re-render tabs to update selection
                filterChannels()
            }
        }
        categoryTabs.addView(tab)
    }

    private fun loadChannels() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    M3UClient().fetchChannels()
                }
                result.getOrNull()?.let { channels ->
                    allChannels.clear()
                    allChannels.addAll(channels)
                    filteredChannels.clear()
                    filteredChannels.addAll(channels)
                    setupCategories()
                    adapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    inner class ChannelAdapter(
        private val onClick: (Channel) -> Unit
    ) : RecyclerView.Adapter<ChannelAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_channel, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val channel = filteredChannels[position]
            holder.bind(channel)
        }

        override fun getItemCount() = filteredChannels.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(channel: Channel) {
                itemView.findViewById<TextView>(R.id.channel_name).text = channel.name
                itemView.findViewById<TextView>(R.id.channel_category).text = channel.category
                itemView.setOnClickListener { onClick(channel) }
            }
        }
    }
}