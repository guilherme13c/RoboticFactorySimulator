package server;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import fr.tp.inf112.projects.canvas.controller.Observer;
import model.Factory;
import model.FactoryModelChangedNotifier;
import simulation.SimulationServiceUtils;

public class KafkaFactoryModelChangeNotifier implements FactoryModelChangedNotifier {
    private static final Logger LOGGER = Logger.getLogger(KafkaFactoryModelChangeNotifier.class.getName());
    
    private final Factory factoryModel;
    private final KafkaTemplate<String, Factory> simulationEventTemplate;

    public KafkaFactoryModelChangeNotifier(Factory factoryModel, KafkaTemplate<String, Factory> simulationEventTemplate) {
        this.factoryModel = factoryModel;
        this.simulationEventTemplate = simulationEventTemplate;
    }

    @Override
    public void notifyObservers() {
        String topicName = SimulationServiceUtils.getTopicName(factoryModel);
        
        final Message<Factory> factoryMessage = MessageBuilder
                .withPayload(factoryModel)
                .setHeader(KafkaHeaders.TOPIC, topicName)
                .build();

        final CompletableFuture<SendResult<String, Factory>> sendResult = simulationEventTemplate.send(factoryMessage);

        sendResult.whenComplete((result, ex) -> {
            if (ex != null) {
                LOGGER.log(Level.SEVERE, "Unable to send message=[" + factoryModel + "] due to : " + ex.getMessage());
            } else {
                 LOGGER.info("Sent message=[" + factoryModel + "] with offset=[" + result.getRecordMetadata().offset() + "]");
            }
        });
    }

    @Override
    public boolean addObserver(Observer observer) {
        return false;
    }

    @Override
    public boolean removeObserver(Observer observer) {
        return false;
    }
}