package com.gpsl.st.portalpuntano5.ui.web

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.gpsl.st.portalpuntano5.BuildConfig
import kotlinx.serialization.json.*
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Privacy-safe append-only diagnostics for every UI section/bridge route. */
class SectionDiagnosticLogger(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private var fileUri: Uri? = null
    private var fileName: String? = null

    @Synchronized
    fun recordSuccess(route: String, elapsedMs: Long, result: String, docNumber: String?, tokenPresent: Boolean) {
        append(buildString {
            appendHeader(route, elapsedMs, docNumber, tokenPresent)
            appendLine("Resultado: ÉXITO")
            appendLine("Respuesta HTTP: 2xx (Retrofit)")
            appendLine("Resumen de respuesta: " + summarize(result))
            appendLine("Fin del evento: " + Date())
            appendLine()
        })
    }

    @Synchronized
    fun recordFailure(route: String, elapsedMs: Long, error: Exception, docNumber: String?, tokenPresent: Boolean) {
        val http = generateSequence(error as Throwable?) { it.cause }.filterIsInstance<HttpException>().firstOrNull()
        val request = http?.response()?.raw()?.request
        val body = runCatching { http?.response()?.errorBody()?.string().orEmpty() }
            .getOrDefault("(no se pudo leer el cuerpo HTTP)")
        append(buildString {
            appendHeader(route, elapsedMs, docNumber, tokenPresent)
            appendLine("Resultado: ERROR")
            appendLine("Tipo de error: " + error.javaClass.name)
            appendLine("Detalle: " + safe(error.message ?: "sin detalle"))
            appendLine("Método HTTP: " + (request?.method ?: "(no disponible)"))
            appendLine("URL solicitada: " + maskSensitiveUrl(request?.url?.toString().orEmpty().ifBlank { "(no disponible)" }))
            appendLine("Respuesta HTTP: " + (http?.code()?.toString() ?: "no disponible"))
            if (http != null) {
                appendLine("Cuerpo de error (máx. 8000 caracteres; secretos redactados):")
                appendLine(redactSecrets(body).take(8000))
            }
            appendLine("Fin del evento: " + Date())
            appendLine()
        })
    }

    @Synchronized
    fun recordCancelled(route: String, elapsedMs: Long, docNumber: String?, tokenPresent: Boolean) {
        append(buildString {
            appendHeader(route, elapsedMs, docNumber, tokenPresent)
            appendLine("Resultado: SOLICITUD CANCELADA")
            appendLine("Fin del evento: " + Date())
            appendLine()
        })
    }

    @Synchronized
    fun currentFileName(): String? = fileName

    private fun StringBuilder.appendHeader(route: String, elapsedMs: Long, docNumber: String?, tokenPresent: Boolean) {
        appendLine("=== EVENTO DE SECCIÓN ===")
        appendLine("Fecha/hora: " + Date())
        appendLine("Dispositivo/API Android: " + Build.MANUFACTURER + " " + Build.MODEL + " / API " + Build.VERSION.SDK_INT)
        appendLine("Sección/ruta: " + safe(route))
        appendLine("Base URL configurada: " + BuildConfig.PORTAL_BASE_URL)
        appendLine("Duración: " + elapsedMs + " ms")
        appendLine("Sesión/token presente: " + if (tokenPresent) "sí (valor omitido)" else "no")
        appendLine("Documento (enmascarado): " + maskDocument(docNumber.orEmpty()))
    }

    private fun summarize(value: String): String = runCatching {
        when (val element = Json.parseToJsonElement(value)) {
            is JsonArray -> "JSON array; elementos=" + element.size
            is JsonObject -> {
                val details = element.entries.joinToString(", ") { (key, item) ->
                    when (item) {
                        is JsonArray -> key + "=array(" + item.size + ")"
                        is JsonObject -> key + "=objeto"
                        JsonNull -> key + "=null"
                        else -> key + "=" + item::class.simpleName
                    }
                }
                "JSON object; campos=[" + details + "]"
            }
            else -> "JSON " + element::class.simpleName
        }
    }.getOrElse { "respuesta recibida; no se pudo resumir JSON: " + safe(it.message ?: "formato desconocido") }

    private fun append(event: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val uri = fileUri ?: createFile().also { fileUri = it }
        val output = resolver.openOutputStream(uri, "wa")
            ?: throw IOException("No se pudo abrir el log de secciones para escritura.")
        output.bufferedWriter(Charsets.UTF_8).use { it.write(event); it.flush() }
    }

    private fun createFile(): Uri {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val name = "PortalPuntano_secciones_" + stamp + ".log"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PortalPuntano")
            put(MediaStore.Downloads.IS_PENDING, 0)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Android no pudo crear el log de secciones.")
        fileName = name
        return uri
    }

    private fun maskDocument(value: String): String {
        val digits = value.filter(Char::isDigit)
        return if (digits.length <= 3) "***" else "*".repeat((digits.length - 3).coerceAtMost(10)) + digits.takeLast(3)
    }

    private fun maskSensitiveUrl(value: String): String {
        var result = value
        result = result.replace(Regex("(?i)(docNumber/)[0-9]{4,}"), "$1[REDACTADO]")
        result = result.replace(Regex("(?i)([?&](?:docNumber|dni|document|token|access_token)=)[^&]+"), "$1[REDACTADO]")
        return safe(result)
    }

    private fun redactSecrets(value: String): String {
        var result = value
        val patterns = listOf(
            Regex("(?i)(\\\"?(?:password|passwd|token|authToken|authorization|access_token|refresh_token)\\\"?\\s*[:=]\\s*\\\"?)[^\\\",}\\s]+") to "$1[REDACTADO]",
            Regex("(?i)(Bearer\\s+)[A-Za-z0-9._~+/=-]+") to "$1[REDACTADO]"
        )
        patterns.forEach { (pattern, replacement) -> result = pattern.replace(result, replacement) }
        return result
    }

    private fun safe(value: String): String = value.replace(Regex("[\\r\\n\\t]+"), " ").take(1500)
}
