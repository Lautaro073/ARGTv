package com.argtv.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
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
    private lateinit var adapter: ChannelAdapter
    private val channels = mutableListOf<Channel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.channel_list)
        adapter = ChannelAdapter()
        listView.adapter = adapter

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val channel = channels[position]
            openPlayer(channel)
        }

        loadChannels()
    }

    private fun openPlayer(channel: Channel) {
        val intent = Intent(this, PlayerActivity::class.java)
        intent.putExtra("channel_url", channel.url)
        intent.putExtra("channel_name", channel.name)
        startActivity(intent)
    }

    private fun loadChannels() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    M3UClient().fetchChannels()
                }
                result.getOrNull()?.let {
                    channels.clear()
                    channels.addAll(it)
                    adapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    inner class ChannelAdapter : BaseAdapter() {
        override fun getCount() = channels.size
        override fun getItem(pos: Int) = channels[pos]
        override fun getItemId(pos: Int) = pos.toLong()

        override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_channel, parent, false)
            val channel = channels[pos]
            view.findViewById<TextView>(R.id.channel_name).text = channel.name
            view.findViewById<TextView>(R.id.channel_category).text = channel.category
            return view
        }
    }
}