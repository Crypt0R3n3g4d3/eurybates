package com.socrata
package eurybates.kafka

import com.rojoma.json.v3.util.JsonUtil
import util.logging.LazyStringLogger
import java.util.Properties
import com.socrata.eurybates._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, ProducerConfig}


object KafkaServiceProducer {
  def fromProperties(sourceId: String, properties: Properties) : Producer = {
    properties.getProperty(Producer.KafkaProducerType + "." + "broker_list") match {
      case brokerList: String => new KafkaServiceProducer(brokerList, sourceId)
      case _ => throw new IllegalStateException("No configuration passed for Kafka")
    }
  }
}

/** Submit Messages to a Kafka-based queue
  *
  * @param brokerList Comma-separated list of host:port pairs representing Kafka brokers
  * @param sourceId String representation of what created this. Auto-populated in messages.
  * @param encodePrettily Pretty-print JSON
  */
class KafkaServiceProducer(brokerList: String, sourceId:String, encodePrettily: Boolean = true) extends MessageCodec(sourceId) with Producer with QueueUtil {
  val log = new LazyStringLogger(getClass)
  var producer:KafkaProducer[String,  String] = null

  def start() = synchronized {
    val props = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList)
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")

    producer = new KafkaProducer(props)
  }

  def stop() = synchronized {
    producer.close()
  }

  def apply(message: eurybates.Message) {
    val queueName = Name
    val encodedMessage = JsonUtil.renderJson(message, pretty = encodePrettily)

    log.info("Sending " + message + " on queue " + queueName + "with tag " + message.tag)
    producer.send(new ProducerRecord[String, String](queueName, encodedMessage))
  }
}