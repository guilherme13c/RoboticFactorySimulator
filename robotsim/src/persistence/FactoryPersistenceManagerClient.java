package persistence;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.CanvasChooser;
import model.Factory;

public class FactoryPersistenceManagerClient extends FactoryPersistenceManager {
	private static Logger LOGGER = Logger.getLogger(FactoryPersistenceManagerClient.class.getName());

	private Socket clientSocket;
	private ObjectInputStream in;
	private ObjectOutputStream out;

	private String serverAddress;
	private int port;

	public FactoryPersistenceManagerClient(String serverAddress, int serverPort, CanvasChooser canvasChooser) {
		super(canvasChooser);

		this.serverAddress = serverAddress;
		this.port = serverPort;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void persist(Canvas canvasModel) throws IOException {
		try {
			connect();

			out.writeObject(canvasModel);
			out.flush();

			Boolean status = in.readBoolean();

			if (status) {
				return;
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			throw new IOException("Failed to persist Canvas", e);
		} finally {
			close();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Factory read(final String canvasId) throws IOException {
		try {
			connect();

			out.writeObject(canvasId);
			out.flush();

			Factory factory = (Factory) in.readObject();

			if (factory != null) {
				return factory;
			}
			return null;
		} catch (Exception e) {
			throw new IOException("Failed to get Factory instance", e);
		} finally {
			close();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	private void connect() throws IOException {
		clientSocket = new Socket(serverAddress, port);
		out = new ObjectOutputStream(clientSocket.getOutputStream());
		in = new ObjectInputStream(clientSocket.getInputStream());
	}

	/**
	 * {@inheritDoc}
	 */
	private void close() throws IOException {
		if (clientSocket != null && !clientSocket.isClosed()) {
			clientSocket.close();
		}

		if (out != null) {
			out.close();
		}

		if (in != null) {
			in.close();
		}
	}
}