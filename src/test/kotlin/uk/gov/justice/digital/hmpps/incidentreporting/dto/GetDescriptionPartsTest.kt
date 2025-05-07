package uk.gov.justice.digital.hmpps.incidentreporting.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisCode
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisReport
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisStaff
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisStatus
import uk.gov.justice.digital.hmpps.incidentreporting.integration.IntegrationTestBase.Companion.now
import java.time.LocalDateTime

@DisplayName("Get description parts")
class GetDescriptionPartsTest {

  @DisplayName("works")
  @Nested
  inner class HappyPath {
    @Test
    fun `valid description entry split into base description and an addendum`() {
      val minimalReportDto = createBasicReport(
        "Original description" +
          "User:STARK,TONY Date:07-JUN-2024 12:13Some updated details",
      )

      val result = minimalReportDto.getDescriptionParts()

      val expected: Pair<String?, List<DescriptionAddendum>> = Pair(
        "Original description",
        listOf(
          DescriptionAddendum(
            createdBy = "INCIDENT_REPORTING_API",
            firstName = "TONY",
            lastName = "STARK",
            createdAt = LocalDateTime.parse("2024-06-07T12:13"),
            text = "Some updated details",
            sequence = 0,
          ),
        ),
      )

      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `valid description entry split into base description and multiple addendums`() {
      val minimalReportDto = createBasicReport(
        "Original description" +
          "User:STARK,TONY Date:07-JUN-2024 12:13Some updated details" +
          "User:ROGERS,STEVE Date:08-JUN-2024 15:58Second lot of updated details" +
          "User:BANNER,BRUCE Date:11-JUN-2024 08:42Third lot of updated details",
      )

      val result = minimalReportDto.getDescriptionParts()

      val expected: Pair<String?, List<DescriptionAddendum>> = Pair(
        "Original description",
        listOf(
          DescriptionAddendum(
            createdBy = "INCIDENT_REPORTING_API",
            firstName = "TONY",
            lastName = "STARK",
            createdAt = LocalDateTime.parse("2024-06-07T12:13"),
            text = "Some updated details",
            sequence = 0,
          ),
          DescriptionAddendum(
            createdBy = "INCIDENT_REPORTING_API",
            firstName = "STEVE",
            lastName = "ROGERS",
            createdAt = LocalDateTime.parse("2024-06-08T15:58"),
            text = "Second lot of updated details",
            sequence = 1,
          ),
          DescriptionAddendum(
            createdBy = "INCIDENT_REPORTING_API",
            firstName = "BRUCE",
            lastName = "BANNER",
            createdAt = LocalDateTime.parse("2024-06-11T08:42"),
            text = "Third lot of updated details",
            sequence = 2,
          ),
        ),
      )

      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `description without any addendums`() {
      val minimalReportDto = createBasicReport("Some details about an incident")

      val result = minimalReportDto.getDescriptionParts()

      val expected: Pair<String?, List<DescriptionAddendum>> = Pair("Some details about an incident", emptyList())

      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `can accept null description`() {
      val nomisReport = createBasicReport(null)
      val (description, addenda) = nomisReport.getDescriptionParts()
      assertThat(description).isNull()
      assertThat(addenda).isEmpty()
    }

    @Test
    fun `valid description with different date format`() {
      val minimalReportDto = createBasicReport(
        "Original description" +
          "User:STARK,TONY Date:07/06/2024 12:13Some updated details",
      )

      val result = minimalReportDto.getDescriptionParts()

      val expected: Pair<String?, List<DescriptionAddendum>> = Pair(
        "Original description",
        listOf(
          DescriptionAddendum(
            createdBy = "INCIDENT_REPORTING_API",
            firstName = "TONY",
            lastName = "STARK",
            createdAt = LocalDateTime.parse("2024-06-07T12:13"),
            text = "Some updated details",
            sequence = 0,
          ),
        ),
      )

      assertThat(result).isEqualTo(expected)
    }
  }

  @DisplayName("handles errors")
  @Nested
  inner class Errors {
    @Test
    fun `exception thrown when no valid date found in addendum`() {
      val testDescription = "Original description" +
        "User:STARK,TONY Date:Some updated details"
      val minimalReportDto = createBasicReport(testDescription)

      val result = minimalReportDto.getDescriptionParts()

      val expected: Pair<String?, List<DescriptionAddendum>> = Pair(testDescription, emptyList())

      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `exception thrown when addendums does not contain expected pattern`() {
      val testDescription = "Original description" +
        "User:STARK,TONYSome updated details"
      val minimalReportDto = createBasicReport(testDescription)

      val result = minimalReportDto.getDescriptionParts()

      val expected: Pair<String?, List<DescriptionAddendum>> = Pair(testDescription, emptyList())

      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `exception thrown when name cannot be extracted from addendum`() {
      val testDescription = "Original description" +
        "User:STARK TONY Date:07-JUN-2024 12:13Some updated details"
      val minimalReportDto = createBasicReport(testDescription)

      val result = minimalReportDto.getDescriptionParts()

      val expected: Pair<String?, List<DescriptionAddendum>> = Pair(testDescription, emptyList())

      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `exception thrown when text cannot be extracted from addendum`() {
      val testDescription = "Original description" +
        "User:STARK,TONY Date: 07-JUN-2024 12:13Some updated details"
      val minimalReportDto = createBasicReport(testDescription)

      val result = minimalReportDto.getDescriptionParts()

      val expected: Pair<String?, List<DescriptionAddendum>> = Pair(testDescription, emptyList())

      assertThat(result).isEqualTo(expected)
    }
  }

  private fun createBasicReport(description: String?) = NomisReport(
    incidentId = 112414323,
    questionnaireId = 2124,
    title = "TITLE",
    description = description,
    prison = NomisCode("MDI", "Moorland (HMP)"),
    status = NomisStatus("AWAN", "Awaiting Analysis"),
    type = "FINDS1",
    lockedResponse = false,
    incidentDateTime = now.minusDays(1),
    reportingStaff = NomisStaff("user1", 121, "John", "Smith"),
    reportedDateTime = now.minusMinutes(5),
    createDateTime = now.minusMinutes(4),
    createdBy = "user1",
    staffParties = emptyList(),
    offenderParties = emptyList(),
    requirements = emptyList(),
    questions = emptyList(),
    history = emptyList(),
  )
}
