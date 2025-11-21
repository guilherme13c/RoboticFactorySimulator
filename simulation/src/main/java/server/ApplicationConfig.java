package server;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

import fr.tp.inf112.projects.canvas.model.CanvasPersistenceManager;
import fr.tp.inf112.projects.canvas.model.impl.BasicVertex;
import fr.tp.inf112.projects.canvas.view.FileCanvasChooser;
import model.Component;
import model.shapes.PositionedShape;
import persistence.client.FactoryPersistenceManagerClient;

@Configuration
public class ApplicationConfig implements WebMvcConfigurer {
	private static transient final Logger LOGGER = Logger.getLogger(ApplicationConfig.class.getName());

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

	@Bean
	CanvasPersistenceManager canvasPersistenceManager(@Value("${server.persistence.host}") String host,
			@Value("${server.persistence.port}") int port) {

		final FileCanvasChooser canvasChooser = new FileCanvasChooser("factory", "Puck Factory");
		return new FactoryPersistenceManagerClient(host, port, canvasChooser);
	}

	@Bean
	@Primary
	ObjectMapper objectMapper() {
		LOGGER.info("DEBUG: Loading Custom ObjectMapper Configuration...");

		ObjectMapper objectMapper = new ObjectMapper();
		PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
				 .allowIfSubType(PositionedShape.class.getPackageName())
				 .allowIfSubType(Component.class.getPackageName())
				 .allowIfSubType(BasicVertex.class.getPackageName())
				 .allowIfSubType(ArrayList.class.getName())
				 .allowIfSubType(LinkedHashSet.class.getName())
				 .build();
		
		objectMapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		
		objectMapper.addMixIn(BasicVertex.class, BasicVertexMixin.class);
		
		LOGGER.info("DEBUG: BasicVertexMixin registered successfully.");

		return objectMapper;
	}
	
	@SuppressWarnings("removal")
	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		@SuppressWarnings("deprecation")
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper());
		
		converters.add(0, converter);
		
		LOGGER.info("DEBUG: Registered Custom MappingJackson2HttpMessageConverter at index 0.");
	}
}