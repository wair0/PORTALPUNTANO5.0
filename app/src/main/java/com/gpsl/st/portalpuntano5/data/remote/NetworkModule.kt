package com.gpsl.st.portalpuntano5.data.remote

import com.gpsl.st.portalpuntano5.BuildConfig
import com.gpsl.st.portalpuntano5.data.local.SessionStore
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class NetworkModule(private val session: SessionStore) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true; explicitNulls = false }
    private val auth = Interceptor { chain ->
        val original = chain.request()
        val isLoginRequest = original.url.encodedPath.trimEnd('/').endsWith("/login", ignoreCase = true)
        val request = original.newBuilder().apply {
            if (!isLoginRequest) session.token?.takeIf { it.isNotBlank() }?.let {
                header("Authorization", "Bearer $it")
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
        if (!BuildConfig.DEMO_MODE) {
            require(configuredBaseUrl.isNotBlank()) {
                "Servicio no configurado: define PORTAL_BASE_URL con la URL oficial antes de habilitar el modo conectado."
            }
            require(configuredBaseUrl.startsWith("https://", ignoreCase = true)) {
                "Configuración insegura: el servicio debe usar HTTPS antes de habilitar el modo conectado."
            }
        }
        // Retrofit exige una URL base válida incluso en modo demo. Esta URL reservada
        // no identifica un servicio real y evita que la app se cierre al arrancar
        // cuando PORTAL_BASE_URL no está configurada. Las operaciones demo no llaman a la API.
        val effectiveBaseUrl = configuredBaseUrl.ifBlank { "https://portal-puntano-demo.invalid/" }
        return Retrofit.Builder().baseUrl(effectiveBaseUrl).client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build()
    }
    val portal: PortalApi by lazy { retrofit(BuildConfig.PORTAL_BASE_URL).create(PortalApi::class.java) }
}