package com.argtv.ui.main

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import com.argtv.R
import com.argtv.data.model.Channel
import com.argtv.data.api.M3UClient
import com.argtv.ui.player.PlayerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var searchInput: EditText
    private lateinit var adapter: ChannelAdapter
    
    private val allChannels = mutableListOf<Channel>()
    private val filteredChannels = mutableListOf<Channel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.channel_list)
        searchInput = findViewById(R.id.search_input)
        
        adapter = ChannelAdapter()
        listView.adapter = adapter

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val channel = filteredChannels[position]
            openPlayer(channel)
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterChannels(s?.toString() ?: "")
            }
        })

        loadChannels()
    }

    private fun openPlayer(channel: Channel) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("channel_id", channel.id)
            putExtra("channel_url", channel.url)
            putExtra("channel_name", channel.name)
        }
        startActivity(intent)
    }

    private fun filterChannels(query: String) {
        filteredChannels.clear()
        if (query.isEmpty()) {
            filteredChannels.addAll(allChannels)
        } else {
            val lowerQuery = query.lowercase()
            allChannels.filter { channel ->
                channel.name.lowercase().contains(lowerQuery) || 
                channel.category.lowercase().contains(lowerQuery)
            }.forEach { filteredChannels.add(it) }
        }
        adapter.notifyDataSetChanged()
    }

    private fun loadChannels() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    M3UClient().fetchChannels()
                }
                result.getOrNull()?.let { fetchedChannels ->
                    allChannels.clear()
                    allChannels.addAll(fetchedChannels)
                    filteredChannels.clear()
                    filteredChannels.addAll(fetchedChannels)
                    adapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    inner class ChannelAdapter : BaseAdapter() {
        override fun getCount() = filteredChannels.size
        override fun getItem(pos: Int) = filteredChannels[pos]
        override fun getItemId(pos: Int) = pos.toLong()

        override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_channel, parent, false)
            val channel = filteredChannels[pos]
            view.findViewById<TextView>(R.id.channel_name).text = channel.name
            view.findViewById<TextView>(R.id.channel_category).text = channel.category
            return view
        }
    }
}