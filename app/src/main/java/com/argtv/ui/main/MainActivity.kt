package com.argtv.ui.main

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
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

    private lateinit var carousel: RecyclerView
    private lateinit var categoryTitle: TextView
    private lateinit var categoryButtons: LinearLayout
    private lateinit var adapter: ChannelAdapter
    
    private val allChannels = mutableListOf<Channel>()
    private var currentCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        carousel = findViewById(R.id.channel_carousel)
        categoryTitle = findViewById(R.id.category_title)
        categoryButtons = findViewById(R.id.category_buttons)
        
        setupCarousel()
        loadChannels()
    }

    private fun setupCarousel() {
        adapter = ChannelAdapter { channel -> openPlayer(channel) }
        carousel.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        carousel.adapter = adapter
    }

    private fun openPlayer(channel: Channel) {
        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra("channel_id", channel.id)
            putExtra("channel_url", channel.url)
            putExtra("channel_name", channel.name)
        })
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
                    setupCategories()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupCategories() {
        categoryButtons.removeAllViews()
        
        // "Todos" button
        addCategoryButton("Todos", null)
        
        // Category buttons
        allChannels.map { it.category }.distinct().sorted().forEach { category ->
            addCategoryButton(category.replaceFirstChar { it.uppercase() }, category)
        }
        
        updateCarousel()
    }

    private fun addCategoryButton(name: String, category: String?) {
        val btn = TextView(this).apply {
            text = name
            textSize = 16f
            setPadding(32, 16, 32, 16)
            
            val isSelected = currentCategory == category
            setTextColor(if (isSelected) Color.parseColor("#FCD116") else Color.parseColor("#888888"))
            background = if (isSelected) resources.getDrawable(R.drawable.category_selected, null) else null
            
            setOnClickListener {
                currentCategory = category
                setupCategories()
            }
        }
        categoryButtons.addView(btn)
    }

    private fun updateCarousel() {
        val category = currentCategory ?: "Todos"
        categoryTitle.text = category
        
        val filtered = if (currentCategory == null) {
            allChannels
        } else {
            allChannels.filter { it.category == currentCategory }
        }
        
        adapter.submitList(filtered)
    }

    inner class ChannelAdapter(
        private val onClick: (Channel) -> Unit
    ) : RecyclerView.Adapter<ChannelAdapter.ViewHolder>() {

        private var channels = mutableListOf<Channel>()

        fun submitList(list: List<Channel>) {
            channels.clear()
            channels.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_channel, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(channels[position])
        }

        override fun getItemCount() = channels.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(channel: Channel) {
                itemView.findViewById<TextView>(R.id.channel_name).text = channel.name
                itemView.findViewById<TextView>(R.id.channel_category).text = channel.category
                itemView.findViewById<TextView>(R.id.channel_icon).text = channel.name.firstOrNull()?.uppercase() ?: "📺"
                itemView.setOnClickListener { onClick(channel) }
            }
        }
    }
}