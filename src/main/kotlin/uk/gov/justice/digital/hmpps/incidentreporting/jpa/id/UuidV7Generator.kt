package uk.gov.justice.digital.hmpps.incidentreporting.jpa.id

import com.github.f4b6a3.uuid.UuidCreator
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.generator.BeforeExecutionGenerator
import org.hibernate.generator.EventType
import org.hibernate.generator.EventTypeSets.INSERT_ONLY
import java.util.EnumSet
import java.util.UUID

class UuidV7Generator : BeforeExecutionGenerator {
  override fun getEventTypes(): EnumSet<EventType> {
    return INSERT_ONLY
  }

  override fun generate(
    session: SharedSessionContractImplementor?,
    owner: Any?,
    currentValue: Any?,
    eventType: EventType?,
  ): UUID {
    // NB: the default `org.hibernate.id.uuid.UuidGenerator` ignores session, owner, currentValue and eventType
    return UuidCreator.getTimeOrderedEpoch()
  }
}
