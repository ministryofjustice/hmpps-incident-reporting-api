package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.incidentreporting.constants.CorrectionReason
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Event
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import java.time.Clock
import java.time.LocalDateTime

fun NomisReport.toNewEntity(clock: Clock): Report {
  val report = Report(
    incidentNumber = "$incidentId",
    type = mapIncidentType(type),
    incidentDateAndTime = incidentDateTime,
    prisonId = prison.code,
    title = title ?: "NO DETAILS GIVEN",
    description = description ?: "NO DETAILS GIVEN",
    reportedBy = reportingStaff.username,
    reportedDate = reportedDateTime,
    status = mapIncidentStatus(status.code),
    questionSetId = "$questionnaireId",
    createdDate = LocalDateTime.now(clock),
    lastModifiedDate = LocalDateTime.now(clock),
    lastModifiedBy = reportingStaff.username,
    source = InformationSource.NOMIS,
    assignedTo = reportingStaff.username,
    event = Event(
      eventId = "$incidentId",
      eventDateAndTime = incidentDateTime,
      prisonId = prison.code,
      title = title ?: "NO DETAILS GIVEN",
      description = description ?: "NO DETAILS GIVEN",
      createdDate = LocalDateTime.now(clock),
      lastModifiedDate = LocalDateTime.now(clock),
      lastModifiedBy = reportingStaff.username,
    ),
  )

  staffParties.forEach {
    report.addStaffInvolved(
      staffRole = mapStaffRole(it.role.code),
      username = it.staff.username,
      comment = it.comment,
    )
  }

  offenderParties.forEach {
    report.addPrisonerInvolved(
      prisonerNumber = it.offender.offenderNo,
      prisonerInvolvement = mapPrisonerRole(it.role.code),
      prisonerOutcome = it.outcome?.let { prisonerOutcome -> mapPrisonerOutcome(prisonerOutcome.code) },
      comment = it.comment,
    )
  }

  requirements.forEach {
    report.addCorrectionRequest(
      correctionRequestedBy = it.staff.username,
      correctionRequestedAt = it.date.atStartOfDay(),
      descriptionOfChange = it.comment ?: "NO DETAILS GIVEN",
      reason = CorrectionReason.OTHER,
    )
  }

  questions.sortedBy { it.sequence }.forEach { question ->
    val dataItem = report.addQuestion(
      code = "QID-%012d".format(question.questionId),
      question = question.question,
    )
    question.answers
      .filter { it.answer != null }
      .sortedBy { it.sequence }
      .forEach { answer ->
        dataItem.addResponse(
          response = answer.answer!!,
          additionalInformation = answer.comment,
          recordedBy = answer.recordingStaff.username,
          recordedOn = report.reportedDate,
        )
      }
  }

  history.forEach { history ->
    val historyRecord = report.addHistory(
      type = mapIncidentType(history.type),
      incidentChangeDate = history.incidentChangeDate.atStartOfDay(),
      staffChanged = history.incidentChangeStaff.username,
    )

    history.questions.sortedBy { it.sequence }.forEach { question ->
      val dataItem = historyRecord.addQuestion(
        code = "QID-%012d".format(question.questionId),
        question = question.question,
      )
      question.answers
        .filter { it.answer != null }
        .sortedBy { it.responseSequence }
        .forEach { answer ->
          dataItem.addResponse(
            response = answer.answer!!,
            additionalInformation = answer.comment,
            recordedBy = answer.recordingStaff.username,
            recordedOn = report.reportedDate,
          )
        }
    }
  }
  return report
}

fun mapIncidentType(type: String) = when (type) {
  "SELF_HARM" -> Type.SELF_HARM
  "MISC" -> Type.MISCELLANEOUS
  "ASSAULTS3" -> Type.ASSAULT
  "DAMAGE" -> Type.DAMAGE
  "FIND0422" -> Type.FINDS
  "KEY_LOCKNEW" -> Type.KEY_LOCK_INCIDENT
  "DISORDER1" -> Type.DISORDER
  "FIRE" -> Type.FIRE
  "TOOL_LOSS" -> Type.TOOL_LOSS
  "FOOD_REF" -> Type.FOOD_REFUSAL
  "DEATH" -> Type.DEATH_IN_CUSTODY
  "TRF3" -> Type.TEMPORARY_RELEASE_FAILURE
  "RADIO_COMP" -> Type.RADIO_COMPROMISE
  "DRONE1" -> Type.DRONE_SIGHTING
  "ABSCOND" -> Type.ABSCONDER
  "REL_ERROR" -> Type.RELEASED_IN_ERROR
  "BOMB" -> Type.BOMB_THREAT
  "CLOSE_DOWN" -> Type.FULL_CLOSE_DOWN_SEARCH
  "BREACH" -> Type.BREACH_OF_SECURITY
  "DEATH_NI" -> Type.DEATH_OTHER
  "ESCAPE_EST" -> Type.ESCAPE_FROM_CUSTODY
  "ATT_ESCAPE" -> Type.ATTEMPTED_ESCAPE_FROM_CUSTODY
  "ESCAPE_ESC" -> Type.ESCAPE_FROM_ESCORT
  "ATT_ESC_E" -> Type.ATTEMPTED_ESCAPE_FROM_ESCORT
  else -> throw ValidationException("Unknown incident type: $type")
}

fun mapIncidentStatus(code: String) = when (code) {
  "AWAN" -> Status.AWAITING_ANALYSIS
  "INAN" -> Status.IN_ANALYSIS
  "INREQ" -> Status.INFORMATION_REQUIRED
  "INAME" -> Status.INFORMATION_AMENDED
  "CLOSE" -> Status.CLOSED
  "PIU" -> Status.POST_INCIDENT_UPDATE
  "IUP" -> Status.INCIDENT_UPDATED
  "DUP" -> Status.DUPLICATE
  else -> throw ValidationException("Unknown incident status: $code")
}

fun mapStaffRole(code: String) = when (code) {
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
  else -> throw ValidationException("Unknown staff code: $code")
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
  else -> throw ValidationException("Unknown prisoner outcome: $code")
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
  else -> throw ValidationException("Unknown prisoner role: $code")
}
