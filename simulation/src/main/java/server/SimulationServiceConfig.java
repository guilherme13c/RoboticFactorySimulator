package server;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;

import model.Factory;
import simulation.SimulationServiceUtils;

@Configuration
public class SimulationServiceConfig {

    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    ProducerFactory<String, Factory> producerFactory() {
        final Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, SimulationServiceUtils.BOOTSTRAP_SERVERS);
        
        @SuppressWarnings("deprecation")
		final JsonSerializer<Factory> factorySerializer = new JsonSerializer<>(objectMapper);
        
        return new DefaultKafkaProducerFactory<>(
                config,
                new StringSerializer(),
                factorySerializer
        );
    }

    @Bean
    @Primary
    KafkaTemplate<String, Factory> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}