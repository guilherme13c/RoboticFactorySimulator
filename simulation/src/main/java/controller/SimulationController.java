package controller;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.CanvasPersistenceManager;
import model.Factory;

@RestController
@RequestMapping("/simulation")
public class SimulationController {
	private static Logger LOGGER = Logger.getLogger(SimulationController.class.getName());
	
    private Map<String, Factory> models;
    private CanvasPersistenceManager persistenceManager;    
       
    public SimulationController(CanvasPersistenceManager canvasPersistenceManager) {
    	persistenceManager = canvasPersistenceManager;
    	models = new ConcurrentHashMap<>();

    	LOGGER.info("SimulationController initialized.");
    }
    
    @PostMapping("/start/{factoryId}")
    public boolean startSimulation(@PathVariable String factoryId) {
        LOGGER.info("Request received to start simulation for factoryId: " + factoryId);
    	
    	Factory factory;
    	
        try {
			factory = (Factory) persistenceManager.read(factoryId);
		} catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load factory configuration for factoryId: " + factoryId, e);
			
			return false;
		}
        
        factory.startSimulation();
        LOGGER.info("Simulation started successfully for factoryId: " + factoryId);
        
		return true;
    }

    @GetMapping("/{factoryId}")
    public Factory getSimulatedModel(@PathVariable String factoryId) {
        LOGGER.info("Fetching simulation model status for factoryId: " + factoryId);
        Factory factoryModel = models.get(factoryId);

        if (factoryModel == null) {
            LOGGER.warning("No active simulation found for factoryId: " + factoryId);
            return null;
        }

        return factoryModel;
    }

    @DeleteMapping("/stop/{factoryId}")
    public boolean stopSimulation(@RequestParam String factoryId) {
        LOGGER.info("Request received to stop simulation for factoryId: " + factoryId);
        Factory factoryModel = models.remove(factoryId);

        if (factoryModel == null) {
            LOGGER.warning("Cannot stop simulation: No active model found for factoryId: " + factoryId);
            return false;
        }

        try {
            persistenceManager.persist((Canvas) factoryModel);
            LOGGER.info("Simulation stopped and state persisted for factoryId: " + factoryId);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to persist factory state after stopping for factoryId: " + factoryId, e);
        }

        return true;
    }
}
