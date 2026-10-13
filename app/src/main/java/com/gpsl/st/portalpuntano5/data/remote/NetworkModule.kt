package com.gpsl.st.portalpuntano5.data.remote

import com.gpsl.st.portalpuntano5.BuildConfig
import com.gpsl.st.portalpuntano5.data.local.SessionStore
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class NetworkModule(private val session: SessionStore) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true; explicitNulls = false }
    private val auth = Interceptor { chain ->
        val configuredBaseUrl = BuildConfig.PORTAL_BASE_URL.trim()
        if (configuredBaseUrl.isBlank()) {
            throw IOException("Portal Puntano real no está configurado: falta PORTAL_BASE_URL.")
        }
        // Compatibilidad con la URL exacta que usa el código fuente de Portal Puntano 4.0.
        // Atención: este endpoint heredado usa HTTP y no cifra credenciales ni tokens.
        val isKnownLegacyEndpoint =
            configuredBaseUrl.equals("http://puntano.gob.ar:8080/portalServer/", ignoreCase = true)
        if (!configuredBaseUrl.startsWith("https://", ignoreCase = true) && !isKnownLegacyEndpoint) {
            throw IOException("Configuración rechazada: use HTTPS o la URL heredada exacta de Portal Puntano 4.0.")
        }
        val original = chain.request()
        val isLoginRequest = original.url.encodedPath.trimEnd('/').endsWith("/login", ignoreCase = true)
        val request = original.newBuilder().apply {
            if (!isLoginRequest) session.token?.takeIf { it.isNotBlank() }?.let {
                header("X-Authorization", "Bearer $it")
            }
        }.build()
        val response = chain.proceed(request)
        if (response.code == 401) {
            // Una respuesta de no autorizado invalida la sesión local para evitar
            // seguir enviando un token caducado en las siguientes peticiones.
            session.clear()
        }
        response
    }
    private val client = OkHttpClient.Builder().addInterceptor(auth)
        .connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    private fun retrofit(baseUrl: String): Retrofit {
        val configuredBaseUrl = baseUrl.trim()
        // Retrofit necesita una base válida al construir el cliente. Si falta configuración,
        // usamos un dominio reservado; el interceptor rechaza cada petición antes de abrir la red.
        val effectiveBaseUrl = configuredBaseUrl.ifBlank { "https://portal-puntano-unconfigured.invalid/" }
        return Retrofit.Builder().baseUrl(effectiveBaseUrl).client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build()
    }
    val portal: PortalApi by lazy { retrofit(BuildConfig.PORTAL_BASE_URL).create(PortalApi::class.java) }
}