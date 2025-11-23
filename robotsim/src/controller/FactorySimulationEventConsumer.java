package controller;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import simulation.SimulationServiceUtils;

public class FactorySimulationEventConsumer {
    private static final Logger LOGGER = Logger.getLogger(FactorySimulationEventConsumer.class.getName());

    private final KafkaConsumer<String, String> consumer;
    private final RemoteSimulationController controller;

    public FactorySimulationEventConsumer(final RemoteSimulationController controller) {
        this.controller = controller;
        
        final Properties props = SimulationServiceUtils.getDefaultConsumerProperties();
        
        this.consumer = new KafkaConsumer<>(props);
        
        final String topicName = SimulationServiceUtils.getTopicName(controller.getFactoryId());
        this.consumer.subscribe(Collections.singletonList(topicName));
    }

    public void consumeMessages() {
        try {
            while (controller.isAnimationRunning()) {
                final ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                
                if (!records.isEmpty()) {
                    LOGGER.info("Received " + records.count() + " records from Kafka.");
                    for (final ConsumerRecord<String, String> record : records) {
                        controller.updateCanvasFromJSON(record.value());
                    }
                }
                
                for (final ConsumerRecord<String, String> record : records) {
                    controller.updateCanvasFromJSON(record.value());
                }
            }
        } finally {
            consumer.close();
        }
    }
}