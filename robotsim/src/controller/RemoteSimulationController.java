package controller;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

import fr.tp.inf112.projects.canvas.controller.Observer;
import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.CanvasPersistenceManager;
import fr.tp.inf112.projects.canvas.model.impl.BasicVertex;
import model.*;
import model.shapes.*;

public class RemoteSimulationController extends SimulatorController {
private static final Logger LOGGER = Logger.getLogger(RemoteSimulationController.class.getName());
    
    private final String simulationServiceUrl;
    private final String factoryId;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private volatile boolean simulationRunning;

    public RemoteSimulationController(Factory factoryModel, CanvasPersistenceManager persistenceManager, String simulationServiceUrl) {
        super(factoryModel, persistenceManager);
        this.factoryId = factoryModel.getName();
        this.simulationServiceUrl = simulationServiceUrl;
        this.httpClient = HttpClient.newBuilder()
        		.version(HttpClient.Version.HTTP_1_1)
        		.build();
        this.objectMapper = createObjectMapper();
        this.simulationRunning = false;
    }

    private ObjectMapper createObjectMapper() {
        final PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(PositionedShape.class.getPackageName())
                .allowIfSubType(Component.class.getPackageName())
                .allowIfSubType(BasicVertex.class.getPackageName())
                .allowIfSubType(ArrayList.class.getName())
                .allowIfSubType(LinkedHashSet.class.getName())
                .build();

        final ObjectMapper mapper = new ObjectMapper();
        mapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL);
        return mapper;
    }

    private String getEncodedFactoryId() {
        return URLEncoder.encode(factoryId, StandardCharsets.UTF_8);
    }

    @Override
    public void startAnimation() {
        try {
            String url = String.format("%s/%s/start", simulationServiceUrl, getEncodedFactoryId());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                simulationRunning = true;
                // 2. Start the viewer update loop in a separate thread
                new Thread(this::updateViewer).start();
            } else {
                LOGGER.log(Level.SEVERE, "Failed to start simulation: " + response.body());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error starting remote simulation", e);
        }
    }

    @Override
    public void stopAnimation() {
        try {
            simulationRunning = false;
            String url = String.format("%s/%s/stop", simulationServiceUrl, getEncodedFactoryId());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error stopping remote simulation", e);
        }
    }

    @Override
    public boolean isAnimationRunning() {
        return simulationRunning;
    }

    private void updateViewer() {
        while (simulationRunning) {
            try {
                String url = String.format("%s/%s", simulationServiceUrl, getEncodedFactoryId());
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    Factory remoteFactory = objectMapper.readValue(response.body(), Factory.class);
                    setCanvas(remoteFactory);
                }
                
                Thread.sleep(30); // Refresh rate
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error updating viewer", e);
            }
        }
    }

    @Override
    public void setCanvas(final Canvas canvasModel) {
        Canvas oldCanvas = getCanvas();
        final List<Observer> observers = ((Factory)oldCanvas).getObservers(); 
        
        super.setCanvas(canvasModel);
        
        for (final Observer observer : observers) {
            ((Factory)getCanvas()).addObserver(observer);
        }
        
        ((Factory)getCanvas()).notifyObservers();
    }
}
