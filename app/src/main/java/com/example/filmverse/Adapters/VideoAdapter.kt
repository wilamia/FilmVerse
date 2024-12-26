package com.example.filmverse.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.filmverse.Activities.DetailActivity
import com.example.filmverse.Domian.Item
import com.example.filmverse.R

class VideoAdapter(private val videos: List<Item>, private val listener: DetailActivity) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {
    private val webViews = mutableListOf<WebView>()
    inner class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val videoTitle: TextView = view.findViewById(R.id.videoTitle)
        val webView: WebView = view.findViewById(R.id.webView)
        init {
            webViews.add(webView)  // Добавляем текущий WebView в список
        }
        fun bind(video: Item) {
            videoTitle.text = video.name ?: "Без названия"

            // Проверка наличия URL
            if (video.site == "KINOPOISK_WIDGET" && !video.url.isNullOrEmpty()) {
                webView.settings.javaScriptEnabled = true
                webView.webChromeClient = WebChromeClient()
                webView.webViewClient = WebViewClient()

                // Загружаем видео сразу при отображении элемента
                webView.loadUrl(video.url!!)
                pauseAllVideos()
            } else {
                videoTitle.text = "Недоступно"
                webView.loadUrl("about:blank") // Очищаем WebView для других источников
            }
        }
        private fun pauseAllVideos() {
            for (wv in webViews) {
                wv.loadUrl("about:blank")  // Ставим на паузу, загружая пустую страницу
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.video_item, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(videos[position])
    }

    override fun getItemCount(): Int = videos.size
}