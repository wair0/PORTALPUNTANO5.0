package com.gpsl.st.portalpuntano5.data.remote

import com.gpsl.st.portalpuntano5.data.remote.dto.*
import kotlinx.serialization.json.JsonElement
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

    // El código original de Portal Puntano 4.0 usa el ID interno del usuario,
    // y devuelve un objeto con una lista "liquidations" agrupada por año.
    @GET("secure/liquidations/{userId}")
    suspend fun liquidations(@Path("userId") userId: String): LiquidationsResponseDto

    @GET("secure/judicialExpedient/docType/DNI/docNumber/{docNumber}") suspend fun expedients(@Path("docNumber") docNumber: String): List<ExpedientDto>
    @GET("secure/actions/expedient/{id}") suspend fun expedientActions(@Path("id") id: String): JsonElement
    @GET("secure/civilRegistration/certificates") suspend fun certificates(): List<CertificateDto>
    @GET("secure/appointments/rm") suspend fun appointments(): List<AppointmentDto>
    @GET("secure/driverLicense/license") suspend fun driverLicense(): CredentialDto
    @GET("secure/ministryOfHealth/credential") suspend fun healthCredential(): CredentialDto

    // La respuesta legacy puede ser un objeto contenedor y no un array directo.
    @GET("secure/education/student/tutor") suspend fun students(): JsonElement
    @GET("secure/education/schoolCycle/person/{personId}") suspend fun schoolCycles(@Path("personId") personId: String): JsonElement
    @GET("secure/education/analyticalCertificate/docNumber/{docNumber}") suspend fun analyticalCertificate(@Path("docNumber") docNumber: String): JsonElement
    @GET("secure/education/studentId/{studentId}/schoolCycle/{schoolCycleId}") suspend fun studentCycle(@Path("studentId") studentId: String, @Path("schoolCycleId") schoolCycleId: String): JsonElement
    @GET("secure/education/subjects/studentId/{studentId}/schoolCycle/{schoolCycleId}") suspend fun subjects(@Path("studentId") studentId: String, @Path("schoolCycleId") schoolCycleId: String): JsonElement
    @GET("secure/education/studentId/{studentId}/schoolCycle/{schoolCycleId}/reportCard") suspend fun reportCard(@Path("studentId") studentId: String, @Path("schoolCycleId") schoolCycleId: String): JsonElement
    @GET("secure/education/studentId/{studentId}/concurrence/dateFrom/{dateFrom}/dateTo/{dateTo}") suspend fun concurrence(@Path("studentId") studentId: String, @Path("dateFrom") dateFrom: String, @Path("dateTo") dateTo: String, @Query("present") present: Boolean = false): JsonElement
}
