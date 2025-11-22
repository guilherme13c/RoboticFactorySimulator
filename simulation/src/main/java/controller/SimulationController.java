package controller;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.tp.inf112.projects.canvas.model.CanvasPersistenceManager;
import model.Factory;
import model.FactoryModelChangedNotifier;
import server.KafkaFactoryModelChangeNotifier;

@RestController
public class SimulationController {
	private static Logger LOGGER = Logger.getLogger(SimulationController.class.getName());

	private Map<String, Factory> models;
	private CanvasPersistenceManager persistenceManager;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@Autowired
    private KafkaTemplate<String, Factory> simulationEventTemplate;

	public SimulationController(CanvasPersistenceManager canvasPersistenceManager) {
		persistenceManager = canvasPersistenceManager;
		models = new ConcurrentHashMap<>();

		LOGGER.info("SimulationController initialized.");
	}

	@PostMapping("/start")
	public boolean startSimulation(@RequestParam String factoryId) {
		LOGGER.info("Request received to start simulation for factoryId: " + factoryId);

		Factory factory;
		try {
			factory = (Factory) persistenceManager.read(factoryId);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Failed to load factory configuration for factoryId: " + factoryId, e);
			return false;
		}
		
		FactoryModelChangedNotifier notifier = new KafkaFactoryModelChangeNotifier(factory, simulationEventTemplate);
        factory.setNotifier(notifier);

		factory.startSimulation();
		models.put(factoryId, factory);

		LOGGER.info("Simulation started successfully for factoryId: " + factoryId);

		return true;
	}

	@GetMapping("/")
	public Factory getSimulatedModel(@RequestParam String factoryId) {
		Factory factoryModel = models.get(factoryId);

		if (factoryModel == null) {
			LOGGER.warning("No active simulation found for factoryId: " + factoryId);
			return null;
		}

		try {
            String jsonDebug = objectMapper.writeValueAsString(factoryModel);
            LOGGER.info("DEBUG SERVER JSON: " + jsonDebug);
        } catch (Exception e) {
            LOGGER.severe("Could not serialize for debug: " + e.getMessage());
        }
		
		LOGGER.info(factoryModel.toString());

		return factoryModel;
	}

	@PostMapping("/stop")
	public boolean stopSimulation(@RequestParam String factoryId) {
		LOGGER.info("Request received to stop simulation for factoryId: " + factoryId);
		Factory factoryModel = models.remove(factoryId);

		if (factoryModel == null) {
			LOGGER.warning("Cannot stop simulation: No active model found for factoryId: " + factoryId);
			return false;
		}

		factoryModel.stopSimulation();
		
		factoryModel.setNotifier(null);

		return true;
	}

	@GetMapping("health")
	public boolean health() {
		return true;
	}

}
