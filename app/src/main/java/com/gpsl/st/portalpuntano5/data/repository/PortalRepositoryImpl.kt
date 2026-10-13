package com.gpsl.st.portalpuntano5.data.repository

import com.gpsl.st.portalpuntano5.data.demo.DemoData
import com.gpsl.st.portalpuntano5.data.local.SessionStore
import com.gpsl.st.portalpuntano5.data.remote.PortalApi
import com.gpsl.st.portalpuntano5.domain.model.*
import com.gpsl.st.portalpuntano5.domain.repository.PortalRepository
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.contentOrNull

class PortalRepositoryImpl(
    private val api: PortalApi,
    private val session: SessionStore,
    private val demo: Boolean
) : PortalRepository {
    private val doc get() = session.docNumber ?: error("Sesión expirada (401)")
    private val userId get() = session.userId ?: error("Volvé a iniciar sesión para consultar los recibos de sueldo.")

    override suspend fun homeSummary(): HomeSummary {
        val profile = if (demo) DemoData.profile else {
            val name = session.fullName ?: "Ciudadano"
            Profile(
                name,
                doc,
                name.split(" ").filter { it.isNotBlank() }
                    .take(2).joinToString("") { it.first().uppercase() }
            )
        }
        val appts = appointments()
        val procs = procedures()
        return HomeSummary(
            profile,
            appts.firstOrNull { it.status != "REALIZADO" },
            procs.count { it.status == "VIGENTE" },
            0
        )
    }

    override suspend fun healthOverview(): HealthOverview {
        if (demo) return DemoData.health
        error("El servicio de salud todavía no está integrado y validado. No se muestran datos de demostración.")
    }

    override suspend fun appointments(): List<Appointment> =
        if (demo) DemoData.appointments else api.appointments().map {
            Appointment(
                it.id.orEmpty(),
                it.healthCenter.orEmpty(),
                it.specialty.orEmpty(),
                it.professional.orEmpty(),
                it.date.orEmpty(),
                it.time.orEmpty(),
                (it.status ?: "PENDIENTE").uppercase()
            )
        }

    private val steps = listOf("Iniciado", "En revisión", "Aprobación", "Listo")

    override suspend fun procedures(): List<Procedure> {
        if (demo) return DemoData.procedures

        // Portal Puntano 4.0 exige status=ACT o status=FIN y devuelve { content, page }.
        val active = api.procedures(doc, status = "ACT").content.map { it.toProcedure("ACT") }
        val finished = api.procedures(doc, status = "FIN").content.map { it.toProcedure("FIN") }
        return active + finished
    }

    private fun com.gpsl.st.portalpuntano5.data.remote.dto.ProcedureDto.toProcedure(requestedStatus: String): Procedure {
        val rawStatus = (procedureStatus ?: status ?: requestedStatus).uppercase()
        val done = requestedStatus == "FIN" || rawStatus.contains("REALIZ") || rawStatus == "FIN"
        val progress = progress ?: if (done) 100 else 33
        return Procedure(
            id?.toString()?.removeSurrounding("\"") ?: "",
            name ?: coverLetter.orEmpty(),
            if (done) "REALIZADO" else "VIGENTE",
            progress,
            date ?: dateCreated.orEmpty(),
            steps,
            (progress * steps.size / 100).coerceIn(1, steps.size)
        )
    }

    override suspend fun payslips(): List<Payslip> {
        if (demo) return DemoData.payslips
        return api.liquidations(userId).liquidations.flatMap { group ->
            val year = group.year?.contentOrNull.orEmpty()
            group.liquidation.map { item ->
                Payslip(item.id?.contentOrNull ?: "", listOf(year, item.period ?: item.name.orEmpty()).filter { it.isNotBlank() }.joinToString(" · "), item.net?.contentOrNull.orEmpty())
            }
        }
    }

    override suspend fun credentials(): List<Credential> {
        if (demo) return DemoData.credentials

        val name = (session.fullName ?: "").uppercase()
        val licenseResult = try {
            Result.success(api.driverLicense())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
        val healthResult = try {
            Result.success(api.healthCredential())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
        val credentials = mutableListOf<Credential>()

        licenseResult.getOrNull()?.let {
            credentials += Credential(
                "LICENCIA",
                "Licencia de conducir",
                name,
                it.number.orEmpty(),
                it.expiration.orEmpty(),
                it.categories.orEmpty()
            )
        }
        healthResult.getOrNull()?.let {
            credentials += Credential(
                "SALUD",
                "Credencial personal de salud",
                name,
                it.number.orEmpty(),
                it.expiration ?: "Vigente",
                ""
            )
        }

        if (credentials.isEmpty()) {
            val cause = licenseResult.exceptionOrNull() ?: healthResult.exceptionOrNull()
            throw IllegalStateException(
                "No se pudieron consultar las credenciales. El servicio no respondió correctamente.",
                cause
            )
        }
        return credentials
    }

    override suspend fun students(): List<Student> =
        if (demo) DemoData.students else api.students().map {
            Student(it.studentId?.contentOrNull ?: it.id?.contentOrNull ?: "", it.name.orEmpty(), it.docNumber.orEmpty(), it.school ?: it.schoolName.orEmpty())
        }

    override suspend fun expedients(): List<Expedient> =
        if (demo) DemoData.expedients else api.expedients(doc).map {
            Expedient(
                it.id.orEmpty(),
                it.number.orEmpty(),
                it.subject.orEmpty(),
                it.court.orEmpty(),
                it.lastAction.orEmpty()
            )
        }

    override suspend fun civilCertificates(): List<CivilCertificate> =
        if (demo) DemoData.certificates else api.certificates().map {
            CivilCertificate(it.id.orEmpty(), it.type.orEmpty(), it.date.orEmpty())
        }
}
