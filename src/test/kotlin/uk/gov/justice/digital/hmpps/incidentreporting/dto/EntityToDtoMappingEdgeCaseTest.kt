package uk.gov.justice.digital.hmpps.incidentreporting.dto

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.helper.buildIncidentReport
import uk.gov.justice.digital.hmpps.incidentreporting.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.EventRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository

/**
 * Tests for edge cases when converting JPA entities to DTO classes.
 * Largely, these mappings are field-by-field copies.
 * NB: most conversions are already covered by resource and service tests.
 */
class EntityToDtoMappingEdgeCaseTest : SqsIntegrationTestBase() {
  @Autowired
  lateinit var eventRepository: EventRepository

  @Autowired
  lateinit var reportRepository: ReportRepository

  @BeforeEach
  fun setUp() {
    reportRepository.deleteAll()
    eventRepository.deleteAll()
  }

  @Test
  fun `report must have a non-null id to map to the dto`() {
    val unsavedReport = buildIncidentReport(
      incidentNumber = "1234",
      reportTime = now,
    )
    assertThat(unsavedReport.id).isNull()
    assertThatThrownBy { unsavedReport.toDto() }
      .isInstanceOf(NullPointerException::class.java)

    val savedReport = reportRepository.save(unsavedReport)
    assertThat(savedReport.id).isNotNull()
    assertThat(savedReport.toDto().id).isEqualTo(savedReport.id)
  }

  @Test
  fun `report dto reflects whether the entity's source was NOMIS`() {
    val reportFromNomis = reportRepository.save(
      buildIncidentReport(
        incidentNumber = "1234",
        reportTime = now,
        source = InformationSource.NOMIS,
      ),
    )
    assertThat(reportFromNomis.toDto().createdInNomis).isTrue()

    val reportFromDps = reportRepository.save(
      buildIncidentReport(
        incidentNumber = "1235",
        reportTime = now,
        source = InformationSource.DPS,
      ),
    )
    assertThat(reportFromDps.toDto().createdInNomis).isFalse()
  }
}
