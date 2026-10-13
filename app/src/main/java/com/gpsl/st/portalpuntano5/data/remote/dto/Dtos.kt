package com.gpsl.st.portalpuntano5.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable data class LoginRequestDto(val docType: String, val docNumber: String, val sex: String, val password: String)
@Serializable data class LoginResponseDto(val authToken: AuthTokenDto? = null, val user: LoginUserDto? = null, val mustChangeCode: Boolean? = null)
@Serializable data class AuthTokenDto(val token: String? = null)
@Serializable data class LoginUserDto(val userId: String? = null, val fullName: String? = null, val name: String? = null)
@Serializable data class ProcedurePageDto(val content: List<ProcedureDto> = emptyList(), val page: ProcedurePageInfoDto? = null)
@Serializable data class ProcedurePageInfoDto(val totalElements: Long? = null)
@Serializable data class ProcedureDto(val id: kotlinx.serialization.json.JsonPrimitive? = null, val name: String? = null, val coverLetter: String? = null, val status: String? = null, val procedureStatus: String? = null, val date: String? = null, val dateCreated: String? = null, val progress: Int? = null)
@Serializable data class LiquidationDto(val id: String? = null, val period: String? = null, val net: String? = null)
@Serializable data class ExpedientDto(val id: String? = null, val number: String? = null, val subject: String? = null, val court: String? = null, val lastAction: String? = null)
@Serializable data class CertificateDto(val id: String? = null, val type: String? = null, val date: String? = null)
@Serializable data class AppointmentDto(val id: String? = null, val healthCenter: String? = null, val specialty: String? = null, val professional: String? = null, val date: String? = null, val time: String? = null, val status: String? = null)
@Serializable data class CredentialDto(val holder: String? = null, val number: String? = null, val expiration: String? = null, val categories: String? = null)
@Serializable data class StudentDto(val id: String? = null, val name: String? = null, val docNumber: String? = null, val school: String? = null)
