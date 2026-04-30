package com.argtv.ui.player

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.argtv.R

class WebPlayerActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val TAG = "WebPlayer"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val embedUrl = intent.getStringExtra("embed_url") ?: ""
        val title = intent.getStringExtra("title") ?: ""

        if (embedUrl.isEmpty()) {
            Toast.makeText(this, "URL no disponible", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val container = FrameLayout(this)
        webView = WebView(this)
        container.addView(webView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        setContentView(container)

        configureWebView()
        webView.loadUrl(embedUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        
        webView.webChromeClient = WebChromeClient()
        
        // Block ads
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url?.contains("ads") == true || url?.contains("pop") == true) {
                    return true
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Remove ad elements via JS
                webView.evaluateJavascript("""
                    document.querySelectorAll('[class*=ad], [id*=ad], .ads, .advertisement').forEach(el => el.remove());
                """, null)
            }

            override fun onReceivedError(
                view: WebView?, 
                request: WebResourceRequest?, 
                error: WebResourceError?
            ) {
                if (request?.url?.toString()?.contains("ads") == true) {
                    return
                }
                super.onReceivedError(view, request, error)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                finish()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }
}