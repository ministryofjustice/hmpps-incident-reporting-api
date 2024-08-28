package uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications

import jakarta.persistence.criteria.Join
import org.springframework.data.jpa.domain.Specification
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

fun filterByInvolvedStaff(staffUsername: String): Specification<Report> {
  return Specification { root, _, criteriaBuilder ->
    val staffInvolved: Join<StaffInvolvement, Report> = root.join("staffInvolved")
    criteriaBuilder.equal(staffInvolved.get<String>("staffUsername"), staffUsername)
  }
}

fun filterByInvolvedPrisoner(prisonerNumber: String): Specification<Report> {
  return Specification { root, _, criteriaBuilder ->
    val prisonersInvolved: Join<PrisonerInvolvement, Report> = root.join("prisonersInvolved")
    criteriaBuilder.equal(prisonersInvolved.get<String>("prisonerNumber"), prisonerNumber)
  }
}
