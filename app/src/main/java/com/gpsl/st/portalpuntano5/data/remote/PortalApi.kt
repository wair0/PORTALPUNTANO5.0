package com.gpsl.st.portalpuntano5.data.remote

import com.gpsl.st.portalpuntano5.data.remote.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PortalApi {
    @POST("login") suspend fun login(@Body body: LoginRequestDto): LoginResponseDto
    @GET("secure/procedure/user/{docNumber}") suspend fun procedures(@Path("docNumber") docNumber: String): List<ProcedureDto>
    @GET("secure/liquidations/") suspend fun liquidations(): List<LiquidationDto>
    @GET("secure/judicialExpedient/docType/DNI/docNumber/{docNumber}") suspend fun expedients(@Path("docNumber") docNumber: String): List<ExpedientDto>
    @GET("secure/civilRegistration/certificates") suspend fun certificates(): List<CertificateDto>
    @GET("secure/appointments/rm") suspend fun appointments(): List<AppointmentDto>
    @GET("secure/driverLicense/license") suspend fun driverLicense(): CredentialDto
    @GET("secure/ministryOfHealth/credential") suspend fun healthCredential(): CredentialDto
    @GET("secure/dependents") suspend fun students(): List<StudentDto>
}