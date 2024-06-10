package uk.gov.justice.digital.hmpps.incidentreporting.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.ZoneId

@Configuration
class TimeZoneConfiguration(
  @Value("\${spring.jackson.time-zone}") private val timeZone: String,
) {
  @Bean
  fun timeZone(): ZoneId = ZoneId.of(timeZone)
}
