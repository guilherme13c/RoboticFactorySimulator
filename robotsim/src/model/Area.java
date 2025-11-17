package model;

import fr.tp.inf112.projects.canvas.model.Style;
import model.shapes.PositionedShape;
import model.shapes.RectangularShape;

public class Area extends Component {

	private static final long serialVersionUID = 5022214804847296168L;

	private static final Style STYLE = new ComponentStyle(ComponentStyle.DEFAULT_DASH_PATTERN);

	private Machine machine;

	public Area(final Room room, final RectangularShape shape, final String name) {
		super(room.getFactory(), shape, name);

		room.addArea(this);

		machine = null;
	}

	protected void setMachine(final Machine machine) {
		this.machine = machine;
	}

	@Override
	public boolean canBeOverlayed(final PositionedShape shape) {
		return true;
	}

	@Override
	public String toString() {
		return super.toString() + " machine=" + machine + "]";
	}

	@Override
	public Style getStyle() {
		return STYLE;
	}
}
