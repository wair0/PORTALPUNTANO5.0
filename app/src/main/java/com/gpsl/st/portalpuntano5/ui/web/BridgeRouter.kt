package com.gpsl.st.portalpuntano5.ui.web

import com.gpsl.st.portalpuntano5.core.AppContainer
import com.gpsl.st.portalpuntano5.domain.model.LoginRequest
import com.gpsl.st.portalpuntano5.scanner.DniScanner
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

class BridgeRouter(private val c: AppContainer, private val scanner: DniScanner) {
    private val json = Json { encodeDefaults = true }
    private inline fun <reified T> enc(v: T): String = json.encodeToString(v)
    suspend fun handle(route: String, payload: JsonObject): String = when (route) {
        "auth.session" -> enc(c.auth.currentSession())
        "auth.login" -> enc(c.auth.login(LoginRequest(payload.str("docNumber"), payload.str("sex").ifBlank { "M" }, payload.str("password"))))
        "auth.logout" -> { c.auth.logout(); """{"ok":true}""" }
        "auth.scanDni" -> enc(scanner.scan())
        "home.summary" -> enc(c.portal.homeSummary())
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
    private fun JsonObject.str(key: String): String = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
}