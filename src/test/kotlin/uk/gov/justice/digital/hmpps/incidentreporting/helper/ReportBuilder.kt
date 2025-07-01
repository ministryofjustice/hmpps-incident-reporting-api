package uk.gov.justice.digital.hmpps.incidentreporting.helper

import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

fun buildReport(
  reportReference: String,
  /** When report was created */
  reportTime: LocalDateTime,
  location: String = "MDI",
  source: InformationSource = InformationSource.DPS,
  status: Status = Status.DRAFT,
  type: Type = Type.FIND_6,
  reportingUsername: String = "USER1",
  // all "related objects" are optionally generated:
  generateDescriptionAddendums: Int = 0,
  generateStaffInvolvement: Int = 0,
  generatePrisonerInvolvement: Int = 0,
  generateCorrections: Int = 0,
  generateQuestions: Int = 0,
  generateResponses: Int = 0,
  generateHistory: Int = 0,
): Report {
  val incidentDateAndTime = reportTime.minusHours(1)
  val report = Report(
    reportReference = reportReference,
    incidentDateAndTime = incidentDateAndTime,
    location = location,
    source = source,
    modifiedIn = source,
    status = status,
    type = type,
    title = "Incident Report $reportReference",
    description = "A new incident created in the new service of type ${type.description.lowercase()}",
    reportedAt = reportTime,
    createdAt = reportTime,
    modifiedAt = reportTime,
    reportedBy = reportingUsername,
    assignedTo = reportingUsername,
    modifiedBy = reportingUsername,
  )
  report.addStatusHistory(report.status, reportTime, reportingUsername)

  (1..generateDescriptionAddendums).forEach { addendumIndex ->
    report.addDescriptionAddendum(
      sequence = addendumIndex - 1,
      createdBy = "staff-$addendumIndex",
      // Round to seconds to account for NOMIS description format granularity
      createdAt = reportTime.truncatedTo(ChronoUnit.MINUTES),
      firstName = "First $addendumIndex",
      lastName = "Last $addendumIndex",
      text = "Addendum #$addendumIndex",
    )
  }
  (1..generateStaffInvolvement).forEach { staffIndex ->
    report.addStaffInvolved(
      sequence = staffIndex - 1,
      staffUsername = "staff-$staffIndex",
      firstName = "First $staffIndex",
      lastName = "Last $staffIndex",
      staffRole = StaffRole.entries.elementAtWrapped(staffIndex),
      comment = "Comment #$staffIndex",
    )
  }
  (1..generatePrisonerInvolvement).forEach { prisonerIndex ->
    report.addPrisonerInvolved(
      sequence = prisonerIndex - 1,
      prisonerNumber = "A%04dAA".format(prisonerIndex),
      firstName = "First $prisonerIndex",
      lastName = "Last $prisonerIndex",
      prisonerRole = PrisonerRole.entries.elementAtWrapped(prisonerIndex),
      outcome = PrisonerOutcome.entries.elementAtWrapped(prisonerIndex),
      comment = "Comment #$prisonerIndex",
    )
  }
  (1..generateCorrections).forEach { correctionIndex ->
    report.addCorrectionRequest(
      sequence = correctionIndex - 1,
      correctionRequestedAt = reportTime,
      correctionRequestedBy = "qa",
      descriptionOfChange = "Fix request #$correctionIndex",
      location = "MDI",
    )
  }

  (1..generateQuestions).forEach { questionIndex ->
    val question = report.addQuestion(
      code = questionIndex.toString(),
      question = "Question #$questionIndex",
      label = "Label #$questionIndex",
      sequence = questionIndex,
      additionalInformation = "Explanation #$questionIndex",
    )
    (1..generateResponses).forEach { responseIndex ->
      question.addResponse(
        code = "$questionIndex-$responseIndex",
        response = "Response #$responseIndex",
        label = "Label #$responseIndex",
        sequence = responseIndex - 1,
        responseDate = incidentDateAndTime.toLocalDate().minusDays(responseIndex.toLong()),
        additionalInformation = "Prose #$responseIndex",
        recordedBy = "some-user",
        recordedAt = reportTime,
      )
    }
  }

  (1..generateHistory).forEach { historyIndex ->
    val history = report.addHistory(
      type = Type.entries.filter { it.active }.elementAtWrapped(historyIndex),
      changedAt = reportTime.minusMinutes((generateHistory - historyIndex).toLong()),
      changedBy = "some-past-user",
    )
    (1..generateQuestions).forEach { questionIndex ->
      val historicalQuestion = history.addQuestion(
        code = "$historyIndex-$questionIndex",
        question = "Historical question #$historyIndex-$questionIndex",
        label = "Historical label #$historyIndex-$questionIndex",
        sequence = questionIndex,
        additionalInformation = "Explanation #$questionIndex in history #$historyIndex",
      )
      (1..generateResponses).forEach { responseIndex ->
        historicalQuestion.addResponse(
          code = "#$historyIndex-$responseIndex",
          response = "Historical response #$historyIndex-$responseIndex",
          label = "Historical label #$historyIndex-$responseIndex",
          responseDate = incidentDateAndTime.toLocalDate().minusDays(responseIndex.toLong()),
          additionalInformation = "Prose #$responseIndex in history #$historyIndex",
          recordedBy = "some-user",
          recordedAt = reportTime,
          sequence = responseIndex - 1,
        )
      }
    }
  }

  return report
}

fun <T> List<T>.elementAtWrapped(index: Int): T = get(index % size)
