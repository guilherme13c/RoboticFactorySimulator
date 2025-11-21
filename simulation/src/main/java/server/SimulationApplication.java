package server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = { "server", "controller" })
public class SimulationApplication {

	public static void main(String[] args) {
		SpringApplication.run(SimulationApplication.class, args);
	}

}
