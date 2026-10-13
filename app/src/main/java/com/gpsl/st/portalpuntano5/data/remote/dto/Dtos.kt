package com.gpsl.st.portalpuntano5.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable data class LoginRequestDto(val docType: String, val docNumber: String, val sex: String, val password: String)
@Serializable data class LoginResponseDto(val authToken: AuthTokenDto? = null, val user: LoginUserDto? = null, val mustChangeCode: Boolean? = null)
@Serializable data class AuthTokenDto(val token: String? = null)
@Serializable data class LoginUserDto(val userId: String? = null, val fullName: String? = null, val name: String? = null)
@Serializable data class ProcedurePageDto(val content: List<ProcedureDto> = emptyList(), val page: ProcedurePageInfoDto? = null)
@Serializable data class ProcedurePageInfoDto(val totalElements: Long? = null)
@Serializable data class ProcedureDto(val id: JsonPrimitive? = null, val name: String? = null, val coverLetter: String? = null, val status: String? = null, val procedureStatus: String? = null, val date: String? = null, val dateCreated: String? = null, val progress: Int? = null)

// El endpoint real devuelve { "liquidations": [ { "year": "...", "liquidation": [...] } ] }.
@Serializable data class LiquidationsResponseDto(val liquidations: List<LiquidationYearDto> = emptyList())
@Serializable data class LiquidationYearDto(val year: JsonPrimitive? = null, val liquidation: List<LiquidationItemDto> = emptyList())
@Serializable data class LiquidationItemDto(val id: JsonPrimitive? = null, val name: String? = null, val period: String? = null, val net: JsonPrimitive? = null)

@Serializable data class ExpedientDto(val id: String? = null, val number: String? = null, val subject: String? = null, val court: String? = null, val lastAction: String? = null)
@Serializable data class CertificateDto(val id: String? = null, val type: String? = null, val date: String? = null)
@Serializable data class AppointmentDto(val id: String? = null, val healthCenter: String? = null, val specialty: String? = null, val professional: String? = null, val date: String? = null, val time: String? = null, val status: String? = null)
@Serializable data class CredentialDto(val holder: String? = null, val number: String? = null, val expiration: String? = null, val categories: String? = null)

// El código original utiliza studentId; se mantienen alias para tolerar variantes del servicio.
@Serializable data class StudentDto(val studentId: JsonPrimitive? = null, val id: JsonPrimitive? = null, val name: String? = null, val docNumber: String? = null, val school: String? = null, val schoolName: String? = null)
