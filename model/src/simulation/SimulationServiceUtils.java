package simulation;

import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import model.Factory;

public class SimulationServiceUtils {
	public static final String BOOTSTRAP_SERVERS = "localhost:9092";
	public static final String GROUP_ID = "Factory-Simulation-Group";
	public static final String AUTO_OFFSET_RESET = "earliest";
	private static final String TOPIC_PREFIX = "simulation-topic-";

	private static String sanitize(String input) {
        return input.replaceAll("[^a-zA-Z0-9._-]", "-");
    }
	
	public static String getTopicName(final Factory factoryModel) {
        return TOPIC_PREFIX + sanitize(factoryModel.getId());
    }

	public static String getTopicName(final String factoryId) {
		return TOPIC_PREFIX + sanitize(factoryId);
	}

	public static Properties getDefaultConsumerProperties() {
		final Properties props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, AUTO_OFFSET_RESET);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		return props;
	}
}