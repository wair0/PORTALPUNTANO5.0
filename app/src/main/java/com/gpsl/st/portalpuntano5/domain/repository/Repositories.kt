package com.gpsl.st.portalpuntano5.domain.repository

import com.gpsl.st.portalpuntano5.domain.model.*

interface AuthRepository {
    fun currentSession(): SessionState
    suspend fun login(request: LoginRequest): SessionState
    fun logout()
}
interface PortalRepository {
    suspend fun homeSummary(): HomeSummary
    suspend fun healthOverview(): HealthOverview
    suspend fun appointments(): List<Appointment>
    suspend fun procedures(): List<Procedure>
    suspend fun payslips(): List<Payslip>
    suspend fun credentials(): List<Credential>
    suspend fun students(): List<Student>
    suspend fun expedients(): List<Expedient>
    suspend fun civilCertificates(): List<CivilCertificate>
}