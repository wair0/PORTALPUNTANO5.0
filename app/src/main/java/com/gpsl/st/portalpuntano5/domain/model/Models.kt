package com.gpsl.st.portalpuntano5.domain.model

import kotlinx.serialization.Serializable

@Serializable data class Profile(val fullName: String, val docNumber: String, val initials: String)
@Serializable data class SessionState(val authenticated: Boolean, val profile: Profile? = null)
@Serializable data class Appointment(val id: String, val center: String, val specialty: String, val professional: String, val date: String, val time: String, val status: String)
@Serializable data class HomeSummary(val profile: Profile, val nextAppointment: Appointment?, val activeProcedures: Int, val notifications: Int)
@Serializable data class Consultation(val id: String, val professional: String, val specialty: String, val center: String, val date: String, val diagnosis: String)
@Serializable data class HealthCounts(val consultas: Int, val diagnosticos: Int, val estudios: Int, val recetas: Int, val certificados: Int)
@Serializable data class HealthOverview(val consultations: List<Consultation>, val counts: HealthCounts)
@Serializable data class Procedure(val id: String, val title: String, val status: String, val progress: Int, val updatedAt: String, val steps: List<String>, val currentStep: Int)
@Serializable data class Payslip(val id: String, val period: String, val net: String)
@Serializable data class Credential(val kind: String, val title: String, val holder: String, val number: String, val validUntil: String, val detail: String)
@Serializable data class Student(val id: String, val name: String, val docNumber: String, val school: String)
@Serializable data class Expedient(val id: String, val number: String, val subject: String, val court: String, val lastAction: String)
@Serializable data class CivilCertificate(val id: String, val type: String, val date: String)
@Serializable data class LoginRequest(val docNumber: String, val sex: String, val password: String)