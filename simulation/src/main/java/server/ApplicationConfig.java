package server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;

import fr.tp.inf112.projects.canvas.model.CanvasPersistenceManager;
import fr.tp.inf112.projects.canvas.model.impl.BasicVertex;
import fr.tp.inf112.projects.canvas.view.FileCanvasChooser;
import model.Component;
import model.shapes.PositionedShape;
import persistence.client.FactoryPersistenceManagerClient;

@Configuration
public class ApplicationConfig {

	public static class BasicVertexSerializer extends JsonSerializer<BasicVertex> {
        @Override
        public void serialize(BasicVertex value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeNumberField("xCoordinate", value.getxCoordinate());
            gen.writeNumberField("yCoordinate", value.getyCoordinate());
            gen.writeEndObject();
        }
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
		final PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
				.allowIfSubType(PositionedShape.class.getPackageName())
				.allowIfSubType(Component.class.getPackageName())
				.allowIfSubType(BasicVertex.class.getPackageName())
				.allowIfSubType(ArrayList.class.getName())
				.allowIfSubType(LinkedHashSet.class.getName())
				.build();

		final ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL);

		SimpleModule module = new SimpleModule();
        module.addSerializer(BasicVertex.class, new BasicVertexSerializer());
        objectMapper.registerModule(module);
		
		return objectMapper;
	}
}