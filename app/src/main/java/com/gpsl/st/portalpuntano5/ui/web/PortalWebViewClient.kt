package com.gpsl.st.portalpuntano5.ui.web

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat

class PortalWebViewClient(private val loader: WebViewAssetLoader, private val onPageReady: (WebView) -> Unit) : WebViewClientCompat() {
    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? = loader.shouldInterceptRequest(request.url)
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (request.url.host == WebViewAssetLoader.DEFAULT_DOMAIN) return false
        runCatching { view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(request.url.toString()))) }
        return true
    }
    override fun onPageFinished(view: WebView, url: String?) = onPageReady(view)
}