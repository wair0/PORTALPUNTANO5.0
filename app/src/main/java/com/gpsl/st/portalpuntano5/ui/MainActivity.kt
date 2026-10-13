package com.gpsl.st.portalpuntano5.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewFeature
import com.gpsl.st.portalpuntano5.BuildConfig
import com.gpsl.st.portalpuntano5.PortalPuntanoApp
import com.gpsl.st.portalpuntano5.scanner.DniScanner
import com.gpsl.st.portalpuntano5.ui.web.BridgeRouter
import com.gpsl.st.portalpuntano5.ui.web.LoginDiagnosticLogger
import com.gpsl.st.portalpuntano5.ui.web.HomeDiagnosticLogger
import com.gpsl.st.portalpuntano5.ui.web.PortalBridge
import com.gpsl.st.portalpuntano5.ui.web.SectionDiagnosticLogger
import com.gpsl.st.portalpuntano5.ui.web.PortalWebViewClient

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private var insetTop = 0f
    private var insetBottom = 0f

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        val container = (application as PortalPuntanoApp).container
        webView = WebView(this)
        setContentView(webView)
        val loader = WebViewAssetLoader.Builder().addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this)).build()
        webView.apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.apply { javaScriptEnabled = true; allowFileAccess = false; allowContentAccess = false; allowFileAccessFromFileURLs = false; allowUniversalAccessFromFileURLs = false; domStorageEnabled = true; setSupportZoom(false) }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, false)
            webViewClient = PortalWebViewClient(loader) { applyInsets() }
            val diagnostics = LoginDiagnosticLogger(this@MainActivity)
            addJavascriptInterface(
                PortalBridge(this, lifecycleScope, BridgeRouter(container, DniScanner(this@MainActivity), diagnostics, HomeDiagnosticLogger(this@MainActivity), SectionDiagnosticLogger(this@MainActivity))),
                "PortalNative"
            )
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
            loadUrl("https://${WebViewAssetLoader.DEFAULT_DOMAIN}/assets/ui/index.html")
        }
        ViewCompat.setOnApplyWindowInsetsListener(webView) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val d = resources.displayMetrics.density
            insetTop = bars.top / d; insetBottom = bars.bottom / d
            applyInsets(); insets
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { if (webView.canGoBack()) webView.goBack() else finish() }
        })
    }
    private fun applyInsets() { webView.evaluateJavascript("window.PortalUI&&PortalUI.setInsets($insetTop,$insetBottom)", null) }
    override fun onDestroy() { webView.removeJavascriptInterface("PortalNative"); webView.destroy(); super.onDestroy() }
}
