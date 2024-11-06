package uk.gov.justice.digital.hmpps.incidentreporting.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.CorrectionReason
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.integration.SqsIntegrationTestBase

@DisplayName("Constants resource")
class ConstantsResourceTest : SqsIntegrationTestBase() {
  @ParameterizedTest(name = "cannot access {0} constants without authorisation")
  @ValueSource(strings = ["error-codes", "correction-reasons", "information-sources", "prisoner-outcomes", "prisoner-roles", "staff-roles", "statuses", "types"])
  fun `cannot access without authorisation`(endpoint: String) {
    webTestClient.get().uri("/constants/$endpoint")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @ParameterizedTest(name = "can access {0} constants without special roles")
  @ValueSource(strings = ["error-codes", "correction-reasons", "information-sources", "prisoner-outcomes", "prisoner-roles", "staff-roles", "statuses", "types"])
  fun `can access without special roles`(endpoint: String) {
    webTestClient.get().uri("/constants/$endpoint")
      .headers(setAuthorisation(roles = emptyList(), scopes = listOf("read")))
      .header("Content-Type", "application/json")
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("$").value<List<Map<String, Any?>>> { list ->
        assertThat(list).hasSizeGreaterThan(0)
        assertThat(list).allSatisfy { constant ->
          assertThat(constant).containsKeys("code", "description")
        }
      }
  }

  companion object {
    @JvmStatic
    fun testCases() = listOf(
      arguments(
        "error-codes",
        ErrorCode.entries.size,
        arrayOf(
          mapOf("code" to "100", "description" to "ValidationFailure"),
        ),
      ),
      arguments(
        "correction-reasons",
        CorrectionReason.entries.size,
        arrayOf(
          mapOf("code" to "MISTAKE", "description" to "Mistake"),
        ),
      ),
      arguments(
        "information-sources",
        InformationSource.entries.size,
        arrayOf(
          mapOf("code" to "DPS", "description" to "DPS"),
        ),
      ),
      arguments(
        "prisoner-outcomes",
        PrisonerOutcome.entries.size,
        arrayOf(
          mapOf(
            "code" to "LOCAL_INVESTIGATION",
            "description" to "Investigation (local)",
            "nomisCode" to "ILOC",
          ),
        ),
      ),
      arguments(
        "prisoner-roles",
        PrisonerRole.entries.size,
        arrayOf(
          mapOf(
            "code" to "ABSCONDER",
            "description" to "Absconder",
            "nomisCode" to "ABS",
          ),
          mapOf(
            "code" to "IMPEDED_STAFF",
            "description" to "Impeded staff",
            "nomisCode" to "IMPED",
          ),
        ),
      ),
      arguments(
        "staff-roles",
        StaffRole.entries.size,
        arrayOf(
          mapOf(
            "code" to "ACTIVELY_INVOLVED",
            "description" to "Actively involved",
            "nomisCodes" to listOf("AI", "INV"),
          ),
        ),
      ),
      arguments(
        "statuses",
        Status.entries.size,
        arrayOf(
          mapOf(
            "code" to "DRAFT",
            "description" to "Draft",
            "nomisCode" to null,
          ),
          mapOf(
            "code" to "IN_ANALYSIS",
            "description" to "In analysis",
            "nomisCode" to "INAN",
          ),
        ),
      ),
      arguments(
        "types",
        Type.entries.size,
        arrayOf(
          mapOf(
            "code" to "DISORDER",
            "description" to "Disorder",
            "active" to true,
            "nomisCode" to "DISORDER1",
          ),
          mapOf(
            "code" to "OLD_ASSAULT2",
            "description" to "Assault (from April 2017)",
            "active" to false,
            "nomisCode" to "ASSAULTS1",
          ),
        ),
      ),
    )
  }

  @ParameterizedTest(name = "exposes {0} constants")
  @MethodSource("testCases")
  fun `exposes constants`(endpoint: String, expectedCount: Int, expectedSamples: Array<Map<String, Any?>>) {
    webTestClient.get().uri("/constants/$endpoint")
      .headers(setAuthorisation(roles = emptyList(), scopes = listOf("read")))
      .header("Content-Type", "application/json")
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("$").value<List<Map<String, Any?>>> { list ->
        assertThat(list).hasSize(expectedCount)
        assertThat(list).containsOnlyOnce(*expectedSamples)
      }
  }
}
