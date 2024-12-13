package uk.gov.justice.digital.hmpps.incidentreporting.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.justice.digital.hmpps.incidentreporting.dto.prisonersearch.Prisoner

const val PRISONER_SEARCH_WIREMOCK_PORT = 8081

class PrisonerSearchMockServer : MockServer(PRISONER_SEARCH_WIREMOCK_PORT) {
  fun stubSearchByPrisonerNumbers(prisoners: List<Prisoner>): StubMapping {
    val requestBody = mapper.writeValueAsString(
      mapOf(
        "prisonerNumbers" to prisoners.map { it.prisonerNumber },
      ),
    )
    return stubFor(
      post("/prisoner-search/prisoner-numbers")
        .withRequestBody(
          WireMock.equalToJson(requestBody, true, false),
        )
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsBytes(prisoners)),
        ),
    )
  }

  fun stubSearchFails(): StubMapping {
    return stubFor(
      post("/prisoner-search/prisoner-numbers")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(500),
        ),
    )
  }
}
