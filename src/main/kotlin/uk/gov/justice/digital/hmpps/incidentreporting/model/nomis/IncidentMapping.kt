package uk.gov.justice.digital.hmpps.incidentreporting.model.nomis

import uk.gov.justice.digital.hmpps.incidentreporting.jpa.CorrectionReason
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentEvent
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentReport
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.convertIncidentType
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.mapIncidentStatus
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.mapPrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.mapPrisonerRole
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.mapStaffRole
import uk.gov.justice.digital.hmpps.incidentreporting.service.InformationSource
import java.time.Clock
import java.time.LocalDateTime

fun NomisIncidentReport.toNewEntity(clock: Clock): IncidentReport {
  val ir = IncidentReport(
    incidentNumber = "$incidentId",
    incidentType = convertIncidentType(type),
    incidentDateAndTime = incidentDateTime,
    prisonId = prison.code,
    summary = title,
    incidentDetails = description ?: "NO DETAILS GIVEN",
    reportedBy = reportingStaff.username,
    reportedDate = reportedDateTime,
    status = mapIncidentStatus(status.code),
    questionSetId = "$questionnaireId",
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
        dataItem.addAnswer(
          itemValue = answer.answer!!,
          additionalInformation = answer.comment,
          recordedBy = answer.recordingStaff.username,
          recordedOn = ir.reportedDate,
        )
      }
  }

  history.forEach { history ->
    val historyRecord = ir.addIncidentHistory(
      incidentType = convertIncidentType(history.type),
      incidentChangeDate = history.incidentChangeDate.atStartOfDay(),
      staffChanged = history.incidentChangeStaff.username,
    )

    history.questions.sortedBy { it.sequence }.forEach { question ->
      val dataItem = historyRecord.addHistoricalResponse(
        dataItem = "QID-%012d".format(question.questionId),
        dataItemDescription = question.question,
      )
      question.answers
        .sortedBy { it.responseSequence }
        .filter { it.answer != null }
        .forEach { answer ->
          dataItem.addAnswer(
            itemValue = answer.answer!!,
            additionalInformation = answer.comment,
            recordedBy = answer.recordingStaff.username,
            recordedOn = ir.reportedDate,
          )
        }
    }
  }
  return ir
}
