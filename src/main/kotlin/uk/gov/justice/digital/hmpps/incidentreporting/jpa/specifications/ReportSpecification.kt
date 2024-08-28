package uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications

import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.PrisonerInvolvement
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.StaffInvolvement
import java.time.LocalDate

fun filterByPrisonId(prisonId: String) = Report::prisonId.buildSpecForEqualTo(prisonId)

fun filterBySource(informationSource: InformationSource) = Report::source.buildSpecForEqualTo(informationSource)

fun filterByStatuses(statuses: Collection<Status>) = Report::status.buildSpecForIn(statuses)
fun filterByStatuses(vararg statuses: Status) = filterByStatuses(statuses.toList())

fun filterByType(type: Type) = Report::type.buildSpecForEqualTo(type)

fun filterByIncidentDateFrom(date: LocalDate) =
  Report::incidentDateAndTime.buildSpecForGreaterThanOrEqualTo(date.atStartOfDay())

fun filterByIncidentDateUntil(date: LocalDate) =
  Report::incidentDateAndTime.buildSpecForLessThan(date.plusDays(1).atStartOfDay())

fun filterByReportedDateFrom(date: LocalDate) =
  Report::reportedAt.buildSpecForGreaterThanOrEqualTo(date.atStartOfDay())

fun filterByReportedDateUntil(date: LocalDate) =
  Report::reportedAt.buildSpecForLessThan(date.plusDays(1).atStartOfDay())

fun filterByInvolvedStaff(staffUsername: String) =
  Report::staffInvolved.buildSpecForRelatedEntityPropertyEqualTo(StaffInvolvement::staffUsername, staffUsername)

fun filterByInvolvedPrisoner(prisonerNumber: String) =
  Report::prisonersInvolved.buildSpecForRelatedEntityPropertyEqualTo(PrisonerInvolvement::prisonerNumber, prisonerNumber)
