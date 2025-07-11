package uk.gov.justice.digital.hmpps.incidentreporting.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.incidentreporting.dto.prisonersearch.Prisoner

@DisplayName("Prisoner search service")
class PrisonerSearchServiceTest {
  private val mapper = ObjectMapper().findAndRegisterModules()

  private fun createWebClientMockResponses(vararg responses: List<Prisoner>): WebClient {
    val responseIterator = responses.iterator()
    return WebClient.builder()
      .exchangeFunction {
        val response = if (!responseIterator.hasNext()) {
          // returns 404 once `responses` runs out
          ClientResponse.create(HttpStatus.NOT_FOUND).build()
        } else {
          ClientResponse.create(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(mapper.writeValueAsString(responseIterator.next()))
            .build()
        }
        Mono.just(response)
      }
      .build()
  }

  @Test
  fun `returns an empty map when passed no prisoner numbers`() {
    val webClient = createWebClientMockResponses() // would throw 404 if ever called
    val prisonerSearchService = PrisonerSearchService(webClient, mapper)
    val prisoners = prisonerSearchService.searchByPrisonerNumbers(emptySet())
    assertThat(prisoners).isEmpty()
  }

  @Test
  fun `calls prisoner search once if passed up to 900 prisoner numbers`() {
    val mockPrisonerNumbers = (1..900).map {
      String.format("A%04dAA", it)
    }
    val mockPrisoners = mockPrisonerNumbers.map {
      Prisoner(
        prisonerNumber = it,
        firstName = "First name",
        lastName = "Surname",
      )
    }
    val webClient = createWebClientMockResponses(mockPrisoners)
    val prisonerSearchService = PrisonerSearchService(webClient, mapper)
    val prisoners = prisonerSearchService.searchByPrisonerNumbers(mockPrisonerNumbers.shuffled().toSet())
    assertThat(prisoners).hasSize(mockPrisonerNumbers.size)
  }

  @Test
  fun `calls prisoner search repeatedly in pages of 900`() {
    // generate 900 prisoners for page 1
    val response1 = (1..900).map {
      Prisoner(
        String.format("A%04dAA", it),
        firstName = "First name",
        lastName = "Surname",
      )
    }
    // generate 900 prisoners for page 2
    val response2 = (900..<1800).map {
      Prisoner(
        String.format("A%04dAA", it),
        firstName = "First name",
        lastName = "Surname",
      )
    }
    // generate 200 prisoners for page 3
    val response3 = (1800..2000).map {
      Prisoner(
        String.format("A%04dAA", it),
        firstName = "First name",
        lastName = "Surname",
      )
    }
    // all 2000 prisoner numbers
    val allPrisonerNumbers = (1..2000).map { String.format("A%04dAA", it) }.toSet()

    val webClient = createWebClientMockResponses(response1, response2, response3)
    val prisonerSearchService = PrisonerSearchService(webClient, mapper)
    val prisoners = prisonerSearchService.searchByPrisonerNumbers(allPrisonerNumbers)
    assertThat(prisoners).hasSize(2000)
  }

  @Test
  fun `throws an error if any prisoner numbers are not found`() {
    val webClient = createWebClientMockResponses(
      listOf(
        Prisoner(
          prisonerNumber = "A0001AA",
          firstName = "First name",
          lastName = "Surname",
        ),
      ),
    )
    val prisonerSearchService = PrisonerSearchService(webClient, mapper)
    assertThatThrownBy {
      prisonerSearchService.searchByPrisonerNumbers(setOf("A0001AA", "A0002AA"))
    }.isInstanceOf(PrisonersNotFoundException::class.java)
      .hasMessageContaining("A0002AA")
      .hasFieldOrPropertyWithValue("missingPrisonerNumbers", setOf("A0002AA"))
  }
}
