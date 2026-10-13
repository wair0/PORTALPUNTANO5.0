package com.gpsl.st.portalpuntano5.data.repository

import android.util.Base64
import com.gpsl.st.portalpuntano5.data.demo.DemoData
import com.gpsl.st.portalpuntano5.data.local.SessionStore
import com.gpsl.st.portalpuntano5.data.remote.PortalApi
import com.gpsl.st.portalpuntano5.data.remote.dto.LoginRequestDto
import com.gpsl.st.portalpuntano5.domain.model.*
import com.gpsl.st.portalpuntano5.domain.repository.AuthRepository

class AuthRepositoryImpl(private val api: PortalApi, private val session: SessionStore, private val demo: Boolean) : AuthRepository {
    override fun currentSession() = if (session.isLoggedIn) SessionState(true, profileFromStore()) else SessionState(false)
    override suspend fun login(request: LoginRequest): SessionState {
        if (demo) {
            session.saveSession("demo", request.docNumber, DemoData.profile.fullName)
            return currentSession()
        }
        val res = api.login(LoginRequestDto("DNI", request.docNumber, request.sex, request.password))
        val token = res.authToken?.token?.takeIf { it.isNotBlank() } ?: error("El servidor no devolvió un token de sesión válido")
        session.saveSession(token, request.docNumber, res.user?.fullName ?: res.user?.name ?: claim(token, "name") ?: request.docNumber)
        return currentSession()
    }
    override fun logout() = session.clear()
    private fun profileFromStore(): Profile {
        val name = session.fullName ?: "Ciudadano"
        val initials = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
        return Profile(name, session.docNumber ?: "", initials.ifBlank { "PP" })
    }
    private fun claim(jwt: String, key: String): String? = runCatching {
        val payload = String(Base64.decode(jwt.split(".")[1], Base64.URL_SAFE or Base64.NO_PADDING))
        Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"").find(payload)?.groupValues?.get(1)
    }.getOrNull()
}