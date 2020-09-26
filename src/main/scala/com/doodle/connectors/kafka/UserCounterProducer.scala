package com.doodle.connectors.kafka

import com.doodle.config.UserCounterConfig
import com.doodle.containers.UserCountRecord
import com.ovoenergy.kafka.serialization.circe.circeJsonSerializer
import com.ovoenergy.kafka.serialization.core.nullSerializer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import io.circe.generic.auto._

/**
  * Kafka Producer of UserCountRecord
  */
class UserCounterProducer {
  private val TOPIC: String = UserCounterConfig.USER_COUNTER_TOPIC

  /**
    * Writes to User Counter Topic the given UserCountRecord
    * @param userCounter UserCountRecord to write to the Kafka topic
    */
  def writeToKafka(userCounter: UserCountRecord): Unit = {
    val producer: KafkaProducer[Unit, UserCountRecord] = new KafkaProducer[Unit, UserCountRecord](
      UserCounterConfig.KAFKA_PRODUCER_CONFIG,
      nullSerializer[Unit],
      circeJsonSerializer[UserCountRecord]
    )

    val record: ProducerRecord[Unit, UserCountRecord] = new ProducerRecord[Unit, UserCountRecord](TOPIC, Unit, userCounter)
    producer.send(record)

    producer.close()
  }

}
