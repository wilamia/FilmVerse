package com.example.filmverse.Adapters

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import okio.IOException
import java.io.ByteArrayInputStream

class CustomWebViewClient : WebViewClient() {
    private val adServers = listOf(
        "ads.example.com",
        "doubleclick.net",
        "googleadservices.com",
        "adserver.com",
        "usesentry.com" // Добавьте другие домены рекламы здесь
    )
}