package persistence.server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.view.FileCanvasChooser;
import model.Factory;
import persistence.model.FactoryPersistenceManager;

public class FactoryPersistenceServer {
	private static transient Logger LOGGER = Logger.getLogger(FactoryPersistenceServer.class.getName());

	private ServerSocket serverSocket;
	private Socket clientSocket;
	private ObjectOutputStream out;
	private ObjectInputStream in;

	private void run(int port) throws Exception {
		serverSocket = new ServerSocket(port);
		LOGGER.info("server running on port: " + port);

		while (true) {
			try {
				clientSocket = serverSocket.accept();
				LOGGER.info("accepted connection");

				out = new ObjectOutputStream(clientSocket.getOutputStream());
				in = new ObjectInputStream(clientSocket.getInputStream());

				FileCanvasChooser fcc = new FileCanvasChooser("factory", "factory simulation");
				FactoryPersistenceManager fpm = new FactoryPersistenceManager(fcc);

				Object obj = in.readObject();

				LOGGER.info("received: " + obj.toString());
				
				if (obj instanceof String) {
					LOGGER.info("retrieving: " + obj.toString());
					final Canvas canvasModel = fpm.read((String) obj);
					out.writeObject(canvasModel);
					out.flush();
				} else if (obj instanceof Canvas) {
					LOGGER.info("persisting: " + ((Factory) obj).toString());
					fpm.persist((Factory) obj);
				} else {
					LOGGER.log(Level.INFO, "obj is neither String or Factory");
				}
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, e.getMessage(), e);
			} finally {
				clientSocket.close();
			}
		}
	}

	private void stop() {
		try {
			if (in != null) {
				in.close();
			}

			if (out != null) {
				out.close();
			}

			if (clientSocket != null) {
				clientSocket.close();
			}

			if (serverSocket != null) {
				serverSocket.close();
			}
		} catch (Exception e) {
		}
	}

	public static void main(String[] args) {
		FactoryPersistenceServer server = new FactoryPersistenceServer();

		int port = 9000;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		}

		try {
			server.run(port);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		} finally {
			server.stop();
		}
	}
}
