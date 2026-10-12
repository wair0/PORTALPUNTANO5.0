package com.gpsl.st.portalpuntano5.ui.web

import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.json.JSONObject

class PortalBridge(private val webView: WebView, private val scope: CoroutineScope, private val router: BridgeRouter) {
    @JavascriptInterface fun request(id: Int, route: String, payload: String) {
        scope.launch {
            try {
                val body = runCatching { Json.parseToJsonElement(payload).jsonObject }.getOrDefault(JsonObject(emptyMap()))
                val result = router.handle(route, body)
                eval("window.PortalBridge.resolve($id, ${result.sanitize()})")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                eval("window.PortalBridge.reject($id, ${JSONObject.quote(e.message ?: "Error inesperado")})")
            }
        }
    }
    private fun eval(js: String) = webView.evaluateJavascript(js, null)
    private fun String.sanitize() = replace("\u2028", "\\u2028").replace("\u2029", "\\u2029")
}