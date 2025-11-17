package model.path;

import java.util.List;

import model.Component;
import model.Position;

public interface FactoryPathFinder {

	List<Position> findPath(Component sourceComponent, Component targetComponent);
}
