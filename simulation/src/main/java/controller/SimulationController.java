package controller;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private Map<String, Factory> models;
    private CanvasPersistenceManager persistenceManager;
       
    public SimulationController(CanvasPersistenceManager canvasPersistenceManager) {
    	persistenceManager = canvasPersistenceManager;
    	models = new ConcurrentHashMap<>();
    }
    
    @PostMapping("/start/{factoryId}")
    public boolean startSimulation(@PathVariable String factoryId) {
    	Factory factory;
    	
        try {
			factory = (Factory) persistenceManager.read(factoryId);
		} catch (IOException e) {
			e.printStackTrace();
			
			return false;
		}
        
        factory.startSimulation();
        
		return true;
    }

    @GetMapping("/{factoryId}")
    public Factory getSimulatedModel(@PathVariable String factoryId) {        
        Factory factoryModel = models.get(factoryId);

        if (factoryModel == null) {
        	return null;
        }

        return factoryModel;
    }

    @DeleteMapping("/stop/{factoryId}")
    public boolean stopSimulation(@RequestParam String factoryId) {
        Factory factoryModel = models.remove(factoryId);
        
        if (factoryModel == null) {
        	return false;
        }
        
        try {
			persistenceManager.persist((Canvas) factoryModel);
		} catch (IOException e) {
			e.printStackTrace();
		}

        return true;
    }
}
