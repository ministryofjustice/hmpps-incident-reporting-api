package uk.gov.justice.digital.hmpps.incidentreporting.service

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.incidentreporting.dto.prisonersearch.Prisoner

@Service
class PrisonerSearchService(
  private val prisonerSearchWebClient: WebClient,
) {
  /**
   * Search for prisoners by their prisoner number
   *
   * Requires ROLE_GLOBAL_SEARCH or ROLE_PRISONER_SEARCH role.
   */
  fun searchByPrisonerNumbers(prisonerNumbers: Set<String>): Map<String, Prisoner> {
    if (prisonerNumbers.isEmpty()) {
      return emptyMap()
    }

    val requests = prisonerNumbers
      .chunked(900)
      .map { pageOfPrisonerNumbers ->
        val requestBody = mapOf("prisonerNumbers" to pageOfPrisonerNumbers)
        prisonerSearchWebClient
          .post()
          .uri("/prisoner-search/prisoner-numbers")
          .header("Content-Type", "application/json")
          .bodyValue(requestBody)
          .retrieve()
          .bodyToMono<List<Prisoner>>()
      }
    val foundPrisoners = Flux.merge(requests)
      .collectList()
      .block()!!
      .flatten()
      .associateBy(Prisoner::prisonerNumber)

    // error if *any* prisoner numbers were not found
    val missingPrisoners = prisonerNumbers.subtract(foundPrisoners.keys)
    if (missingPrisoners.any()) {
      throw PrisonersNotFoundException(missingPrisoners)
    }

    return foundPrisoners
  }
}

class PrisonersNotFoundException(
  @Suppress("unused")
  val missingPrisonerNumbers: Set<String>,
) : Exception("Prisoner numbers not found: ${missingPrisonerNumbers.joinToString()}")
