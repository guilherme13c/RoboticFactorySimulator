package controller;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.CanvasPersistenceManager;
import fr.tp.inf112.projects.canvas.model.Vertex;
import fr.tp.inf112.projects.canvas.model.impl.BasicVertex;
import model.Factory;

public class RemoteSimulationController extends SimulatorController {
private static final Logger LOGGER = Logger.getLogger(RemoteSimulationController.class.getName());
    
    private final String simulationServiceUrl;
    private final String factoryId;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private volatile boolean simulationRunning;
    private Thread updateViewThread;

    public RemoteSimulationController(Factory factoryModel, CanvasPersistenceManager persistenceManager, String simulationServiceUrl) {
        super(factoryModel, persistenceManager);
        this.factoryId = factoryModel.getId();
        this.simulationServiceUrl = simulationServiceUrl;
        this.httpClient = HttpClient.newBuilder()
        		.version(HttpClient.Version.HTTP_1_1)
        		.build();
        this.objectMapper = createObjectMapper();
        this.simulationRunning = false;
    }

    abstract static class BasicVertexMixin {
        @JsonCreator
        public BasicVertexMixin(
            @JsonProperty("xCoordinate") int xCoordinate, 
            @JsonProperty("yCoordinate") int yCoordinate
        ) { }
    }
    
    private ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();

        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.addMixIn(BasicVertex.class, BasicVertexMixin.class);
        
        SimpleModule module = new SimpleModule();
        module.addAbstractTypeMapping(Vertex.class, BasicVertex.class);
        mapper.registerModule(module);
        
        return mapper;
    }

    private String getEncodedFactoryId() {
        return URLEncoder.encode(factoryId, StandardCharsets.UTF_8);
    }

    @Override
    public void startAnimation() {
        try {
        	String url = String.format("%s/start?factoryId=%s", simulationServiceUrl, getEncodedFactoryId());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            
            LOGGER.info(request.toString());

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                simulationRunning = true;
                
                if (updateViewThread != null) {
                	updateViewThread.interrupt();
                }
                
                updateViewThread = new Thread(this::updateViewer);
                updateViewThread.start();
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
            String url = String.format("%s/stop?factoryId=%s", simulationServiceUrl, getEncodedFactoryId());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            LOGGER.info(request.toString());
            
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
    	LOGGER.info("Starting viewer update loop...");
        while (simulationRunning) {
            try {
            	String url = String.format("%s?factoryId=%s", simulationServiceUrl, getEncodedFactoryId());
            	HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();
            	
                LOGGER.info(request.toString());

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                LOGGER.info("DEBUG JSON: " + response.body());
                if (response.statusCode() == 200) {
                    Factory remoteFactory = objectMapper.readValue(response.body(), Factory.class);
                    
                    SwingUtilities.invokeLater(() -> {
                        Factory localFactory = (Factory) getCanvas();
                        
                        if (localFactory != null) {
                            localFactory.getComponents().clear();
                            
                            localFactory.getComponents().addAll(remoteFactory.getComponents());
                            
                            localFactory.notifyObservers();
                        }
                    });
                }
                
                Thread.sleep(25);
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
    	super.setCanvas(canvasModel);
    }
}
