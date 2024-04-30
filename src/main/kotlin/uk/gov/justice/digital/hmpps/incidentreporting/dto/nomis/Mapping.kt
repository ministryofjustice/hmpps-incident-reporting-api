package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

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
    type = Type.fromNomisCode(type),
    incidentDateAndTime = incidentDateTime,
    prisonId = prison.code,
    title = title ?: "NO DETAILS GIVEN",
    description = description ?: "NO DETAILS GIVEN",
    reportedBy = reportingStaff.username,
    reportedDate = reportedDateTime,
    status = Status.fromNomisCode(status.code),
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
      staffRole = StaffRole.fromNomisCode(it.role.code),
      username = it.staff.username,
      comment = it.comment,
    )
  }

  offenderParties.forEach {
    report.addPrisonerInvolved(
      prisonerNumber = it.offender.offenderNo,
      prisonerInvolvement = PrisonerRole.fromNomisCode(it.role.code),
      prisonerOutcome = it.outcome?.let { prisonerOutcome -> PrisonerOutcome.fromNomisCode(prisonerOutcome.code) },
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
      type = Type.fromNomisCode(history.type),
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
