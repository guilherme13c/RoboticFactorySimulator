package controller;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.CanvasPersistenceManager;
import fr.tp.inf112.projects.canvas.model.impl.BasicVertex;
import model.Component;
import model.Factory;
import model.shapes.PositionedShape;

public class RemoteSimulationController extends SimulatorController {
	private static transient final Logger LOGGER = Logger.getLogger(RemoteSimulationController.class.getName());

	private final String simulationServiceUrl;
	private final String factoryId;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private volatile boolean simulationRunning;
	private Thread updateViewThread;

	public RemoteSimulationController(Factory factoryModel, CanvasPersistenceManager persistenceManager,
			String simulationServiceUrl) {
		super(factoryModel, persistenceManager);
		this.factoryId = factoryModel.getId();
		this.simulationServiceUrl = simulationServiceUrl;
		this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
		this.objectMapper = createObjectMapper();
		this.simulationRunning = false;
	}

	@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.ANY)
	abstract static class BasicVertexMixin {
        @JsonCreator
        public BasicVertexMixin(
            @JsonProperty("xCoordinate") int xCoordinate, 
            @JsonProperty("yCoordinate") int yCoordinate
        ) { }
        
        @JsonProperty("xCoordinate")
        abstract int getxCoordinate();

        @JsonProperty("yCoordinate")
        abstract int getyCoordinate();
    }

	private ObjectMapper createObjectMapper() {
		final ObjectMapper mapper = new ObjectMapper();

		PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
				 .allowIfSubType(PositionedShape.class.getPackageName())
				 .allowIfSubType(Component.class.getPackageName())
				 .allowIfSubType(BasicVertex.class.getPackageName())
				 .allowIfSubType(ArrayList.class.getName())
				 .allowIfSubType(LinkedHashSet.class.getName())
				 .build();
		
		mapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		
		mapper.addMixIn(BasicVertex.class, BasicVertexMixin.class);

		return mapper;
	}

	private String getEncodedFactoryId() {
		return URLEncoder.encode(factoryId, StandardCharsets.UTF_8);
	}

	@Override
	public void startAnimation() {
		try {
			String url = String.format("%s/start?factoryId=%s", simulationServiceUrl, getEncodedFactoryId());
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
					.POST(HttpRequest.BodyPublishers.noBody()).build();

			LOGGER.info(request.toString());

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() == 200) {
				simulationRunning = true;

				if (updateViewThread != null && updateViewThread.isAlive()) {
					updateViewThread.interrupt();
				}

				updateViewThread = new Thread(this::updateViewer);
				updateViewThread.start();
			} else {
				LOGGER.log(Level.SEVERE, "Failed to start simulation: " + response.body());
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error starting remote simulation: " + e.getMessage(), e);
		}
	}

	@Override
	public void stopAnimation() {
		try {
			simulationRunning = false;
			String url = String.format("%s/stop?factoryId=%s", simulationServiceUrl, getEncodedFactoryId());
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
					.POST(HttpRequest.BodyPublishers.noBody()).build();

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
				HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();

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
