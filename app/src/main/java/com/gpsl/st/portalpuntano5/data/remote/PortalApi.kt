package com.gpsl.st.portalpuntano5.data.remote

import com.gpsl.st.portalpuntano5.data.remote.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PortalApi {
    // Ruta confirmada en login.page.ts de Portal Puntano 4.0.
    @POST("token/mobile/login") suspend fun login(@Body body: LoginRequestDto): LoginResponseDto

    // Portal Puntano 4.0 requiere status y devuelve una respuesta paginada.
    @GET("secure/procedure/user/{docNumber}")
    suspend fun procedures(
        @Path("docNumber") docNumber: String,
        @Query("status") status: String,
        @Query("search") search: String = "",
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 500,
        @Query("sort") sort: String = "dateCreated,desc"
    ): ProcedurePageDto

    @GET("secure/liquidations/") suspend fun liquidations(): List<LiquidationDto>
    @GET("secure/judicialExpedient/docType/DNI/docNumber/{docNumber}") suspend fun expedients(@Path("docNumber") docNumber: String): List<ExpedientDto>
    @GET("secure/civilRegistration/certificates") suspend fun certificates(): List<CertificateDto>
    @GET("secure/appointments/rm") suspend fun appointments(): List<AppointmentDto>
    @GET("secure/driverLicense/license") suspend fun driverLicense(): CredentialDto
    @GET("secure/ministryOfHealth/credential") suspend fun healthCredential(): CredentialDto
    @GET("secure/dependents") suspend fun students(): List<StudentDto>
}
