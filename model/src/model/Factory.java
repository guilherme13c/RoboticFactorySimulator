package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import fr.tp.inf112.projects.canvas.controller.Observable;
import fr.tp.inf112.projects.canvas.controller.Observer;
import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.Figure;
import fr.tp.inf112.projects.canvas.model.Style;
import model.motion.Motion;
import model.shapes.PositionedShape;
import model.shapes.RectangularShape;

public class Factory extends Component implements Canvas, Observable {

	private static final long serialVersionUID = 5156526483612458192L;

	private static final ComponentStyle DEFAULT = new ComponentStyle(5.0f);

	@JsonManagedReference
	private final List<Component> components;

	@JsonIgnore
	private transient FactoryModelChangedNotifier notifier;

	@JsonIgnore
	private transient boolean simulationStarted;

	@JsonIgnore
	private transient List<Thread> activeThreads = new ArrayList<>();

	public Factory() {
		super();
		this.components = new ArrayList<>();
		this.notifier = new LocalFactoryModelChangedNotifier();
	}

	public Factory(final int width, final int height, final String name) {
		super(null, new RectangularShape(0, 0, width, height), name);

		components = new ArrayList<>();
		notifier = new LocalFactoryModelChangedNotifier();
		simulationStarted = false;
		this.setId(name);
	}

	public void setNotifier(FactoryModelChangedNotifier notifier) {
        this.notifier = notifier;
    }

	@Override
    public boolean addObserver(Observer observer) {
        if (notifier == null) {
            return false;
        }
        return notifier.addObserver(observer);
    }

	@Override
    public boolean removeObserver(Observer observer) {
        if (notifier == null) {
        	return false;
        }
        return notifier.removeObserver(observer);
    }

	@Override
    public void notifyObservers() {
        if (notifier == null) {
        	return;
        }
        notifier.notifyObservers();
    }

	public boolean addComponent(final Component component) {
		if (components.add(component)) {
			notifyObservers();

			return true;
		}

		return false;
	}

	public boolean removeComponent(final Component component) {
		if (components.remove(component)) {
			notifyObservers();

			return true;
		}

		return false;
	}

	public List<Component> getComponents() {
		return components;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@JsonIgnore
	@Override
	public Collection<Figure> getFigures() {
		return (Collection) components;
	}

	@Override
	public String toString() {
		return super.toString() + " components=" + components + "]";
	}

	@Override
	public boolean isSimulationStarted() {
		return simulationStarted;
	}

	public void startSimulation() {
		if (!isSimulationStarted()) {
			this.simulationStarted = true;
			notifyObservers();

			behave();
		}
	}

	public void stopSimulation() {
		if (isSimulationStarted()) {
			this.simulationStarted = false;

			if (activeThreads != null) {
				for (Thread th : activeThreads) {
					if (th != null && th.isAlive()) {
						th.interrupt();
					}
				}
				activeThreads.clear();
			}

			notifyObservers();
		}
	}

	@Override
	public boolean behave() {
		boolean behaved = true;

		if (activeThreads == null) {
			activeThreads = new ArrayList<>();
		}

		for (final Component component : getComponents()) {
			Thread th = new Thread(component);
			activeThreads.add(th);
			th.start();
		}

		return behaved;
	}

	@Override
	public Style getStyle() {
		return DEFAULT;
	}

	public boolean hasObstacleAt(final PositionedShape shape) {
		for (final Component component : getComponents()) {
			if (component.overlays(shape) && !component.canBeOverlayed(shape)) {
				return true;
			}
		}

		return false;
	}

	public boolean hasMobileComponentAt(final PositionedShape shape, final Component movingComponent) {
		for (final Component component : getComponents()) {
			if (component != movingComponent && component.isMobile() && component.overlays(shape)) {
				return true;
			}
		}

		return false;
	}

	public Component getMobileComponentAt(final Position position, final Component ignoredComponent) {
		if (position == null) {
			return null;
		}

		return getMobileComponentAt(new RectangularShape(position.getxCoordinate(), position.getyCoordinate(), 2, 2),
				ignoredComponent);
	}

	public Component getMobileComponentAt(final PositionedShape shape, final Component ignoredComponent) {
		if (shape == null) {
			return null;
		}

		for (final Component component : getComponents()) {
			if (component != ignoredComponent && component.isMobile() && component.overlays(shape)) {
				return component;
			}
		}

		return null;
	}

	public synchronized int moveComponent(final Motion motion, final Component component) {
		final Position targetPosition = motion.getTargetPosition();
		final PositionedShape shape = new RectangularShape(targetPosition.getxCoordinate(),
				targetPosition.getyCoordinate(), component.getWidth(), component.getHeight());

		if (hasObstacleAt(shape) || hasMobileComponentAt(shape, component)) {
			return 0;
		}

		return motion.moveToTarget();
	}
}