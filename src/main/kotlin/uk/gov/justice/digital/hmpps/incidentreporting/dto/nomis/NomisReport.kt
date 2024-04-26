package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "NOMIS Incident Report Details")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisReport(
  @Schema(description = "The Incident id")
  val incidentId: Long,
  @Schema(description = "The id of the questionnaire associated with this incident")
  val questionnaireId: Long,
  @Schema(description = "A summary of the incident")
  val title: String?,
  @Schema(description = "The incident details")
  val description: String?,
  @Schema(description = "Prison where the incident occurred")
  val prison: NomisCode,

  @Schema(description = "Status details")
  val status: NomisStatus,
  @Schema(description = "The incident questionnaire type")
  val type: String,

  @Schema(description = "If the response is locked ie if the response is completed")
  val lockedResponse: Boolean,

  @Schema(description = "The date and time of the incident")
  val incidentDateTime: LocalDateTime,

  @Schema(description = "The staff member who reported the incident")
  val reportingStaff: NomisStaff,
  @Schema(description = "The date and time the incident was reported")
  val reportedDateTime: LocalDateTime,

  @Schema(description = "Staff involved in the incident")
  val staffParties: List<NomisStaffParty>,

  @Schema(description = "Offenders involved in the incident")
  val offenderParties: List<NomisOffenderParty>,

  @Schema(description = "Requirements for completing the incident report")
  val requirements: List<NomisRequirement>,

  @Schema(description = "Questions asked for the incident")
  val questions: List<NomisQuestion>,

  @Schema(description = "Historical questionnaire details for the incident")
  val history: List<NomisHistory>,
)
