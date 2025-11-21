package persistence.model;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.CanvasChooser;
import fr.tp.inf112.projects.canvas.model.impl.AbstractCanvasPersistenceManager;

public class FactoryPersistenceManager extends AbstractCanvasPersistenceManager {

	private static transient Logger LOGGER = Logger.getLogger(FactoryPersistenceManager.class.getName());
	
	public FactoryPersistenceManager(final CanvasChooser canvasChooser) {
		super(canvasChooser);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Canvas read(final String canvasId) throws IOException {
		try (final InputStream fileInputStream = new FileInputStream(canvasId);
				final InputStream bufInputStream = new BufferedInputStream(fileInputStream);
				final ObjectInputStream objectInputStrteam = new ObjectInputStream(bufInputStream);) {
			return (Canvas) objectInputStrteam.readObject();
		} catch (ClassNotFoundException | IOException ex) {
			throw new IOException(ex);
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void persist(Canvas canvasModel) throws IOException {
		if (canvasModel.getId() == null) {
			throw new IOException("Canvas missing ID");
		}
		
		try (final OutputStream fileOutStream = new FileOutputStream(canvasModel.getId());
				final OutputStream bufOutStream = new BufferedOutputStream(fileOutStream);
				final ObjectOutputStream objOutStream = new ObjectOutputStream(bufOutStream);) {
			objOutStream.writeObject(canvasModel);
		} catch (IOException e) {
			LOGGER.info("failed to persist canvas model: " + canvasModel.toString());
			throw e;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean delete(final Canvas canvasModel) throws IOException {
		final File canvasFile = new File(canvasModel.getId());

		return canvasFile.delete();
	}
}
