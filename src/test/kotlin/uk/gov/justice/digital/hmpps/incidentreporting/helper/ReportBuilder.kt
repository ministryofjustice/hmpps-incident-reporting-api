package uk.gov.justice.digital.hmpps.incidentreporting.helper

import uk.gov.justice.digital.hmpps.incidentreporting.constants.CorrectionReason
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import java.time.LocalDateTime

fun buildReport(
  reportReference: String,
  /** When report was created */
  reportTime: LocalDateTime,
  prisonId: String = "MDI",
  source: InformationSource = InformationSource.DPS,
  status: Status = Status.DRAFT,
  type: Type = Type.FINDS,
  reportingUsername: String = "USER1",
  // all related entities apart from event are optionally generated:
  generateStaffInvolvement: Int = 0,
  generatePrisonerInvolvement: Int = 0,
  generateCorrections: Int = 0,
  generateQuestions: Int = 0,
  generateResponses: Int = 0,
  generateHistory: Int = 0,
): Report {
  val eventDateAndTime = reportTime.minusHours(1)
  val report = Report(
    reportReference = reportReference,
    incidentDateAndTime = eventDateAndTime,
    prisonId = prisonId,
    source = source,
    status = status,
    type = type,
    title = "Incident Report $reportReference",
    description = "A new incident created in the new service of type ${type.description}",
    reportedAt = reportTime,
    createdAt = reportTime,
    modifiedAt = reportTime,
    reportedBy = reportingUsername,
    assignedTo = reportingUsername,
    modifiedBy = reportingUsername,
    event = buildEvent(
      eventReference = reportReference,
      eventDateAndTime = eventDateAndTime,
      reportDateAndTime = reportTime,
      prisonId = prisonId,
      reportingUsername = reportingUsername,
    ),
  )
  report.addStatusHistory(report.status, reportTime, reportingUsername)

  (1..generateStaffInvolvement).forEach { staffIndex ->
    report.addStaffInvolved(
      staffUsername = "staff-$staffIndex",
      staffRole = StaffRole.entries.elementAtWrapped(staffIndex),
      comment = "Comment #$staffIndex",
    )
  }
  (1..generatePrisonerInvolvement).forEach { prisonerIndex ->
    report.addPrisonerInvolved(
      prisonerNumber = "A%04dAA".format(prisonerIndex),
      prisonerRole = PrisonerRole.entries.elementAtWrapped(prisonerIndex),
      outcome = PrisonerOutcome.entries.elementAtWrapped(prisonerIndex),
      comment = "Comment #$prisonerIndex",
    )
  }
  (1..generateCorrections).forEach { correctionIndex ->
    report.addCorrectionRequest(
      correctionRequestedAt = reportTime,
      correctionRequestedBy = "qa",
      reason = CorrectionReason.NOT_SPECIFIED,
      descriptionOfChange = "Fix request #$correctionIndex",
    )
  }

  (1..generateQuestions).forEach { questionIndex ->
    val question = report.addQuestion(
      code = "QID-%012d".format(questionIndex),
      question = "Question #$questionIndex",
      additionalInformation = "Explanation #$questionIndex",
    )
    (1..generateResponses).forEach { responseIndex ->
      question.addResponse(
        response = "Response #$responseIndex",
        responseDate = eventDateAndTime.toLocalDate().minusDays(responseIndex.toLong()),
        additionalInformation = "Prose #$responseIndex",
        recordedBy = "some-user",
        recordedAt = reportTime,
      )
    }
  }

  (1..generateHistory).forEach { historyIndex ->
    val history = report.addHistory(
      type = Type.entries.elementAtWrapped(historyIndex),
      changedAt = reportTime.minusMinutes((generateHistory - historyIndex).toLong()),
      changedBy = "some-past-user",
    )
    (1..generateQuestions).forEach { questionIndex ->
      val historicalQuestion = history.addQuestion(
        code = "QID-$historyIndex-%012d".format(questionIndex),
        question = "Historical question #$historyIndex-$questionIndex",
        additionalInformation = "Explanation #$questionIndex in history #$historyIndex",
      )
      (1..generateResponses).forEach { responseIndex ->
        historicalQuestion.addResponse(
          response = "Historical response #$historyIndex-$responseIndex",
          responseDate = eventDateAndTime.toLocalDate().minusDays(responseIndex.toLong()),
          additionalInformation = "Prose #$responseIndex in history #$historyIndex",
          recordedBy = "some-user",
          recordedAt = reportTime,
        )
      }
    }
  }

  return report
}

fun <T> List<T>.elementAtWrapped(index: Int): T = get(index % size)
