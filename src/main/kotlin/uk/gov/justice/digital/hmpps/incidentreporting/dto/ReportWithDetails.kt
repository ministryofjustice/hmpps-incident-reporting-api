package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Incident report with all related information", accessMode = Schema.AccessMode.READ_ONLY)
@JsonInclude(JsonInclude.Include.ALWAYS)
class ReportWithDetails(
  id: UUID,
  reportReference: String,
  type: Type,
  incidentDateAndTime: LocalDateTime,
  location: String,
  title: String,
  description: String,
  @Suppress("unused")
  @param:Schema(description = "Addendums to the description")
  val descriptionAddendums: List<DescriptionAddendum>,
  reportedBy: String,
  reportedAt: LocalDateTime,
  status: Status,
  createdAt: LocalDateTime,
  modifiedAt: LocalDateTime,
  modifiedBy: String,
  createdInNomis: Boolean,
  lastModifiedInNomis: Boolean,
  duplicatedReportId: UUID?,

  @param:Schema(description = "The question-response pairs that make up this report")
  val questions: List<Question>,
  @param:Schema(description = "Prior versions of this report, created when the report type changes")
  val history: List<History>,
  @param:Schema(description = "Previous statuses the incident report transitioned to")
  val historyOfStatuses: List<StatusHistory>,
  @param:Schema(description = "Previous incident types")
  val incidentTypeHistory: List<IncidentTypeHistory>,
  @param:Schema(description = "Which members of staff were involved?")
  val staffInvolved: List<StaffInvolvement>,
  @param:Schema(description = "Which prisoners were involved?")
  val prisonersInvolved: List<PrisonerInvolvement>,
  @param:Schema(description = "The corrections that were requested of this report")
  val correctionRequests: List<CorrectionRequest>,

  @param:Schema(
    description = "Internal flag which, " +
      "if false, is used to indicate that addition of staff involvements is unfinished",
  )
  val staffInvolvementDone: Boolean,
  @param:Schema(
    description = "Internal flag which, " +
      "if false, is used to indicate that addition of prisoner involvements is unfinished",
  )
  val prisonerInvolvementDone: Boolean,
) : ReportBasic(
  id = id,
  reportReference = reportReference,
  type = type,
  incidentDateAndTime = incidentDateAndTime,
  location = location,
  title = title,
  description = description,
  reportedBy = reportedBy,
  reportedAt = reportedAt,
  status = status,
  createdAt = createdAt,
  modifiedAt = modifiedAt,
  modifiedBy = modifiedBy,
  createdInNomis = createdInNomis,
  lastModifiedInNomis = lastModifiedInNomis,
  duplicatedReportId = duplicatedReportId,
) {
  // NB: this property can be removed once fully migrated off NOMIS and reconciliation checks are turned off
  @Suppress("unused")
  @get:Schema(
    description = "NOMIS incident report type code, which may be null for newer incident types",
    nullable = true,
    deprecated = true,
  )
  @get:JsonProperty
  val nomisType: String?
    get() = type.nomisType

  // NB: this property can be removed once fully migrated off NOMIS and reconciliation checks are turned off
  @Suppress("unused")
  @get:Schema(
    description = "Previous NOMIS incident report status code, which may be null for statuses that cannot be mapped",
    nullable = true,
    deprecated = true,
  )
  @get:JsonProperty
  val nomisStatus: String?
    get() = status.nomisStatus
}
