package com.gpsl.st.portalpuntano5.data.remote

import android.text.Html
import com.gpsl.st.portalpuntano5.data.local.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Lee las pantallas oficiales desde Kotlin y convierte su contenido en registros
 * simples para la WebView local. Nunca carga el sitio oficial dentro de la interfaz.
 */
class OfficialPortalDataSource(private val session: SessionStore) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    private val pages = linkedMapOf(
        "health" to ("Portal de Salud" to "/health"),
        "rm" to ("Historia clínica" to "/rm"),
        "consultations" to ("Consultas" to "/consultations"),
        "diagnosis" to ("Diagnósticos" to "/diagnosis"),
        "studies" to ("Estudios" to "/studies"),
        "prescriptions" to ("Recetas" to "/prescriptions"),
        "health-certificate" to ("Certificados de salud" to "/health-certificate"),
        "shifts" to ("Turnos" to "/shifts"),
        "procedures" to ("Mis trámites" to "/proceduresHome"),
        "payslips" to ("Recibos de sueldo" to "/screenError"),
        "credentials" to ("Credenciales" to "/credentials"),
        "education" to ("Educación" to "/students-list"),
        "judicial" to ("Expedientes judiciales" to "/judicialExpedients"),
        "civil" to ("Registro civil" to "/proceedings")
    )

    fun catalog(): String {
        val array = JSONArray()
        pages.forEach { (key, value) ->
            array.put(JSONObject().put("key", key).put("title", value.first).put("path", value.second))
        }
        return JSONObject().put("sections", array).toString()
    }

    suspend fun fetch(key: String): String = withContext(Dispatchers.IO) {
        val entry = pages[key] ?: throw IOException("Sección oficial no permitida.")
        if (!session.isLoggedIn) throw IOException("Iniciá sesión en PP 5.0 para consultar tus datos.")
        val cached = session.getOfficialCache(key)
        try {
            val url = "https://mobilepuntano.sanluis.gob.ar" + entry.second
            val request = Request.Builder().url(url)
                .header("Accept", "text/html,application/json;q=0.9,*/*;q=0.8")
                .header("User-Agent", "PortalPuntano5/5.0 Android")
                .header("X-Authorization", "Bearer " + session.token.orEmpty())
                .header("Authorization", "Bearer " + session.token.orEmpty())
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isRedirect) {
                    throw IOException("El portal oficial redirigió la consulta al inicio de sesión (HTTP ${response.code}). La sesión actual de PP 5.0 todavía no está reconocida por esa web.")
                }
                if (!response.isSuccessful) throw IOException("Portal oficial respondió HTTP ${response.code}.")
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) throw IOException("El portal oficial devolvió una respuesta vacía.")
                if (looksLikeLogin(body)) {
                    throw IOException("El portal oficial no reconoció la sesión actual. No se solicitó un segundo inicio de sesión.")
                }
                val result = parseResponse(key, entry.first, url, response.header("Content-Type").orEmpty(), body)
                session.putOfficialCache(key, result)
                result
            }
        } catch (e: Exception) {
            if (cached != null) {
                val stale = JSONObject(cached).put("cached", true).put("notice", "Sin conexión o actualización fallida; se muestran los últimos datos guardados.")
                stale.toString()
            } else {
                if (e is IOException) throw e
                throw IOException(e.message ?: "No se pudo consultar el portal oficial.", e)
            }
        }
    }

    private fun looksLikeLogin(html: String): Boolean {
        val lower = html.lowercase()
        return (lower.contains("type=\"password\"") || lower.contains("name=\"password\"")) &&
            (lower.contains("iniciar sesión") || lower.contains("iniciar sesion") || lower.contains("login") || lower.contains("contraseña"))
    }

    private fun parseResponse(key: String, title: String, url: String, contentType: String, body: String): String {
        val jsonBody = contentType.contains("json", true) || body.trimStart().let { it.startsWith("{") || it.startsWith("[") }
        if (jsonBody) {
            val records = runCatching { jsonRecords(body) }.getOrDefault(JSONArray())
            return JSONObject()
                .put("key", key).put("title", title).put("source", url)
                .put("format", "json").put("cached", false)
                .put("records", records)
                .put("message", if (records.length() == 0) "La respuesta JSON no contiene registros reconocibles." else "")
                .toString()
        }

        val clean = body
            .replace(Regex("(?is)<(script|style|noscript|svg)[^>]*>.*?</\\1>"), " ")
            .replace(Regex("(?is)<(nav|footer|header)[^>]*>.*?</\\1>"), " ")
        val headings = Regex("(?is)<h[1-4][^>]*>(.*?)</h[1-4]>").findAll(clean)
            .map { plain(it.groupValues[1]) }.filter { it.length in 2..160 }.distinct().take(20).toList()
        val records = JSONArray()
        val tableRows = Regex("(?is)<tr[^>]*>(.*?)</tr>").findAll(clean).map { it.groupValues[1] }.toList()
        for (row in tableRows) {
            val cells = Regex("(?is)<t[dh][^>]*>(.*?)</t[dh]>").findAll(row)
                .map { plain(it.groupValues[1]) }.filter { it.isNotBlank() }.toList()
            if (cells.isNotEmpty()) records.put(JSONObject().put("title", cells.first().take(180)).put("detail", cells.drop(1).joinToString(" · ").take(700)))
        }
        if (records.length() == 0) {
            val items = Regex("(?is)<li[^>]*>(.*?)</li>").findAll(clean)
                .map { plain(it.groupValues[1]) }.filter { it.length >= 3 && it.length <= 500 }.distinct().take(100).toList()
            items.forEach { records.put(JSONObject().put("title", it.take(180)).put("detail", "")) }
        }
        if (records.length() == 0) {
            val paragraphs = Regex("(?is)<p[^>]*>(.*?)</p>").findAll(clean)
                .map { plain(it.groupValues[1]) }.filter { it.length >= 3 && it.length <= 500 }.distinct().take(50).toList()
            paragraphs.forEach { records.put(JSONObject().put("title", it.take(180)).put("detail", "")) }
        }
        val pageText = plain(clean.replace(Regex("(?is)<[^>]+>"), " ")).take(2500)
        if (records.length() == 0 && pageText.isNotBlank()) {
            records.put(JSONObject().put("title", headings.firstOrNull() ?: title).put("detail", pageText))
        }
        return JSONObject().put("key", key).put("title", title).put("source", url)
            .put("format", "html").put("cached", false)
            .put("headings", JSONArray(headings)).put("records", records)
            .put("message", if (records.length() == 0) "La página no contiene registros legibles en HTML estático; puede cargar los datos mediante JavaScript." else "")
            .toString()
    }

    private fun jsonRecords(body: String): JSONArray {
        val out = JSONArray()
        fun walk(value: Any?, depth: Int) {
            if (depth > 7 || out.length() >= 100) return
            when (value) {
                is JSONArray -> for (i in 0 until value.length()) walk(value.opt(i), depth + 1)
                is JSONObject -> {
                    val titleKeys = listOf("title", "name", "nombre", "type", "tipo", "number", "numero", "id", "date", "fecha", "specialty", "especialidad")
                    val detailKeys = listOf("detail", "description", "descripcion", "status", "estado", "date", "fecha", "professional", "profesional", "center", "centro")
                    val title = titleKeys.firstNotNullOfOrNull { k -> value.opt(k)?.takeIf { it !is JSONObject && it !is JSONArray && it != JSONObject.NULL }?.toString()?.takeIf { it.isNotBlank() } }
                    if (title != null) {
                        val detail = detailKeys.mapNotNull { k ->
                            value.opt(k)?.takeIf { it !is JSONObject && it !is JSONArray && it != JSONObject.NULL }?.toString()?.takeIf { it.isNotBlank() && it != title }
                        }.distinct().joinToString(" · ").take(700)
                        out.put(JSONObject().put("title", title.take(180)).put("detail", detail))
                    } else {
                        value.keys().forEach { key -> walk(value.opt(key), depth + 1) }
                    }
                }
            }
        }
        val root: Any = if (body.trimStart().startsWith("[")) JSONArray(body) else JSONObject(body)
        walk(root, 0)
        return out
    }

    @Suppress("DEPRECATION")
    private fun plain(fragment: String): String = Html.fromHtml(fragment.replace(Regex("(?i)<br\\s*/?>"), " ").replace(Regex("(?is)<[^>]+>"), " ")).toString()
        .replace(Regex("\\s+"), " ").trim()
}
