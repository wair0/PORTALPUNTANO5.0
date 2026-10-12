package com.gpsl.st.portalpuntano5.data.demo

import com.gpsl.st.portalpuntano5.domain.model.*

object DemoData {
    val profile = Profile("Valentina Ríos", "00.000.000", "VR")
    private val steps = listOf("Iniciado", "En revisión", "Aprobación", "Listo")
    val appointments = listOf(
        Appointment("a1", "Hospital Regional Dr. Ramón Carrillo", "Cardiología", "Dr. M. Aguirre", "2026-10-14", "09:30", "CONFIRMADO"),
        Appointment("a2", "Centro de Salud N.º 3", "Clínica médica", "Dra. L. Funes", "2026-10-22", "16:00", "PENDIENTE"),
        Appointment("a3", "Policlínico Ciudad de la Punta", "Oftalmología", "Dr. S. Lucero", "2026-09-30", "11:15", "REALIZADO"))
    val procedures = listOf(
        Procedure("p1", "Certificado de libre deuda de alimentos", "VIGENTE", 66, "2026-10-08", steps, 2),
        Procedure("p2", "Reclamo — Defensoría del Pueblo", "VIGENTE", 33, "2026-10-05", steps, 1),
        Procedure("p3", "Acta de nacimiento digital", "REALIZADO", 100, "2026-09-12", steps, 4))
    val health = HealthOverview(listOf(
        Consultation("c1", "Dra. L. Funes", "Clínica médica", "Centro de Salud N.º 3", "2026-09-18", "Control anual"),
        Consultation("c2", "Dr. S. Lucero", "Oftalmología", "Policlínico Ciudad de la Punta", "2026-08-02", "Fatiga visual")), HealthCounts(12, 5, 3, 4, 2))
    val payslips = listOf(Payslip("l1", "Septiembre 2026", "$ 1.284.500,00"), Payslip("l2", "Agosto 2026", "$ 1.261.200,00"))
    val credentials = listOf(
        Credential("LICENCIA", "Licencia de conducir", "VALENTINA RÍOS", "00 000 000", "04/2029", "Categorías B1 · A2.1"),
        Credential("MATRICULA", "Matrícula profesional", "VALENTINA RÍOS", "MP 0000", "12/2027", "Registro provincial"),
        Credential("SALUD", "Credencial personal de salud", "VALENTINA RÍOS", "CPS 0000 0000", "Vigente", "Cobertura provincial"))
    val students = listOf(Student("s1", "Mateo Ríos", "00.000.001", "Escuela N.º 12 «Juan Pascual Pringles»"))
    val expedients = listOf(Expedient("j1", "EXP-1520/2026", "Alimentos — actualización de cuota", "Juzgado de Familia N.º 2", "2026-10-06"))
    val certificates = listOf(CivilCertificate("r1", "Acta de nacimiento", "2026-09-12"))
}