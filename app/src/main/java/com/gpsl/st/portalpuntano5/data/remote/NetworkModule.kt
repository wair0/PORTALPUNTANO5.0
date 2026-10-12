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
        val req = chain.request().newBuilder().apply { session.token?.let { header("Authorization", "Bearer $it") } }.build()
        chain.proceed(req)
    }
    private val client = OkHttpClient.Builder().addInterceptor(auth)
        .connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    private fun retrofit(baseUrl: String) = Retrofit.Builder().baseUrl(baseUrl).client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build()
    val portal: PortalApi by lazy { retrofit(BuildConfig.PORTAL_BASE_URL).create(PortalApi::class.java) }
}