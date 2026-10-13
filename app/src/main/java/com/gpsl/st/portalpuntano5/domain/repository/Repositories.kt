package com.gpsl.st.portalpuntano5.domain.repository

import com.gpsl.st.portalpuntano5.domain.model.*
import kotlinx.serialization.json.JsonElement

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
    suspend fun expedientActions(id: String): JsonElement
    suspend fun schoolCycles(personId: String): JsonElement
    suspend fun analyticalCertificate(docNumber: String): JsonElement
    suspend fun studentCycle(studentId: String, schoolCycleId: String): JsonElement
    suspend fun subjects(studentId: String, schoolCycleId: String): JsonElement
    suspend fun reportCard(studentId: String, schoolCycleId: String): JsonElement
    suspend fun concurrence(studentId: String, dateFrom: String, dateTo: String): JsonElement
    suspend fun civilCertificates(): List<CivilCertificate>
}