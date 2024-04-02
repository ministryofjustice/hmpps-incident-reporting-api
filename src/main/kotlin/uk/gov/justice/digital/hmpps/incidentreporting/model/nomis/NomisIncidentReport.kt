package uk.gov.justice.digital.hmpps.incidentreporting.model.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.CorrectionReason
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentEvent
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentReport
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentStatus
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentType
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.PrisonerRole
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.StaffRole
import uk.gov.justice.digital.hmpps.incidentreporting.service.InformationSource
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "NOMIS Incident Report Details")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisIncidentReport(
  @Schema(description = "The Incident id")
  val incidentId: Long,
  @Schema(description = "The id of the questionnaire associated with this incident")
  val questionnaireId: Long,
  @Schema(description = "A summary of the incident")
  val title: String?,
  @Schema(description = "The incident details")
  val description: String?,
  @Schema(description = "Prison where the incident occurred")
  val prison: CodeDescription,

  @Schema(description = "Status details")
  val status: NomisIncidentStatus,
  @Schema(description = "The incident questionnaire type")
  val type: String,

  @Schema(description = "If the response is locked ie if the response is completed")
  val lockedResponse: Boolean,

  @Schema(description = "The date and time of the incident")
  val incidentDateTime: LocalDateTime,

  @Schema(description = "The staff member who reported the incident")
  val reportingStaff: Staff,
  @Schema(description = "The date and time the incident was reported")
  val reportedDateTime: LocalDateTime,

  @Schema(description = "Staff involved in the incident")
  val staffParties: List<StaffParty>,

  @Schema(description = "Offenders involved in the incident")
  val offenderParties: List<OffenderParty>,

  @Schema(description = "Requirements for completing the incident report")
  val requirements: List<Requirement>,

  @Schema(description = "Questions asked for the incident")
  val questions: List<Question>,

  @Schema(description = "Historical questionnaire details for the incident")
  val history: List<History>,
) {
  fun toNewEntity(clock: Clock): IncidentReport {
    val ir = IncidentReport(
      incidentNumber = incidentId.toString(),
      incidentType = convertIncidentType(type),
      incidentDateAndTime = incidentDateTime,
      prisonId = prison.code,
      summary = title,
      incidentDetails = description ?: "NO DETAILS GIVEN",
      reportedBy = reportingStaff.username,
      reportedDate = reportedDateTime,
      status = mapIncidentStatus(status.code),
      questionSetId = questionnaireId.toString(),
      createdDate = LocalDateTime.now(clock),
      lastModifiedDate = LocalDateTime.now(clock),
      lastModifiedBy = reportingStaff.username,
      source = InformationSource.NOMIS,
      assignedTo = reportingStaff.username,
      event = IncidentEvent(
        eventId = "$incidentId",
        eventDateAndTime = incidentDateTime,
        prisonId = prison.code,
        eventDetails = description ?: "NO DETAILS GIVEN",
        createdDate = LocalDateTime.now(clock),
        lastModifiedDate = LocalDateTime.now(clock),
        lastModifiedBy = reportingStaff.username,
      ),
    )

    staffParties.forEach {
      ir.addStaffInvolved(
        staffRole = mapStaffRole(it.role.code),
        username = it.staff.username,
        comment = it.comment,
      )
    }

    offenderParties.forEach {
      ir.addPrisonerInvolved(
        prisonerNumber = it.offender.offenderNo,
        prisonerInvolvement = mapPrisonerRole(it.role.code),
        prisonerOutcome = it.outcome?.let { prisonerOutcome -> mapPrisonerOutcome(prisonerOutcome.code) },
        comment = it.comment,
      )
    }

    requirements.forEach {
      ir.addCorrectionRequest(
        correctionRequestedBy = it.staff.username,
        correctionRequestedAt = it.date.atStartOfDay(),
        descriptionOfChange = it.comment,
        reason = CorrectionReason.OTHER,
      )
    }

    questions.sortedBy { it.sequence }.forEach { question ->
      val dataItem = ir.addIncidentData(
        dataItem = "QID-%012d".format(question.questionId),
        dataItemDescription = question.question,
      )
      question.answers
        .sortedBy { it.sequence }
        .filter { it.answer != null }
        .forEach { answer ->
          dataItem.addDataItem(
            itemValue = answer.answer!!,
            additionalInformation = answer.comment,
            recordedBy = answer.recordingStaff.username,
            recordedOn = ir.reportedDate,
          )
        }
    }
    return ir
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
  @Schema(description = "Staff involved in the incident")
  val staff: Staff,
  @Schema(description = "Staff role in the incident")
  val role: CodeDescription,
  @Schema(description = "General information about the incident")
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
  @Schema(description = "Offender involved in the incident")
  val offender: Offender,
  @Schema(description = "Offender role in the incident")
  val role: CodeDescription,
  @Schema(description = "The outcome of the incident")
  val outcome: CodeDescription?,
  @Schema(description = "General information about the incident")
  val comment: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Requirement(
  @Schema(description = "The update required to the incident report")
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
  @Schema(description = "The sequence number of the question for this incident")
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
  @Schema(description = "The sequence number of the response for this incident")
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
  @Schema(description = "The history questionnaire id for the incident")
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
  @Schema(description = "The sequence number of the response question for this incident")
  val questionId: Long,
  @Schema(description = "The sequence number of the question for this incident")
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
  @Schema(description = "The sequence number of the response for this incident")
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

fun mapStaffRole(code: String) =
  when (code) {
    "AI" -> StaffRole.ACTIVELY_INVOLVED
    "AO" -> StaffRole.AUTHORISING_OFFICER
    "CRH" -> StaffRole.CR_HEAD
    "CRS" -> StaffRole.CR_SUPERVISOR
    "CRLG" -> StaffRole.CR_LEGS
    "CRL" -> StaffRole.CR_LEFT_ARM
    "CRR" -> StaffRole.CR_RIGHT_ARM
    "DECEASED" -> StaffRole.DECEASED
    "FOS" -> StaffRole.FIRST_ON_SCENE
    "HEALTH" -> StaffRole.HEALTHCARE
    "HOST" -> StaffRole.HOSTAGE
    "INPOS" -> StaffRole.IN_POSSESSION
    "INV" -> StaffRole.ACTIVELY_INVOLVED
    "NEG" -> StaffRole.NEGOTIATOR
    "PAS" -> StaffRole.PRESENT_AT_SCENE
    "SUSIN" -> StaffRole.SUSPECTED_INVOLVEMENT
    "VICT" -> StaffRole.VICTIM
    "WIT" -> StaffRole.WITNESS
    else -> throw RuntimeException("Unknown staff code: $code")
  }

fun mapIncidentStatus(code: String) =
  when (code) {
    "AWAN" -> IncidentStatus.AWAITING_ANALYSIS
    "INAN" -> IncidentStatus.IN_ANALYSIS
    "INREQ" -> IncidentStatus.INFORMATION_REQUIRED
    "INAME" -> IncidentStatus.INFORMATION_AMENDED
    "CLOSE" -> IncidentStatus.CLOSED
    "PIU" -> IncidentStatus.POST_INCIDENT_UPDATE
    "IUP" -> IncidentStatus.INCIDENT_UPDATED
    "DUP" -> IncidentStatus.DUPLICATE
    else -> throw RuntimeException("Unknown incident status: $code")
  }

fun convertIncidentType(type: String) = when (type) {
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

fun mapPrisonerOutcome(code: String) = when (code) {
  "ACCT" -> PrisonerOutcome.ACCT
  "CBP" -> PrisonerOutcome.CHARGED_BY_POLICE
  "CON" -> PrisonerOutcome.CONVICTED
  "CORIN" -> PrisonerOutcome.CORONER_INFORMED
  "DEA" -> PrisonerOutcome.DEATH
  "DUTGOV" -> PrisonerOutcome.SEEN_DUTY_GOV
  "FCHRG" -> PrisonerOutcome.FURTHER_CHARGES
  "HELTH" -> PrisonerOutcome.SEEN_HEALTHCARE
  "ILOC" -> PrisonerOutcome.LOCAL_INVESTIGATION
  "IMB" -> PrisonerOutcome.SEEN_IMB
  "IPOL" -> PrisonerOutcome.POLICE_INVESTIGATION
  "NKI" -> PrisonerOutcome.NEXT_OF_KIN_INFORMED
  "OUTH" -> PrisonerOutcome.SEEN_OUTSIDE_HOSP
  "POR" -> PrisonerOutcome.PLACED_ON_REPORT
  "RMND" -> PrisonerOutcome.REMAND
  "TRL" -> PrisonerOutcome.TRIAL
  "TRN" -> PrisonerOutcome.TRANSFER

  else -> throw RuntimeException("Unknown prisoner outcome: $code")
}

fun mapPrisonerRole(code: String): PrisonerRole = when (code) {
  "ABSCONDEE" -> PrisonerRole.ABSCONDER
  "ACTIVE_INVOLVEMENT" -> PrisonerRole.ACTIVE_INVOLVEMENT
  "ASSAILANT" -> PrisonerRole.ASSAILANT
  "ASSISTED_STAFF" -> PrisonerRole.ASSISTED_STAFF
  "DECEASED" -> PrisonerRole.DECEASED
  "ESCAPE" -> PrisonerRole.ESCAPE
  "FIGHT" -> PrisonerRole.FIGHTER
  "HOST" -> PrisonerRole.HOSTAGE
  "IMPED" -> PrisonerRole.IMPEDED_STAFF
  "INPOSS" -> PrisonerRole.IN_POSSESSION
  "INREC" -> PrisonerRole.INTENDED_RECIPIENT
  "LICFAIL" -> PrisonerRole.LICENSE_FAILURE
  "PERP" -> PrisonerRole.PERPETRATOR
  "PRESENT" -> PrisonerRole.PRESENT_AT_SCENE
  "SUSASS" -> PrisonerRole.SUSPECTED_ASSAILANT
  "SUSINV" -> PrisonerRole.SUSPECTED_INVOLVED
  "TRF" -> PrisonerRole.TEMPORARY_RELEASE_FAILURE
  "VICT" -> PrisonerRole.VICTIM
  else -> throw RuntimeException("Unknown prisoner role: $code")
}
