package server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.tp.inf112.projects.canvas.model.CanvasPersistenceManager;
import fr.tp.inf112.projects.canvas.view.FileCanvasChooser;
import persistence.client.FactoryPersistenceManagerClient;

@Configuration
public class ApplicationConfig {

	@Bean
	CanvasPersistenceManager canvasPersistenceManager(@Value("${server.persistence.host}") String host,
			@Value("${server.persistence.port}") int port) {

		final FileCanvasChooser canvasChooser = new FileCanvasChooser("factory", "Puck Factory");
		return new FactoryPersistenceManagerClient(host, port, canvasChooser);
	}
}