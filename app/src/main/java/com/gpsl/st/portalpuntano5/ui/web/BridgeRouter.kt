package com.gpsl.st.portalpuntano5.ui.web

import com.gpsl.st.portalpuntano5.BuildConfig
import com.gpsl.st.portalpuntano5.core.AppContainer
import com.gpsl.st.portalpuntano5.domain.model.LoginRequest
import com.gpsl.st.portalpuntano5.scanner.DniScanner
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import retrofit2.HttpException

class BridgeRouter(
    private val c: AppContainer,
    private val scanner: DniScanner,
    private val loginLogger: LoginDiagnosticLogger,
    private val homeLogger: HomeDiagnosticLogger
) {
    private val json = Json { encodeDefaults = true }
    private inline fun <reified T> enc(v: T): String = json.encodeToString(v)

    suspend fun handle(route: String, payload: JsonObject): String = when (route) {
        "app.status" -> enc(mapOf("demoMode" to BuildConfig.DEMO_MODE))
        "auth.session" -> enc(c.auth.currentSession())
        "auth.login" -> login(payload)
        "auth.logout" -> { c.auth.logout(); """{"ok":true}""" }
        "auth.scanDni" -> enc(scanner.scan())
        "home.summary" -> homeSummary()
        "health.overview" -> enc(c.portal.healthOverview())
        "shifts.list" -> enc(c.portal.appointments())
        "procedures.list" -> enc(c.portal.procedures())
        "payslips.list" -> enc(c.portal.payslips())
        "credentials.list" -> enc(c.portal.credentials())
        "education.students" -> enc(c.portal.students())
        "judicial.list" -> enc(c.portal.expedients())
        "civil.certificates" -> enc(c.portal.civilCertificates())
        "files.download" -> error("Descarga disponible en la próxima fase")
        else -> error("Ruta desconocida: $route")
    }

    private suspend fun homeSummary(): String {
        try {
            return enc(c.portal.homeSummary())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val logName = runCatching {
                homeLogger.recordFailure(c.session.docNumber, c.session.isLoggedIn, e)
            }.getOrNull()
            val status = (e as? HttpException)?.code()?.let { "HTTP " + it } ?: e.javaClass.simpleName
            val logMessage = if (logName != null) " Diagnóstico: Descargas/PortalPuntano/" + logName else
                " No se pudo crear el archivo de diagnóstico."
            throw IllegalStateException("No se pudo cargar el inicio (" + status + ")." + logMessage, e)
        }
    }

    private suspend fun login(payload: JsonObject): String {
        val docNumber = payload.str("docNumber")
        val sex = payload.str("sex").ifBlank { "M" }
        val password = payload.str("password")

        // Create the Downloads log synchronously before starting the network request.
        val logName = loginLogger.begin(docNumber, sex)
        try {
            val result = c.auth.login(LoginRequest(docNumber, sex, password))
            loginLogger.recordSuccess()
            return enc(result)
        } catch (e: CancellationException) {
            loginLogger.recordFailure("Solicitud cancelada")
            throw e
        } catch (e: HttpException) {
            val body = runCatching { e.response()?.errorBody()?.string().orEmpty() }
                .getOrDefault("(no se pudo leer el cuerpo de error)")
            loginLogger.recordHttpError(e.code(), e.message, body)
            throw IllegalStateException(
                "Inicio de sesión rechazado (HTTP ${e.code()}). Consultá Descargas/PortalPuntano/$logName"
            )
        } catch (e: Exception) {
            loginLogger.recordFailure("${e.javaClass.simpleName}: ${e.message ?: "sin detalle"}")
            throw IllegalStateException(
                "No se pudo iniciar sesión. Consultá Descargas/PortalPuntano/$logName"
            )
        }
    }

    private fun JsonObject.str(key: String): String = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
}
