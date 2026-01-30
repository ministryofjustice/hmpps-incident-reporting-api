package uk.gov.justice.digital.hmpps.incidentreporting.config

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Configuration
class CustomisedJacksonObjectMapper(
  private val zoneId: ZoneId,
  @param:Value($$"${spring.jackson.date-format}") private val zonedDateTimeFormat: String,
) {
  @Bean
  fun javaTimeModule(): JavaTimeModule {
    val naiveDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(zoneId)
    val naiveDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(zoneId)
    val zonedDateTimeFormatter = DateTimeFormatter.ofPattern(zonedDateTimeFormat).withZone(zoneId)

    val module = JavaTimeModule()
    module.addSerializer(LocalDateSerializer(naiveDateFormatter))
    module.addSerializer(LocalDateTimeSerializer(naiveDateTimeFormatter))
    module.addSerializer(ZonedDateTimeSerializer(zonedDateTimeFormatter))
    return module
  }
}
