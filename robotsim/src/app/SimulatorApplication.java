package app;

import java.awt.Component;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import fr.tp.inf112.projects.canvas.model.impl.BasicVertex;
import fr.tp.inf112.projects.canvas.view.CanvasViewer;
import fr.tp.inf112.projects.canvas.view.FileCanvasChooser;
import model.Area;
import model.Battery;
import model.ChargingStation;
import model.Conveyor;
import model.Door;
import model.Factory;
import model.Machine;
import model.Robot;
import model.Room;
import model.path.CustomDijkstraFactoryPathFinder;
import model.path.FactoryPathFinder;
import model.path.JGraphTDijkstraFactoryPathFinder;
import model.shapes.BasicPolygonShape;
import model.shapes.CircularShape;
import model.shapes.RectangularShape;
import persistence.FactoryPersistenceManagerClient;

public class SimulatorApplication {
	private static transient final Logger LOGGER = Logger.getLogger(SimulatorApplication.class.getName());

	public static void main(String[] args) {

		LOGGER.log(Level.INFO, "Starting the robot simulator...");

		LOGGER.log(Level.CONFIG, "With parameters " + Arrays.toString(args) + ".");

		final Factory factory = new Factory(200, 200, "Simple Test Puck Factory");
		final Room room1 = new Room(factory, new RectangularShape(20, 20, 75, 75), "Production Room 1");
		new Door(room1, Room.WALL.BOTTOM, 10, 20, true, "Entrance");
		final Area area1 = new Area(room1, new RectangularShape(35, 35, 50, 50), "Production Area 1");
		final Machine machine1 = new Machine(area1, new RectangularShape(50, 50, 15, 15), "Machine 1");

		final Room room2 = new Room(factory, new RectangularShape(120, 22, 75, 75), "Production Room 2");
		new Door(room2, Room.WALL.LEFT, 10, 20, true, "Entrance");
		final Area area2 = new Area(room2, new RectangularShape(135, 35, 50, 50), "Production Area 1");
		final Machine machine2 = new Machine(area2, new RectangularShape(150, 50, 15, 15), "Machine 1");

		final int baselineSize = 3;
		final int xCoordinate = 10;
		final int yCoordinate = 165;
		final int width = 10;
		final int height = 30;
		final BasicPolygonShape conveyorShape = new BasicPolygonShape();
		conveyorShape.addVertex(new BasicVertex(xCoordinate, yCoordinate));
		conveyorShape.addVertex(new BasicVertex(xCoordinate + width, yCoordinate));
		conveyorShape.addVertex(new BasicVertex(xCoordinate + width, yCoordinate + height - baselineSize));
		conveyorShape
				.addVertex(new BasicVertex(xCoordinate + width + baselineSize, yCoordinate + height - baselineSize));
		conveyorShape.addVertex(new BasicVertex(xCoordinate + width + baselineSize, yCoordinate + height));
		conveyorShape.addVertex(new BasicVertex(xCoordinate - baselineSize, yCoordinate + height));
		conveyorShape.addVertex(new BasicVertex(xCoordinate - baselineSize, yCoordinate + height - baselineSize));
		conveyorShape.addVertex(new BasicVertex(xCoordinate, yCoordinate + height - baselineSize));

		final Room chargingRoom = new Room(factory, new RectangularShape(125, 125, 50, 50), "Charging Room");
		new Door(chargingRoom, Room.WALL.RIGHT, 10, 20, true, "Entrance");
		final ChargingStation chargingStation = new ChargingStation(factory, new RectangularShape(150, 145, 15, 15),
				"Charging Station");

		final FactoryPathFinder jgraphPahtFinder = new JGraphTDijkstraFactoryPathFinder(factory, 5);
		final Robot robot1 = new Robot(factory, jgraphPahtFinder, new CircularShape(5, 5, 2), new Battery(10),
				"Robot 1");
		robot1.addTargetComponent(machine1);
		robot1.addTargetComponent(machine2);
		robot1.addTargetComponent(new Conveyor(factory, conveyorShape, "Conveyor 1"));
		robot1.addTargetComponent(chargingStation);

		final FactoryPathFinder customPathFinder = new CustomDijkstraFactoryPathFinder(factory, 5);
		final Robot robot2 = new Robot(factory, customPathFinder, new CircularShape(45, 5, 2), new Battery(10),
				"Robot 2");
		robot2.addTargetComponent(machine1);
		robot2.addTargetComponent(machine2);
		robot2.addTargetComponent(new Conveyor(factory, conveyorShape, "Conveyor 1"));

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				if (args.length < 1) {
					LOGGER.log(Level.SEVERE, "missing factory persistence manager address arguments");
				}

				final String[] factoryPersistenceManagerServerAddress = args[0].split(":");

				if (factoryPersistenceManagerServerAddress.length != 2) {
					LOGGER.log(Level.SEVERE, "invalid factory perisistence manager server address and port");
					return;
				}

				final FileCanvasChooser canvasChooser = new FileCanvasChooser("factory", "Puck Factory");
				final FactoryPersistenceManagerClient persistenceClient = new FactoryPersistenceManagerClient(
						factoryPersistenceManagerServerAddress[0],
						Integer.parseInt(factoryPersistenceManagerServerAddress[1]), canvasChooser);
				final Component factoryViewer = new CanvasViewer(new SimulatorController(factory, persistenceClient));
				canvasChooser.setViewer(factoryViewer);
			}
		});
	}
}
