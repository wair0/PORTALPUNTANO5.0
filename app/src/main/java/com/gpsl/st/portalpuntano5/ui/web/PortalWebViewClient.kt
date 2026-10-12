package com.gpsl.st.portalpuntano5.ui.web

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat

class PortalWebViewClient(
    private val loader: WebViewAssetLoader,
    private val onPageReady: (WebView) -> Unit
) : WebViewClientCompat() {
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? = loader.shouldInterceptRequest(request.url)

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val uri = request.url
        val isTrustedAssetOrigin =
            uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals(WebViewAssetLoader.DEFAULT_DOMAIN, ignoreCase = true)

        if (isTrustedAssetOrigin) return false

        // Only ordinary web links may leave the app; block arbitrary/custom schemes.
        if (uri.scheme.equals("https", ignoreCase = true) ||
            uri.scheme.equals("http", ignoreCase = true)
        ) {
            runCatching {
                view.context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        }
        return true
    }

    override fun onPageFinished(view: WebView, url: String?) = onPageReady(view)
}
