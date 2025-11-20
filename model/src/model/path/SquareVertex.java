package model.path;

import fr.tp.inf112.projects.graph.impl.GridVertex;
import model.Position;
import model.shapes.RectangularShape;

public class SquareVertex extends GridVertex {

	private final RectangularShape shape;

	public SquareVertex() {
		super(null, 0, 0);
		this.shape = new RectangularShape();
	}
	
	public SquareVertex(final String label, final int xCoordinate, final int yCoordinate, final int size) {
		super(label, xCoordinate, yCoordinate);

		this.shape = new RectangularShape(xCoordinate, yCoordinate, size, size);
	}

	public RectangularShape getShape() {
		return shape;
	}

	public Position getPosition() {
		return getShape() == null ? null : getShape().getPosition();
	}
}
