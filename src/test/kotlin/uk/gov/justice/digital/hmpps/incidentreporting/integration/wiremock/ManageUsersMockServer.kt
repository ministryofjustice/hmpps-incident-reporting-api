package uk.gov.justice.digital.hmpps.incidentreporting.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.get

const val MANAGE_USERS_WIREMOCK_PORT = 8082

class ManageUsersMockServer : MockServer(MANAGE_USERS_WIREMOCK_PORT)
