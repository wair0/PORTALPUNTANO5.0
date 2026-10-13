package com.gpsl.st.portalpuntano5.ui.web

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.gpsl.st.portalpuntano5.BuildConfig
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Creates one diagnostic file for each failed home-screen load/retry, without secrets. */
class HomeDiagnosticLogger(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    @Synchronized
    fun recordFailure(docNumber: String?, tokenPresent: Boolean, error: Exception): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val name = "PortalPuntano_inicio_" + stamp + ".log"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PortalPuntano")
            put(MediaStore.Downloads.IS_PENDING, 0)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Android no pudo crear el diagnóstico de pantalla de inicio.")

        val http = error as? HttpException
        val request = http?.response()?.raw()?.request
        val requestUrl = request?.url?.toString()
            ?: "(URL no disponible; error " + error.javaClass.simpleName + ")"
        val safeUrl = maskSensitiveUrl(requestUrl)
        val responseBody = runCatching { http?.response()?.errorBody()?.string().orEmpty() }
            .getOrDefault("(no se pudo leer el cuerpo HTTP)")
        val content = buildString {
            appendLine("=== DIAGNÓSTICO DE PANTALLA DE INICIO ===")
            appendLine("Inicio: " + Date())
            appendLine("Dispositivo/API Android: " + Build.MANUFACTURER + " " + Build.MODEL + " / API " + Build.VERSION.SDK_INT)
            appendLine("Ruta de la app: home.summary (inicio / reintentar)")
            appendLine("Base URL configurada: " + BuildConfig.PORTAL_BASE_URL)
            appendLine("Método HTTP: " + (request?.method ?: "(no disponible)"))
            appendLine("URL solicitada: " + safeUrl)
            appendLine("Sesión/token presente: " + if (tokenPresent) "sí (valor omitido)" else "no")
            appendLine("Documento de sesión (enmascarado): " + maskDocument(docNumber.orEmpty()))
            if (http != null) {
                appendLine("Respuesta HTTP: " + http.code())
                appendLine("Mensaje HTTP: " + safe(http.message()))
                appendLine("Cuerpo de respuesta (máx. 8000 caracteres; secretos redactados):")
                appendLine(redactSecrets(responseBody).take(8000))
            } else {
                appendLine("Tipo de error: " + error.javaClass.name)
                appendLine("Detalle: " + safe(error.message ?: "sin detalle"))
                appendLine("Respuesta HTTP: no disponible")
            }
            appendLine()
            appendLine("La contraseña y los valores de tokens/autorización no se guardan.")
            appendLine("Fin: " + Date())
        }
        resolver.openOutputStream(uri, "w")?.bufferedWriter(Charsets.UTF_8)?.use {
            it.write(content)
            it.flush()
        } ?: throw IOException("No se pudo escribir el diagnóstico de pantalla de inicio.")
        return name
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

    private fun safe(value: String): String =
        value.replace(Regex("[\\r\\n\\t]+"), " ").take(1500)

    private fun redactSecrets(value: String): String {
        var result = value
        val patterns = listOf(
            Regex("(?i)(\\\"?(?:password|passwd|token|authToken|authorization|access_token|refresh_token)\\\"?\\s*[:=]\\s*\\\"?)[^\\\",}\\s]+") to "$1[REDACTADO]",
            Regex("(?i)(Bearer\\s+)[A-Za-z0-9._~+/=-]+") to "$1[REDACTADO]"
        )
        patterns.forEach { (pattern, replacement) -> result = pattern.replace(result, replacement) }
        return result
    }
}
