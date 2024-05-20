package uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications

import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report

fun filterByPrisonId(prisonId: String) = Report::prisonId.buildSpecForEqualTo(prisonId)

fun filterBySource(informationSource: InformationSource) = Report::source.buildSpecForEqualTo(informationSource)

fun filterByStatus(status: Status) = Report::status.buildSpecForEqualTo(status)

fun filterByType(type: Type) = Report::type.buildSpecForEqualTo(type)
