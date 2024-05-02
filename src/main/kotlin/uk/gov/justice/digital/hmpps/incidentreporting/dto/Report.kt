package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Incident report")
@JsonInclude(JsonInclude.Include.ALWAYS)
data class Report(
  @Schema(description = "The internal ID of this report", required = true)
  val id: UUID,
  @Schema(description = "The human-readable identifier of this report", required = true)
  val incidentNumber: String,
  @Schema(description = "Incident report type", required = true)
  val type: Type,
  @Schema(description = "When the incident took place", required = true, example = "2024-04-29T12:34:56.789012")
  val incidentDateAndTime: LocalDateTime,
  @Schema(description = "The NOMIS id of the prison where incident took place", required = true, example = "MDI")
  val prisonId: String,
  @Schema(description = "Brief title describing the incident", required = true)
  val title: String,
  @Schema(description = "Longer summary of the incident", required = true)
  val description: String,
  @Schema(description = "Event linking multiple incident reports together", required = true)
  val event: Event,

  @Schema(description = "Username of person who created the incident report", required = true)
  val reportedBy: String,
  @Schema(description = "When the incident report was created", required = true, example = "2024-04-29T12:34:56.789012")
  val reportedDate: LocalDateTime,
  @Schema(description = "The current status of this report", required = false, defaultValue = "DRAFT")
  val status: Status = Status.DRAFT,
  @Schema(description = "Optional user who this report is currently assigned to", required = false, defaultValue = "null")
  val assignedTo: String? = null,

  @Schema(description = "The question-response pairs that make up this report", required = true)
  val questions: List<Question> = emptyList(),
  @Schema(description = "Prior versions of this report, created when the report type changes", required = true)
  val history: List<History> = emptyList(),
  @Schema(description = "Previous statuses the incident report transitioned to", required = true)
  val historyOfStatuses: List<StatusHistory> = emptyList(),

  @Schema(description = "Which members of staff were involved?", required = true)
  val staffInvolved: List<StaffInvolvement> = emptyList(),
  @Schema(description = "Which prisoners were involved?", required = true)
  val prisonersInvolved: List<PrisonerInvolvement> = emptyList(),
  @Schema(description = "Where the incident happened", required = true)
  val locations: List<Location> = emptyList(),
  @Schema(description = "What evidence has been recorded", required = true)
  val evidence: List<Evidence> = emptyList(),
  @Schema(description = "The corrections that were requested of this report", required = true)
  val correctionRequests: List<CorrectionRequest> = emptyList(),

  @Schema(description = "When the report was first created", required = true)
  val createdDate: LocalDateTime,
  @Schema(description = "When the report was last changed", required = true)
  val lastModifiedDate: LocalDateTime,
  @Schema(description = "Username of the person who last changed this report", required = true)
  val lastModifiedBy: String,

  @Schema(description = "Whether the report was initially created in NOMIS as opposed to DPS", required = false, defaultValue = "false")
  @JsonProperty(required = false, access = JsonProperty.Access.READ_ONLY)
  val createdInNomis: Boolean = false,
)
