package uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report

fun filterByPrisonId(prisonId: String): Specification<Report> {
  return Specification { root: Root<Report>, _: CriteriaQuery<*>, criteriaBuilder: CriteriaBuilder ->
    criteriaBuilder.equal(root.get<String>(Report::prisonId.name), prisonId)
  }
}

fun filterBySource(informationSource: InformationSource): Specification<Report> {
  return Specification { root: Root<Report>, _: CriteriaQuery<*>, criteriaBuilder: CriteriaBuilder ->
    criteriaBuilder.equal(root.get<InformationSource>(Report::source.name), informationSource)
  }
}

fun filterByStatus(status: Status): Specification<Report> {
  return Specification { root: Root<Report>, _: CriteriaQuery<*>, criteriaBuilder: CriteriaBuilder ->
    criteriaBuilder.equal(root.get<Status>(Report::status.name), status)
  }
}

fun filterByType(type: Type): Specification<Report> {
  return Specification { root: Root<Report>, _: CriteriaQuery<*>, criteriaBuilder: CriteriaBuilder ->
    criteriaBuilder.equal(root.get<Type>(Report::type.name), type)
  }
}
