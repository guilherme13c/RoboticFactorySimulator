package model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fr.tp.inf112.projects.canvas.model.Style;
import fr.tp.inf112.projects.canvas.model.impl.RGBColor;
import model.motion.Motion;
import model.path.FactoryPathFinder;
import model.shapes.CircularShape;
import model.shapes.PositionedShape;
import model.shapes.RectangularShape;

public class Robot extends Component {

	private static final long serialVersionUID = -1218857231970296747L;

	private static final Style STYLE = new ComponentStyle(RGBColor.GREEN, RGBColor.BLACK, 3.0f, null);

	private static final Style BLOCKED_STYLE = new ComponentStyle(RGBColor.RED, RGBColor.BLACK, 3.0f,
			new float[] { 4.0f });

	private final Battery battery;

	private int speed;

	private List<Component> targetComponents;

	private transient Iterator<Component> targetComponentsIterator;

	private Component currTargetComponent;

	private transient Iterator<Position> currentPathPositionsIter;

	private transient boolean blocked;

	private Position memorizedTargetPosition;

	private FactoryPathFinder pathFinder;

	public Robot(final Factory factory,
			final FactoryPathFinder pathFinder,
			final CircularShape shape,
			final Battery battery,
			final String name) {
		super(factory, shape, name);

		this.pathFinder = pathFinder;

		this.battery = battery;

		targetComponents = new ArrayList<>();
		currTargetComponent = null;
		currentPathPositionsIter = null;
		speed = 5;
		blocked = false;
		memorizedTargetPosition = null;
	}

	@Override
	public String toString() {
		return super.toString() + " battery=" + battery + "]";
	}

	protected int getSpeed() {
		return speed;
	}

	protected void setSpeed(final int speed) {
		this.speed = speed;
	}

	public Position getMemorizedTargetPosition() {
		return memorizedTargetPosition;
	}

	private List<Component> getTargetComponents() {
		if (targetComponents == null) {
			targetComponents = new ArrayList<>();
		}

		return targetComponents;
	}

	public boolean addTargetComponent(final Component targetComponent) {
		return getTargetComponents().add(targetComponent);
	}

	public boolean removeTargetComponent(final Component targetComponent) {
		return getTargetComponents().remove(targetComponent);
	}

	@Override
	public boolean isMobile() {
		return true;
	}

	@Override
	public boolean behave() {
		if (getTargetComponents().isEmpty()) {
			return false;
		}

		if (currTargetComponent == null || hasReachedCurrentTarget()) {
			currTargetComponent = nextTargetComponentToVisit();

			computePathToCurrentTargetComponent();
		}

		return moveToNextPathPosition() != 0;
	}

	private Component nextTargetComponentToVisit() {
		if (targetComponentsIterator == null || !targetComponentsIterator.hasNext()) {
			targetComponentsIterator = getTargetComponents().iterator();
		}

		return targetComponentsIterator.hasNext() ? targetComponentsIterator.next() : null;
	}

	private int moveToNextPathPosition() {
		final Motion motion = computeMotion();

		int displacement = motion == null ? 0 : getFactory().moveComponent(motion, this);

		if (displacement != 0) {
			notifyObservers();
		} else if (isLivelyLocked()) {
			final Position freeNeighbouringPosition = findFreeNeighbouringPosition();
			if (freeNeighbouringPosition != null) {
				this.memorizedTargetPosition = freeNeighbouringPosition;
				displacement = moveToNextPathPosition();
				computePathToCurrentTargetComponent();
			}
		}
		return displacement;
	}

	private Position findFreeNeighbouringPosition() {
		final int[] dx = { 0, 1, 0, -1 };
		final int[] dy = { 1, 0, -1, 0 };

		for (int i = 0; i < 4; i++) {
			final PositionedShape shape = new RectangularShape(this.getxCoordinate() + dx[i] * this.getSpeed(),
					this.getyCoordinate() + dy[i] * this.getSpeed(), 1, 1);

			if (!getFactory().hasMobileComponentAt(shape, this) && !getFactory().hasObstacleAt(shape)) {
				return shape.getPosition();
			}
		}

		return null;
	}

	private void computePathToCurrentTargetComponent() {
		final List<Position> currentPathPositions = pathFinder.findPath(this, currTargetComponent);
		currentPathPositionsIter = currentPathPositions.iterator();
	}

	private Motion computeMotion() {
		if (currentPathPositionsIter == null) {
			computePathToCurrentTargetComponent();
		}

		if (!currentPathPositionsIter.hasNext()) {

			// There is no free path to the target
			blocked = true;

			return null;
		}
		blocked = false;

		final Position targetPosition = getTargetPosition();
		final PositionedShape shape = new RectangularShape(targetPosition.getxCoordinate(),
				targetPosition.getyCoordinate(),
				2,
				2);

		// If there is another robot, memorize the target position for the next run
		if (getFactory().hasMobileComponentAt(shape, this)) {
			this.memorizedTargetPosition = targetPosition;

			return null;
		}

		// Reset the memorized position
		this.memorizedTargetPosition = null;

		return new Motion(getPosition(), targetPosition);
	}

	private Position getTargetPosition() {
		// If a target position was memorized, it means that the robot was blocked
		// during the last iteration
		// so it waited for another robot to pass. So try to move to this memorized
		// position otherwise move to
		// the next position from the path
		return this.memorizedTargetPosition == null ? currentPathPositionsIter.next() : this.memorizedTargetPosition;
	}

	public boolean isLivelyLocked() {
		if (memorizedTargetPosition == null) {
			return false;
		}

		final Component otherComponent = getFactory().getMobileComponentAt(memorizedTargetPosition,
				this);

		if (otherComponent instanceof Robot) {
			return getPosition().equals(((Robot) otherComponent).getMemorizedTargetPosition());
		}

		return false;
	}

	private boolean hasReachedCurrentTarget() {
		return getPositionedShape().overlays(currTargetComponent.getPositionedShape());
	}

	@Override
	public boolean canBeOverlayed(final PositionedShape shape) {
		return true;
	}

	@Override
	public Style getStyle() {
		return blocked ? BLOCKED_STYLE : STYLE;
	}
}
