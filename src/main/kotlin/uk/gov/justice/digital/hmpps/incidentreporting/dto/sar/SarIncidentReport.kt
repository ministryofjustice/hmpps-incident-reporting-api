package uk.gov.justice.digital.hmpps.incidentreporting.dto.sar

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.CorrectionRequest
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.DescriptionAddendum
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.HistoricalQuestion
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.HistoricalResponse
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.History
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.PrisonerInvolvement
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Question
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Response
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.StaffInvolvement
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.StatusHistory
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * One incident report a prisoner is named on, as it appears in a subject access request response.
 *
 * This is the response the Offender SAR team signed off at the data review (IR-1461), and follows the
 * `[SAR: Y|N]` tags on the column comments (V1_49, with their changes in V1_50). What is left out is as deliberate as what is included, and
 * follows the SAR integration guiding principles:
 *  - internal identifiers are excluded - the report's UUID, every child row's id and sequence, and the id of a
 *    duplicated report. The report reference is the number people actually quote;
 *  - NOMIS and sync bookkeeping is excluded - where the report was created or last changed, the question set id,
 *    and the "done" flags the reporting journey uses;
 *  - codes that stand for something travel as their descriptions (Self-harm, Victim, Awaiting review), which are
 *    the wording the incident reporting service shows its own users;
 *  - questions and answers travel as their labels, the wording the user was shown when the report was written.
 *    The `question` and `response` columns are the legacy NOMIS form, and on some newer types a key such as
 *    `lock_type`, so they are not for display. The codes are business keys for analysis and are excluded;
 *  - prison codes and usernames travel raw, in attributes of their own, so the template resolves them with
 *    `getPrisonName` and `getUserLastName`;
 *  - nothing owned by another service is fetched. The names of prisoners and staff are those recorded on the
 *    report itself;
 *  - a member of staff appears by surname only, with nothing else about them - not their first name, the
 *    capacity they acted in or the prison they were working at. That is the Offender SAR team's rule from the
 *    data review.
 *
 * Other people named on a report - other prisoners and staff - are included, with what is recorded about them.
 * Redacting third-party data is the Offender SAR team's job, so the default is to return it and let them
 * redact, as they agreed at the data review.
 */
@Schema(description = "An incident report the prisoner is named on, for a subject access request")
data class SarIncidentReport(
  @Schema(description = "The report number people quote, previously the NOMIS incident number", example = "1234567")
  val reportReference: String,

  @Schema(description = "What kind of incident this was", example = "Self-harm")
  val type: String,

  @Schema(description = "Where the report is in its review", example = "Awaiting review")
  val status: String,

  @Schema(description = "When the incident happened")
  val incidentDateAndTime: LocalDateTime,

  @Schema(description = "Prison where the incident happened", example = "MDI")
  val location: String,

  @Schema(description = "Short title. For reports created in DPS it names every prisoner involved")
  val title: String,

  @Schema(description = "What happened, as written by the reporting officer")
  val description: String,

  @Schema(description = "Username of the member of staff who reported the incident", example = "AUSER_GEN")
  val reportedBy: String,

  @Schema(description = "When the incident was reported")
  val reportedAt: LocalDateTime,

  @Schema(description = "Additions made to the description after the report was submitted, oldest first")
  val descriptionAddendums: List<SarDescriptionAddendum>,

  @Schema(description = "Prisoners named on the report, including the prisoner the request is about")
  val prisonersInvolved: List<SarPrisonerInvolvement>,

  @Schema(description = "Members of staff named on the report")
  val staffInvolved: List<SarStaffInvolvement>,

  @Schema(description = "The questions asked about the incident and the answers given, in the order asked")
  val questions: List<SarQuestion>,

  @Schema(description = "Requests made to correct or change the report, oldest first")
  val correctionRequests: List<SarCorrectionRequest>,

  @Schema(description = "Every status the report has had, oldest first")
  val statusHistory: List<SarStatusChange>,

  @Schema(
    description = "Incident types the report had before it was changed, with the questions answered then, oldest first",
  )
  val typeHistory: List<SarTypeChange>,
) {
  companion object {
    /**
     * [prisonerNumber] is the prisoner the request is about, so their own entry among the prisoners involved
     * can be marked - see [SarPrisonerInvolvement.subjectOfRequest].
     */
    fun from(report: Report, prisonerNumber: String) = SarIncidentReport(
      reportReference = report.reportReference,
      type = report.type.description,
      status = report.status.description,
      incidentDateAndTime = report.incidentDateAndTime,
      location = report.location,
      title = report.title,
      description = report.description,
      reportedBy = report.reportedBy,
      reportedAt = report.reportedAt,
      descriptionAddendums = report.descriptionAddendums.map(SarDescriptionAddendum::from),
      prisonersInvolved = report.prisonersInvolved.map { SarPrisonerInvolvement.from(it, prisonerNumber) },
      staffInvolved = report.staffInvolved.map(SarStaffInvolvement::from),
      questions = report.questions.map(SarQuestion::from),
      correctionRequests = report.correctionRequests.map(SarCorrectionRequest::from),
      statusHistory = report.historyOfStatuses.map(SarStatusChange::from),
      typeHistory = report.history.map(SarTypeChange::from),
    )
  }
}

@Schema(description = "An addition made to the description of an incident after the report was submitted")
data class SarDescriptionAddendum(
  @Schema(description = "What was added")
  val text: String,
  @Schema(description = "When it was added")
  val createdAt: LocalDateTime,
  @Schema(description = "Surname of the member of staff who added it, as recorded at the time", example = "Brown")
  val addedByLastName: String,
) {
  companion object {
    fun from(addendum: DescriptionAddendum) = SarDescriptionAddendum(
      text = addendum.text,
      createdAt = addendum.createdAt,
      addedByLastName = addendum.lastName,
    )
  }
}

/**
 * A prisoner named on the report. [prisonerNumber] is included for every prisoner, as the one identifier the
 * guiding principles allow, but the template prints it only for other prisoners: the SAR tool already prints the
 * requester's own number at the top of every page, and [subjectOfRequest] marks which entry that is.
 */
@Schema(description = "A prisoner named on an incident report")
data class SarPrisonerInvolvement(
  @Schema(description = "The prisoner's prison number", example = "A1234BC")
  val prisonerNumber: String,
  @Schema(description = "Whether this is the prisoner the request is about")
  val subjectOfRequest: Boolean,
  @Schema(description = "First name, as recorded on the report")
  val firstName: String,
  @Schema(description = "Last name, as recorded on the report")
  val lastName: String,
  @Schema(description = "How the prisoner was involved", example = "Victim")
  val role: String,
  @Schema(description = "What happened to the prisoner as a result", example = "Seen by Healthcare", nullable = true)
  val outcome: String?,
  @Schema(description = "Comment about the prisoner's involvement", nullable = true)
  val comment: String?,
) {
  companion object {
    fun from(involvement: PrisonerInvolvement, subjectPrisonerNumber: String) = SarPrisonerInvolvement(
      prisonerNumber = involvement.prisonerNumber,
      subjectOfRequest = involvement.prisonerNumber == subjectPrisonerNumber,
      firstName = involvement.firstName,
      lastName = involvement.lastName,
      role = involvement.prisonerRole.description,
      outcome = involvement.outcome?.description,
      comment = involvement.comment,
    )
  }
}

@Schema(description = "A member of staff named on an incident report")
data class SarStaffInvolvement(
  @Schema(description = "Last name, as recorded on the report")
  val lastName: String,
  @Schema(description = "How the member of staff was involved", example = "First on scene")
  val role: String,
  @Schema(description = "Comment about the member of staff's involvement", nullable = true)
  val comment: String?,
) {
  companion object {
    fun from(involvement: StaffInvolvement) = SarStaffInvolvement(
      lastName = involvement.lastName,
      role = involvement.staffRole.description,
      comment = involvement.comment,
    )
  }
}

@Schema(description = "A question asked about an incident, with the answers given")
data class SarQuestion(
  @Schema(description = "The question, worded as the user was shown it")
  val question: String,
  @Schema(description = "Further information given against the question as a whole", nullable = true)
  val additionalInformation: String?,
  @Schema(description = "The answers given, in the order offered")
  val responses: List<SarResponse>,
) {
  companion object {
    fun from(question: Question) = SarQuestion(
      question = question.label,
      additionalInformation = question.additionalInformation,
      responses = question.responses.sortedBy { it.sequence }.map(SarResponse::from),
    )

    fun from(question: HistoricalQuestion) = SarQuestion(
      question = question.label,
      additionalInformation = question.additionalInformation,
      responses = question.responses.sortedBy { it.sequence }.map(SarResponse::from),
    )
  }
}

@Schema(description = "An answer given to a question about an incident")
data class SarResponse(
  @Schema(description = "The answer, worded as the user was shown it")
  val response: String,
  @Schema(description = "Date given as the answer, where the question asks for one", nullable = true)
  val responseDate: LocalDate?,
  @Schema(description = "Further information given with the answer", nullable = true)
  val additionalInformation: String?,
  @Schema(description = "Username of the member of staff who recorded the answer", example = "AUSER_GEN")
  val recordedBy: String,
  @Schema(description = "When the answer was recorded")
  val recordedAt: LocalDateTime,
) {
  companion object {
    fun from(response: Response) = SarResponse(
      response = response.label,
      responseDate = response.responseDate,
      additionalInformation = response.additionalInformation,
      recordedBy = response.recordedBy,
      recordedAt = response.recordedAt,
    )

    fun from(response: HistoricalResponse) = SarResponse(
      response = response.label,
      responseDate = response.responseDate,
      additionalInformation = response.additionalInformation,
      recordedBy = response.recordedBy,
      recordedAt = response.recordedAt,
    )
  }
}

@Schema(description = "A request to correct or change an incident report")
data class SarCorrectionRequest(
  @Schema(description = "What the request asks to be changed, and why")
  val descriptionOfChange: String,
  @Schema(description = "When the request was made")
  val correctionRequestedAt: LocalDateTime,
  @Schema(description = "Username of the member of staff who made the request", example = "CWARDEN_GEN")
  val correctionRequestedBy: String,
  @Schema(description = "What the member of staff asked for or did", example = "Request correction", nullable = true)
  val userAction: String?,
  @Schema(
    description = "Report number of the report this one duplicates, on a request to mark it as a duplicate",
    nullable = true,
  )
  val originalReportReference: String?,
) {
  companion object {
    fun from(correctionRequest: CorrectionRequest) = SarCorrectionRequest(
      descriptionOfChange = correctionRequest.descriptionOfChange,
      correctionRequestedAt = correctionRequest.correctionRequestedAt,
      correctionRequestedBy = correctionRequest.correctionRequestedBy,
      userAction = correctionRequest.userAction?.description,
      originalReportReference = correctionRequest.originalReportReference,
    )
  }
}

@Schema(description = "A status an incident report has had")
data class SarStatusChange(
  @Schema(description = "The status", example = "Needs updating")
  val status: String,
  @Schema(description = "When the report was given this status")
  val changedAt: LocalDateTime,
  @Schema(description = "Username of the member of staff who gave it this status", example = "CWARDEN_GEN")
  val changedBy: String,
) {
  companion object {
    fun from(statusHistory: StatusHistory) = SarStatusChange(
      status = statusHistory.status.description,
      changedAt = statusHistory.changedAt,
      changedBy = statusHistory.changedBy,
    )
  }
}

/**
 * The incident type a report had before it was changed. Changing the type replaces the questions asked, so the
 * questions and answers given under the earlier type are kept here and disclosed like the current ones - they
 * record what was said about the incident at the time.
 */
@Schema(description = "An incident type a report had before it was changed")
data class SarTypeChange(
  @Schema(description = "The type the report had before the change", example = "Assault")
  val previousType: String,
  @Schema(description = "When the type was changed")
  val changedAt: LocalDateTime,
  @Schema(description = "Username of the member of staff who changed it", example = "AUSER_GEN")
  val changedBy: String,
  @Schema(description = "The questions answered under the earlier type, in the order asked")
  val questions: List<SarQuestion>,
) {
  companion object {
    fun from(history: History) = SarTypeChange(
      previousType = history.type.description,
      changedAt = history.changedAt,
      changedBy = history.changedBy,
      questions = history.questions.map(SarQuestion::from),
    )
  }
}
