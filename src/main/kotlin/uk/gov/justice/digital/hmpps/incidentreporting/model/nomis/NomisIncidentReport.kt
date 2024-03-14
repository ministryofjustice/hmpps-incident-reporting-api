package uk.gov.justice.digital.hmpps.incidentreporting.model.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentReport
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentType
import uk.gov.justice.digital.hmpps.incidentreporting.service.InformationSource
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "NOMIS Incident Report Details")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisIncidentReport(
  @Schema(description = "The incidentReport id")
  val incidentId: Long,
  @Schema(description = "The id of the questionnaire associated with this incidentReport")
  val questionnaireId: Long,
  @Schema(description = "A summary of the incidentReport")
  val title: String?,
  @Schema(description = "The incidentReport details")
  val description: String?,
  @Schema(description = "Prison where the incidentReport occurred")
  val prison: CodeDescription,

  @Schema(description = "Status details")
  val status: NomisIncidentStatus,
  @Schema(description = "The incidentReport questionnaire type")
  val type: String,

  @Schema(description = "If the response is locked ie if the response is completed")
  val lockedResponse: Boolean,

  @Schema(description = "The date and time of the incidentReport")
  val incidentDateTime: LocalDateTime,

  @Schema(description = "The staff member who reported the incidentReport")
  val reportingStaff: Staff,
  @Schema(description = "The date and time the incidentReport was reported")
  val reportedDateTime: LocalDateTime,

  @Schema(description = "Staff involved in the incidentReport")
  val staffParties: List<StaffParty>,

  @Schema(description = "Offenders involved in the incidentReport")
  val offenderParties: List<OffenderParty>,

  @Schema(description = "Requirements for completing the incidentReport report")
  val requirements: List<Requirement>,

  @Schema(description = "Questions asked for the incidentReport")
  val questions: List<Question>,

  @Schema(description = "Historical questionnaire details for the incidentReport")
  val history: List<History>,
) {
  fun toNewEntity(clock: Clock): IncidentReport {
    return IncidentReport(
      incidentNumber = incidentId.toString(),
      incidentType = convertIncidentType(),
      incidentDateAndTime = incidentDateTime,
      prisonId = prison.code,
      incidentDetails = title ?: "NO DETAILS GIVEN\n$description",
      reportedBy = reportingStaff.username,
      reportedDate = reportedDateTime,
      status = uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentStatus.DRAFT,
      createdDate = LocalDateTime.now(clock),
      lastModifiedDate = LocalDateTime.now(clock),
      lastModifiedBy = reportingStaff.username,
      source = InformationSource.NOMIS,
    )
  }

  private fun convertIncidentType() = when (type) {
    "SELF_HARM" -> IncidentType.SELF_HARM
    "MISC" -> IncidentType.MISCELLANEOUS
    "ASSAULTS3" -> IncidentType.ASSAULT
    "DAMAGE" -> IncidentType.DAMAGE
    "FIND0422" -> IncidentType.FINDS
    "KEY_LOCKNEW" -> IncidentType.KEY_LOCK_INCIDENT
    "DISORDER1" -> IncidentType.DISORDER
    "FIRE" -> IncidentType.FIRE
    "TOOL_LOSS" -> IncidentType.TOOL_LOSS
    "FOOD_REF" -> IncidentType.FOOD_REFUSAL
    "DEATH" -> IncidentType.DEATH_IN_CUSTODY
    "TRF3" -> IncidentType.TEMPORARY_RELEASE_FAILURE
    "RADIO_COMP" -> IncidentType.RADIO_COMPROMISE
    "DRONE1" -> IncidentType.DRONE_SIGHTING
    "ABSCOND" -> IncidentType.ABSCONDER
    "REL_ERROR" -> IncidentType.RELEASED_IN_ERROR
    "BOMB" -> IncidentType.BOMB_THREAT
    "CLOSE_DOWN" -> IncidentType.FULL_CLOSE_DOWN_SEARCH
    "BREACH" -> IncidentType.BREACH_OF_SECURITY
    "DEATH_NI" -> IncidentType.DEATH_OTHER
    "ESCAPE_EST" -> IncidentType.ESCAPE_FROM_CUSTODY
    "ATT_ESCAPE" -> IncidentType.ATTEMPTED_ESCAPE_FROM_CUSTODY
    "ESCAPE_ESC" -> IncidentType.ESCAPE_FROM_ESCORT
    "ATT_ESC_E" -> IncidentType.ATTEMPTED_ESCAPE_FROM_ESCORT

    else -> throw RuntimeException("Unknown incident type: $type")
  }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Staff(
  @Schema(description = "Username of first account related to staff")
  val username: String,
  @Schema(description = "NOMIS staff id")
  val staffId: Long,
  @Schema(description = "First name of staff member")
  val firstName: String,
  @Schema(description = "Last name of staff member")
  val lastName: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class StaffParty(
  @Schema(description = "Staff involved in the incidentReport")
  val staff: Staff,
  @Schema(description = "Staff role in the incidentReport")
  val role: CodeDescription,
  @Schema(description = "General information about the incidentReport")
  val comment: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Offender(
  @Schema(description = "NOMIS id")
  val offenderNo: String,
  @Schema(description = "First name of staff member")
  val firstName: String?,
  @Schema(description = "Last name of staff member")
  val lastName: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenderParty(
  @Schema(description = "Offender involved in the incidentReport")
  val offender: Offender,
  @Schema(description = "Offender role in the incidentReport")
  val role: CodeDescription,
  @Schema(description = "The outcome of the incidentReport")
  val outcome: CodeDescription?,
  @Schema(description = "General information about the incidentReport")
  val comment: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Requirement(
  @Schema(description = "The update required to the incidentReport report")
  val comment: String?,
  @Schema(description = "Date the requirement was recorded")
  val date: LocalDate,
  @Schema(description = "The staff member who made the requirement request")
  val staff: Staff,
  @Schema(description = "The reporting location of the staff")
  val prisonId: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Question(
  @Schema(description = "The questionnaire question id")
  val questionId: Long,
  @Schema(description = "The sequence number of the question for this incidentReport")
  val sequence: Int,
  @Schema(description = "The Question being asked")
  val question: String,
  @Schema(description = "List of Responses to this question")
  val answers: List<Response> = listOf(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Response(
  @Schema(description = "The id of the questionnaire question answer")
  val questionResponseId: Long?,
  @Schema(description = "The sequence number of the response for this incidentReport")
  val sequence: Int,
  @Schema(description = "The answer text")
  val answer: String?,
  @Schema(description = "Comment added to the response by recording staff")
  val comment: String?,
  @Schema(description = "Recording staff")
  val recordingStaff: Staff,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class History(
  @Schema(description = "The history questionnaire id for the incidentReport")
  val questionnaireId: Long,
  @Schema(description = "The questionnaire type")
  val type: String,
  @Schema(description = "The questionnaire description")
  val description: String?,
  @Schema(description = "Questions asked for the questionnaire")
  val questions: List<HistoryQuestion>,
  @Schema(description = "When the questionnaire was changed")
  val incidentChangeDate: LocalDate,
  @Schema(description = "Who changed the questionnaire")
  val incidentChangeStaff: Staff,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HistoryQuestion(
  @Schema(description = "The sequence number of the response question for this incidentReport")
  val questionId: Long,
  @Schema(description = "The sequence number of the question for this incidentReport")
  val sequence: Int,
  @Schema(description = "The Question being asked")
  val question: String,
  @Schema(description = "Historical list of Responses to this question")
  val answers: List<HistoryResponse> = listOf(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HistoryResponse(
  @Schema(description = "The id of the questionnaire question answer")
  val questionResponseId: Long?,
  @Schema(description = "The sequence number of the response for this incidentReport")
  val responseSequence: Int,
  @Schema(description = "The answer text")
  val answer: String?,
  @Schema(description = "Comment added to the response by recording staff")
  val comment: String?,
  @Schema(description = "Recording staff")
  val recordingStaff: Staff,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CodeDescription(
  @Schema(description = "Code")
  val code: String,
  @Schema(description = "Description")
  val description: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisIncidentStatus(
  @Schema(description = "Status Code")
  val code: String,
  @Schema(description = "Status Description")
  val description: String,
)
