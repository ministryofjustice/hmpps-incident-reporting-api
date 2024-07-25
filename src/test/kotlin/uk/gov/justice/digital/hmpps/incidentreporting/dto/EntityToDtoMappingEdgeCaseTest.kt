package uk.gov.justice.digital.hmpps.incidentreporting.dto

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.util.JsonExpectationsHelper
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.helper.buildEvent
import uk.gov.justice.digital.hmpps.incidentreporting.helper.buildReport
import uk.gov.justice.digital.hmpps.incidentreporting.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.EventRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository

/**
 * Tests for edge cases when converting JPA entities to DTO classes.
 * Largely, these mappings are field-by-field copies.
 * NB: most conversions are already covered by resource and service tests.
 */
@DisplayName("Mapping JPA entities to DTOs")
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
    val unsavedReport = buildReport(
      reportReference = "1234",
      reportTime = now,
    )
    assertThat(unsavedReport.id).isNull()
    assertThatThrownBy { unsavedReport.toDtoBasic() }
      .isInstanceOf(NullPointerException::class.java)
    assertThatThrownBy { unsavedReport.toDtoWithDetails() }
      .isInstanceOf(NullPointerException::class.java)

    val savedReport = reportRepository.save(unsavedReport)
    assertThat(savedReport.id).isNotNull()
    assertThat(savedReport.toDtoBasic().id).isEqualTo(savedReport.id)
    assertThat(savedReport.toDtoWithDetails().id).isEqualTo(savedReport.id)
  }

  @Test
  fun `event must have a non-null id to map to the dto`() {
    val unsavedEvent = buildEvent(
      eventReference = "1234",
      eventDateAndTime = now.minusHours(2),
      reportDateAndTime = now,
    )
    assertThat(unsavedEvent.id).isNull()
    assertThatThrownBy { unsavedEvent.toDto() }
      .isInstanceOf(NullPointerException::class.java)

    val savedEvent = eventRepository.save(unsavedEvent)
    assertThat(savedEvent.id).isNotNull()
    assertThat(savedEvent.toDto().id).isEqualTo(savedEvent.id)
  }

  @Test
  fun `report dto reflects whether the entity's source was NOMIS`() {
    val reportFromNomis = reportRepository.save(
      buildReport(
        reportReference = "1234",
        reportTime = now,
        source = InformationSource.NOMIS,
      ),
    )
    assertThat(reportFromNomis.toDtoBasic().createdInNomis).isTrue()
    assertThat(reportFromNomis.toDtoWithDetails().createdInNomis).isTrue()

    val reportFromDps = reportRepository.save(
      buildReport(
        reportReference = "1235",
        reportTime = now,
        source = InformationSource.DPS,
      ),
    )
    assertThat(reportFromDps.toDtoBasic().createdInNomis).isFalse()
    assertThat(reportFromDps.toDtoWithDetails().createdInNomis).isFalse()
  }

  @DisplayName("serialisation to JSON")
  @Nested
  inner class Serialisation {
    private lateinit var report: Report

    @BeforeEach
    fun setUp() {
      report = reportRepository.save(
        buildReport(
          reportReference = "IR-0000000001124143",
          reportTime = now,
          source = InformationSource.DPS,
          generateStaffInvolvement = 2,
          generatePrisonerInvolvement = 2,
          generateLocations = 1,
          generateCorrections = 1,
          generateQuestions = 3,
          generateResponses = 2,
          generateHistory = 2,
        ),
      )
    }

    @Test
    fun `can serialise basic report`() {
      val expectedJson = getResource("/entity-mapping/sample-report-basic.json")
      val json = report.toDtoBasic().toJson()
      JsonExpectationsHelper().assertJsonEqual(expectedJson, json, false)
    }

    @Test
    fun `can serialise report with all related details`() {
      val expectedJson = getResource("/entity-mapping/sample-report-with-details.json")
      val json = report.toDtoWithDetails().toJson()
      JsonExpectationsHelper().assertJsonEqual(expectedJson, json, false)
    }
  }
}
