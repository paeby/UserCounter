package com.doodle.connectors.kafka
import com.doodle.config.UserCounterConfig
import com.ovoenergy.kafka.serialization.core.nullDeserializer
import com.ovoenergy.kafka.serialization.core.deserializer
import java.util
import org.apache.kafka.clients.consumer.KafkaConsumer

/**
  * Kafka Consumer of User frames reading strings
  * @param fromBeginning if set to true, will consume messages of the topic from the beginning
  */
class UserConsumerString(fromBeginning: Boolean) {
  private val TOPIC_NAME: String = UserCounterConfig.USER_RECORD_TOPIC

  private val group: String = {
    if (fromBeginning) java.util.UUID.randomUUID().toString
    else "UserCounter"
  }

  val consumer: KafkaConsumer[Unit, String] =
    new KafkaConsumer(UserCounterConfig.KAFKA_CONSUMER_CONFIG(group), nullDeserializer[Unit], deserializer(b => b.toString))

  consumer.subscribe(util.Collections.singletonList(TOPIC_NAME))
}
