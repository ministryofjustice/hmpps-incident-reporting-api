package uk.gov.justice.digital.hmpps.incidentreporting.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole
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

  @Test
  fun `exposes NOMIS incident types`() {
    webTestClient.get().uri("/constants/types")
      .headers(setAuthorisation(roles = emptyList(), scopes = listOf("read")))
      .header("Content-Type", "application/json")
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("$").value<List<Map<String, Any?>>> { list ->
        assertThat(list).hasSize(Type.entries.size)
        assertThat(list).containsOnlyOnce(
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
        )
      }
  }

  @Test
  fun `exposes NOMIS prisoner roles codes`() {
    webTestClient.get().uri("/constants/prisoner-roles")
      .headers(setAuthorisation(roles = emptyList(), scopes = listOf("read")))
      .header("Content-Type", "application/json")
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("$").value<List<Map<String, Any?>>> { list ->
        assertThat(list).hasSize(PrisonerRole.entries.size)
        assertThat(list).containsOnlyOnce(
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
        )
      }
  }
}
