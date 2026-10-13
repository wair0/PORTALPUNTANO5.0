package com.gpsl.st.portalpuntano5.data.repository

import com.gpsl.st.portalpuntano5.data.demo.DemoData
import com.gpsl.st.portalpuntano5.data.local.SessionStore
import com.gpsl.st.portalpuntano5.data.remote.PortalApi
import com.gpsl.st.portalpuntano5.domain.model.*
import com.gpsl.st.portalpuntano5.domain.repository.PortalRepository
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import kotlinx.serialization.json.*

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
        val active = api.procedures(userId, status = "ACT").content.map { it.toProcedure("ACT") }
        val finished = api.procedures(userId, status = "FIN").content.map { it.toProcedure("FIN") }
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
        // Una cuenta sin recibos puede responder 404; para esta sección significa lista vacía,
        // no un error que bloquee la navegación.
        val response = try {
            api.liquidations(userId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            if (e.code() == 404 || e.code() == 204) return emptyList()
            throw e
        }
        return response.liquidations.flatMap { group ->
            val year = group.year?.contentOrNull.orEmpty()
            group.liquidation.map { item ->
                Payslip(item.id?.contentOrNull ?: "", listOf(year, item.period ?: item.name.orEmpty()).filter { it.isNotBlank() }.joinToString(" · "), item.net?.contentOrNull.orEmpty())
            }
        }
    }

    override suspend fun credentials(): List<Credential> {
        if (demo) return DemoData.credentials

        val holder = session.fullName.orEmpty().uppercase()
        // Igual que Portal Puntano 4.0: las dos tarjetas existen aunque la persona
        // no tenga una licencia o una matrícula registradas.
        val license = credentialOrAbsent(
            kind = "LICENCIA",
            title = "Licencia de conducir",
            holder = holder,
            absentMessage = "USTED NO POSEE LICENCIA DE CONDUCIR"
        ) { api.driverLicense() }

        val registration = credentialOrAbsent(
            kind = "MATRICULA",
            title = "Matrícula",
            holder = holder,
            absentMessage = "USTED NO POSEE MATRÍCULA"
        ) { api.healthCredential() }

        return listOf(license, registration)
    }

    private suspend fun credentialOrAbsent(
        kind: String,
        title: String,
        holder: String,
        absentMessage: String,
        request: suspend () -> com.gpsl.st.portalpuntano5.data.remote.dto.CredentialDto
    ): Credential {
        val dto = try {
            request()
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            // En el servicio legado, un 404 al consultar una credencial significa
            // que no hay registro para esa persona; otros errores sí deben mostrarse.
            if (e.code() == 404 || e.code() == 204) {
                return Credential(kind, title, holder, "", "", absentMessage)
            }
            throw e
        }

        val message = dto.message?.trim().orEmpty()
        if (message.contains("NO POSEE", ignoreCase = true)) {
            return Credential(kind, title, dto.holder ?: holder, "", "", message.uppercase())
        }

        val number = dto.number ?: dto.registrationNumber ?: dto.matricula ?: dto.licenseNumber ?: ""
        val detail = listOf(dto.categories, dto.professionalTitle, message)
            .firstOrNull { !it.isNullOrBlank() }.orEmpty()

        // Un objeto JSON vacío o sin datos identificatorios no equivale a una
        // credencial válida: mostrar el mensaje de ausencia que usa PP 4.0.
        val hasCredentialData = number.isNotBlank() ||
            !dto.expiration.isNullOrBlank() ||
            !dto.categories.isNullOrBlank() ||
            !dto.professionalTitle.isNullOrBlank()

        return if (hasCredentialData) {
            Credential(kind, title, dto.holder ?: holder, number, dto.expiration.orEmpty(), detail)
        } else {
            Credential(kind, title, holder, "", "", message.ifBlank { absentMessage })
        }
    }

    override suspend fun students(): List<Student> {
        if (demo) return DemoData.students
        val response = api.students()
        val candidates: List<JsonElement> = when (response) {
            is JsonArray -> response.toList()
            is JsonObject -> {
                val wrapped = listOf("person", "students", "data", "content", "student").firstNotNullOfOrNull { response[it] }
                when (wrapped) {
                    is JsonArray -> wrapped.toList()
                    is JsonObject -> listOf(wrapped)
                    else -> if (response.keys.any { it in setOf("studentId", "id", "name", "docNumber", "school") }) listOf(response) else emptyList()
                }
            }
            else -> emptyList()
        }
        return candidates.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            fun value(vararg keys: String): String = keys.firstNotNullOfOrNull { key ->
                obj[key]?.let { element ->
                    when (element) {
                        is JsonPrimitive -> element.contentOrNull
                        is JsonObject -> element["name"]?.jsonPrimitive?.contentOrNull
                        else -> null
                    }
                }?.takeIf { it.isNotBlank() }
            }.orEmpty()
            val id = value("studentId", "id", "personId")
            val name = value("name", "fullName", "studentName").ifBlank {
                listOf(value("firstName", "givenName"), value("lastName", "surname")).filter { it.isNotBlank() }.joinToString(" ")
            }
            if (id.isBlank() && name.isBlank()) null
            else Student(id, name, value("docNumber", "documentNumber", "dni"), value("school", "schoolName", "institution"))
        }
    }

    override suspend fun expedientActions(id: String): JsonElement {
        if (demo) return buildJsonArray { }
        return api.expedientActions(id)
    }

    override suspend fun schoolCycles(personId: String): JsonElement = api.schoolCycles(personId)
    override suspend fun analyticalCertificate(docNumber: String): JsonElement = api.analyticalCertificate(docNumber)
    override suspend fun studentCycle(studentId: String, schoolCycleId: String): JsonElement = api.studentCycle(studentId, schoolCycleId)
    override suspend fun subjects(studentId: String, schoolCycleId: String): JsonElement = api.subjects(studentId, schoolCycleId)
    override suspend fun reportCard(studentId: String, schoolCycleId: String): JsonElement = api.reportCard(studentId, schoolCycleId)
    override suspend fun concurrence(studentId: String, dateFrom: String, dateTo: String): JsonElement =
        api.concurrence(studentId, dateFrom, dateTo, present = false)

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
