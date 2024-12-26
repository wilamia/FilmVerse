package com.example.filmverse.Activities

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.filmverse.R

class VideoPlayerActivity : AppCompatActivity() {

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setFullscreenMode()

        val webView: WebView = findViewById(R.id.webView)
        val playerUrl = intent.getStringExtra("PLAYER_URL")

        val newWidth = (320 * 0.8).toInt()
        val newHeight = (180 * 0.8).toInt()

        val layoutParams = FrameLayout.LayoutParams(newWidth, newHeight)
        layoutParams.gravity = Gravity.CENTER
        webView.layoutParams = layoutParams

        setupWebView(webView, playerUrl)
        webView.addJavascriptInterface(WebAppInterface(), "Android")
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupWebView(webView: WebView, playerUrl: String?) {
        webView.settings.apply {
            domStorageEnabled = true
            javaScriptEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = false
            setSupportZoom(true)
        }

        webView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_MOVE) {
                v.parent.requestDisallowInterceptTouchEvent(true)
            }
            false
        }

        if (playerUrl != null) {
            webView.loadUrl(playerUrl)
            webView.setWebViewClient(object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (view != null) {
                        view.evaluateJavascript("""
                            const style = document.createElement('style');
                            style.innerHTML = `
                                video {
                                    width: 80% !important;
                                    height: auto !important;
                                    max-width: 100%;
                                    max-height: 100%;
                                }
                            `;
                            document.head.appendChild(style);
                            const videoElement = document.querySelector('video');
                            if (videoElement) {
                                videoElement.setAttribute('controls', '');
                            }
                        """.trimIndent(), null)
                    }
                }
            })
        }
    }

    private fun setFullscreenMode() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        actionBar?.hide() // Hide the action bar if present
    }

    override fun onBackPressed() {
        val webView: WebView = findViewById(R.id.webView)
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun toggleFullscreen() {
            Log.d("WebAppInterface", "toggleFullscreen called")
            runOnUiThread {
                if (window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                    setFullscreenMode()
                    Log.d("WebAppInterface", "Entering fullscreen")
                } else {
                    window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
                    Log.d("WebAppInterface", "Exiting fullscreen")
                }
            }
        }
    }
}