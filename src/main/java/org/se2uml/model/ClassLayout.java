package org.se2uml.model;

import java.util.HashMap;
import java.util.Map;
import org.se2uml.model.DiagramModel.*;

/** Persist only manual top-left positions. Missing entries request automatic layout. */
public record ClassLayout(Map<ElementId, Point> manualPositions) implements LayoutState {
    public ClassLayout { manualPositions = Map.copyOf(manualPositions); }

    @Override
    public DiagramKind kind() { return DiagramKind.CLASS; }

    /** Pure layout edit: semantic data and IDs remain unchanged. */
    public ClassLayout withPosition(ElementId classId, Point position) {
        var updated = new HashMap<>(manualPositions);
        updated.put(classId, position);
        return new ClassLayout(updated);
    }

    public ClassLayout withoutPosition(ElementId classId) {
        var updated = new HashMap<>(manualPositions);
        updated.remove(classId);
        return new ClassLayout(updated);
    }

    public static ClassLayout automatic() { return new ClassLayout(Map.of()); }
}
