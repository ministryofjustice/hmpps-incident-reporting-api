package uk.gov.justice.digital.hmpps.incidentreporting.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.constants.TypeFamily
import uk.gov.justice.digital.hmpps.incidentreporting.constants.UserAction
import uk.gov.justice.digital.hmpps.incidentreporting.constants.UserType
import uk.gov.justice.digital.hmpps.incidentreporting.integration.SqsIntegrationTestBase

@DisplayName("Constants resource")
class ConstantsResourceTest : SqsIntegrationTestBase() {
  @ParameterizedTest(name = "cannot access {0} constants without authorisation")
  @ValueSource(
    strings = [
      "error-codes", "information-sources", "prisoner-outcomes", "prisoner-roles", "staff-roles", "statuses", "types",
      "user-actions", "user-types",
    ],
  )
  fun `cannot access without authorisation`(endpoint: String) {
    webTestClient.get().uri("/constants/$endpoint")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @ParameterizedTest(name = "can access {0} constants without special roles")
  @ValueSource(
    strings = [
      "error-codes", "information-sources", "prisoner-outcomes", "prisoner-roles", "staff-roles", "statuses", "types",
      "user-actions", "user-types",
    ],
  )
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

  private data class ConstantsTestCase(
    val endpoint: String,
    val expectedCount: Int,
    val expectedSamples: List<Map<String, Any?>>,
  )

  @DisplayName("exposes constants")
  @TestFactory
  fun `exposes constants`(): List<DynamicTest> = listOf(
    ConstantsTestCase(
      "error-codes",
      ErrorCode.entries.size,
      listOf(
        mapOf("code" to "100", "description" to "ValidationFailure"),
      ),
    ),
    ConstantsTestCase(
      "information-sources",
      InformationSource.entries.size,
      listOf(
        mapOf("code" to "DPS", "description" to "DPS"),
      ),
    ),
    ConstantsTestCase(
      "prisoner-outcomes",
      PrisonerOutcome.entries.size,
      listOf(
        mapOf(
          "code" to "LOCAL_INVESTIGATION",
          "description" to "Investigation (local)",
          "nomisCode" to "ILOC",
        ),
      ),
    ),
    ConstantsTestCase(
      "prisoner-roles",
      PrisonerRole.entries.size,
      listOf(
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
    ConstantsTestCase(
      "staff-roles",
      StaffRole.entries.size,
      listOf(
        mapOf(
          "code" to "ACTIVELY_INVOLVED",
          "description" to "Actively involved",
          "nomisCodes" to listOf("AI", "INV"),
        ),
      ),
    ),
    ConstantsTestCase(
      "statuses",
      Status.entries.size,
      listOf(
        mapOf(
          "code" to "DRAFT",
          "description" to "Draft",
          "nomisCode" to null,
          "ignoreDownstream" to false,
        ),
        mapOf(
          "code" to "ON_HOLD",
          "description" to "On hold",
          "nomisCode" to "INAN",
          "ignoreDownstream" to false,
        ),
      ),
    ),
    ConstantsTestCase(
      "types",
      Type.entries.size,
      listOf(
        mapOf(
          "familyCode" to "DISORDER",
          "code" to "DISORDER_2",
          "description" to "Disorder",
          "active" to true,
          "nomisCode" to "DISORDER1",
        ),
        mapOf(
          "familyCode" to "ASSAULT",
          "code" to "ASSAULT_3",
          "description" to "Assault",
          "active" to false,
          "nomisCode" to "ASSAULTS1",
        ),
      ),
    ),
    ConstantsTestCase(
      "type-families",
      TypeFamily.entries.size,
      listOf(
        mapOf(
          "code" to "DISORDER",
          "description" to "Disorder",
        ),
        mapOf(
          "code" to "KEY_OR_LOCK",
          "description" to "Key or lock compromise",
        ),
      ),
    ),
    ConstantsTestCase(
      "user-actions",
      UserAction.entries.size,
      listOf(
        mapOf(
          "code" to "REQUEST_DUPLICATE",
          "description" to "Request to mark duplicate",
        ),
        mapOf(
          "code" to "CLOSE",
          "description" to "Close",
        ),
      ),
    ),
    ConstantsTestCase(
      "user-types",
      UserType.entries.size,
      listOf(
        mapOf(
          "code" to "REPORTING_OFFICER",
          "description" to "Reporting officer",
        ),
        mapOf(
          "code" to "DATA_WARDEN",
          "description" to "Data warden",
        ),
      ),
    ),
  ).map { (endpoint, expectedCount, expectedSamples) ->
    DynamicTest.dynamicTest("exposes $endpoint constants") {
      webTestClient.get().uri("/constants/$endpoint")
        .headers(setAuthorisation(roles = emptyList(), scopes = listOf("read")))
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus().isOk
        .expectBody().jsonPath("$").value<List<Map<String, Any?>>> { list ->
          assertThat(list).hasSize(expectedCount)
          assertThat(list).containsOnlyOnce(*expectedSamples.toTypedArray())
        }
    }
  }
}
