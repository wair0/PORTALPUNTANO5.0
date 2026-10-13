package com.gpsl.st.portalpuntano5.ui.web

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes login diagnostics to the public Downloads folder without recording credentials.
 * MediaStore Downloads is available on Android 10+ and needs no storage permission.
 */
class LoginDiagnosticLogger(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private var currentUri: Uri? = null
    private var currentName: String? = null

    @Synchronized
    fun begin(docNumber: String, sex: String): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw IOException("El registro directo en Descargas requiere Android 10 o posterior.")
        }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val name = "PortalPuntano_login_$stamp.log"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PortalPuntano")
            put(MediaStore.Downloads.IS_PENDING, 0)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Android no pudo crear el archivo de diagnóstico en Descargas.")
        currentUri = uri
        currentName = name
        append(
            "=== DIAGNÓSTICO DE INICIO DE SESIÓN ===\n" +
                "Inicio: ${Date()}\n" +
                "Dispositivo/API Android: ${Build.MANUFACTURER} ${Build.MODEL} / API ${Build.VERSION.SDK_INT}\n" +
                "Endpoint esperado: ${com.gpsl.st.portalpuntano5.BuildConfig.PORTAL_BASE_URL.trimEnd('/')}/token/mobile/login\n" +
                "Método: POST\n" +
                "Content-Type: application/json\n" +
                "Tipo de documento: DNI\n" +
                "Documento (enmascarado): ${maskDocument(docNumber)}\n" +
                "Sexo enviado: ${sex.ifBlank { "(vacío)" }}\n" +
                "Contraseña: OMITIDA POR SEGURIDAD (solo se registra si está presente)\n" +
                "La contraseña y los tokens no se escriben en este archivo.\n\n"
        )
        return name
    }

    @Synchronized
    fun recordHttpError(code: Int, message: String?, responseBody: String?) {
        append(
            "Respuesta HTTP: $code\n" +
                "Mensaje HTTP: ${safe(message.orEmpty())}\n" +
                "Cuerpo de respuesta (máx. 8000 caracteres, secretos redactados):\n" +
                redactSecrets(responseBody.orEmpty()).take(8000) +
                "\n\n"
        )
    }

    @Synchronized
    fun recordFailure(message: String) {
        append("Fallo local/de conexión: ${safe(message)}\n\n")
    }

    @Synchronized
    fun recordSuccess() {
        append("Resultado: el servidor respondió y la app procesó el inicio de sesión correctamente.\nFin: ${Date()}\n")
    }

    @Synchronized
    private fun append(text: String) {
        val uri = currentUri ?: return
        resolver.openOutputStream(uri, "wa")?.bufferedWriter(Charsets.UTF_8)?.use {
            it.write(text)
            it.flush()
        } ?: throw IOException("No se pudo escribir el archivo de diagnóstico.")
    }

    private fun maskDocument(value: String): String {
        val digits = value.filter(Char::isDigit)
        return if (digits.length <= 3) "***" else "*".repeat((digits.length - 3).coerceAtMost(10)) + digits.takeLast(3)
    }

    private fun safe(value: String): String =
        value.replace(Regex("[\\r\\n\\t]+"), " ").take(1000)

    private fun redactSecrets(value: String): String {
        var result = value
        val patterns = listOf(
            Regex("(?i)(\"?(?:password|passwd|token|authToken|authorization|access_token|refresh_token)\"?\\s*[:=]\\s*\"?)[^\",}\\s]+") to "$1[REDACTADO]",
            Regex("(?i)(Bearer\\s+)[A-Za-z0-9._~+/=-]+") to "$1[REDACTADO]"
        )
        patterns.forEach { (pattern, replacement) -> result = pattern.replace(result, replacement) }
        return result
    }
}
