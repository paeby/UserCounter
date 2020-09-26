package com.doodle.connectors.kafka

import com.doodle.config.UserCounterConfig
import com.doodle.containers.UserRecord
import com.ovoenergy.kafka.serialization.circe.circeJsonDeserializer
import com.ovoenergy.kafka.serialization.core.nullDeserializer
import java.util
import org.apache.kafka.clients.consumer.KafkaConsumer

import io.circe.generic.auto._

/**
  * Kafka Consumer of User frames deserialized in UserRecord
  * @param fromBeginning if set to true, will consume messages of the topic from the beginning
  */
class UserConsumer(fromBeginning: Boolean) {
  private val TOPIC_NAME: String = UserCounterConfig.USER_RECORD_TOPIC

  private val group: String = {
    if (fromBeginning) java.util.UUID.randomUUID().toString
    else "UserCounter"
  }

  val consumer: KafkaConsumer[Unit, UserRecord] =
    new KafkaConsumer(UserCounterConfig.KAFKA_CONSUMER_CONFIG(group), nullDeserializer[Unit], circeJsonDeserializer[UserRecord])

  consumer.subscribe(util.Collections.singletonList(TOPIC_NAME))
}
