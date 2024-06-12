package uk.gov.justice.digital.hmpps.incidentreporting.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.incidentreporting.integration.SqsIntegrationTestBase

@DisplayName("Constants resource")
class ConstantsResourceTest : SqsIntegrationTestBase() {
  @ParameterizedTest(name = "cannot access {0} constants without authorisation")
  @ValueSource(strings = ["prisoner-outcomes", "prisoner-roles", "staff-roles", "statuses", "types"])
  fun `cannot access without authorisation`(endpoint: String) {
    webTestClient.get().uri("/constants/$endpoint")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @ParameterizedTest(name = "can access {0} constants without special roles")
  @ValueSource(strings = ["prisoner-outcomes", "prisoner-roles", "staff-roles", "statuses", "types"])
  fun `can access without special roles`(endpoint: String) {
    webTestClient.get().uri("/constants/$endpoint")
      .headers(setAuthorisation(roles = emptyList(), scopes = listOf("write")))
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
}
