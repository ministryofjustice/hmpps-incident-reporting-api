package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import uk.gov.justice.digital.hmpps.incidentreporting.constants.CorrectionReason
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.NO_DETAILS_GIVEN
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.CorrectionRequest
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Event
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.PrisonerInvolvement
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.StaffInvolvement

fun NomisReport.toNewEntity(): Report {
  val status = Status.fromNomisCode(status.code)
  val report = Report(
    reportReference = "$incidentId",
    type = Type.fromNomisCode(type),
    incidentDateAndTime = incidentDateTime,
    location = prison.code,
    title = title ?: NO_DETAILS_GIVEN,
    description = description ?: NO_DETAILS_GIVEN,
    reportedBy = reportingStaff.username,
    reportedAt = reportedDateTime,
    status = status,
    questionSetId = "$questionnaireId",
    createdAt = createDateTime,
    modifiedAt = lastModifiedDateTime ?: createDateTime,
    modifiedBy = lastModifiedBy ?: createdBy,
    source = InformationSource.NOMIS,
    modifiedIn = InformationSource.NOMIS,
    assignedTo = reportingStaff.username,
    event = Event(
      eventReference = "$incidentId",
      eventDateAndTime = incidentDateTime,
      location = prison.code,
      title = title ?: NO_DETAILS_GIVEN,
      description = description ?: NO_DETAILS_GIVEN,
      createdAt = createDateTime,
      modifiedAt = lastModifiedDateTime ?: createDateTime,
      modifiedBy = lastModifiedBy ?: createdBy,
    ),
  )
  report.addStatusHistory(status, reportedDateTime, reportingStaff.username)

  report.updateNomisStaffInvolvements(staffParties)
  report.updateNomisPrisonerInvolvements(offenderParties)
  report.updateNomisCorrectionRequests(requirements)
  report.updateQuestionAndResponses(questions)
  report.updateHistory(history)

  return report
}

private fun Report.createStaffInvolved(staffParty: NomisStaffParty): StaffInvolvement =
  StaffInvolvement(
    report = this,
    staffUsername = staffParty.staff.username,
    staffRole = StaffRole.fromNomisCode(staffParty.role.code),
    comment = staffParty.comment,
  )

private fun Report.createPrisonerInvolvement(nomisOffenderParty: NomisOffenderParty): PrisonerInvolvement =
  PrisonerInvolvement(
    report = this,
    prisonerNumber = nomisOffenderParty.offender.offenderNo,
    prisonerRole = PrisonerRole.fromNomisCode(nomisOffenderParty.role.code),
    outcome = nomisOffenderParty.outcome?.let { prisonerOutcome -> PrisonerOutcome.fromNomisCode(prisonerOutcome.code) },
    comment = nomisOffenderParty.comment,
  )

private fun Report.createCorrectionRequest(correctionRequest: NomisRequirement): CorrectionRequest =
  CorrectionRequest(
    report = this,
    correctionRequestedBy = correctionRequest.staff.username,
    correctionRequestedAt = correctionRequest.date.atStartOfDay(),
    descriptionOfChange = correctionRequest.comment ?: NO_DETAILS_GIVEN,
    reason = CorrectionReason.NOT_SPECIFIED,
  )

fun Report.updateNomisPrisonerInvolvements(offenderParties: Collection<NomisOffenderParty>) {
  this.prisonersInvolved.retainAll(
    offenderParties.map { offenderParty ->
      val newPrisoner = createPrisonerInvolvement(offenderParty)
      prisonersInvolved.find { it == newPrisoner } ?: addPrisonerInvolved(newPrisoner)
    }.toSet(),
  )
}

fun Report.updateNomisCorrectionRequests(nomisRequirement: Collection<NomisRequirement>) {
  this.correctionRequests.retainAll(
    nomisRequirement.map { correctionRequest ->
      val newCorrection = createCorrectionRequest(correctionRequest)
      this.correctionRequests.find { it == newCorrection } ?: addCorrectionRequest(newCorrection)
    }.toSet(),
  )
}

fun Report.updateNomisStaffInvolvements(staffParties: Collection<NomisStaffParty>) {
  this.staffInvolved.retainAll(
    staffParties.map { staffParty ->
      val newStaff = createStaffInvolved(staffParty)
      this.staffInvolved.find { it == newStaff } ?: addStaffInvolved(newStaff)
    }.toSet(),
  )
}

fun Report.addNomisQuestion(question: NomisQuestion) =
  this.addQuestion(
    code = question.questionId.toString(),
    sequence = question.sequence - 1,
    question = question.question,
  )
