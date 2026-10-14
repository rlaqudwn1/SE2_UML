package org.se2uml.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.se2uml.model.ClassDiagram.*;
import org.se2uml.model.DiagramModel.*;

/** Dependency-free executable checks for the model contract (not an engine test). */
public final class ModelContractCheck {
    public static void main(String[] args) {
        var id = new ElementId("class-001");
        var node = new ClassNode(id, "User", ClassKind.CLASS, List.of(), List.of(), List.of());
        var diagram = new ClassDiagram(List.of(node), List.of());
        var automatic = ClassLayout.automatic();
        var moved = automatic.withPosition(id, new Point(-120, 240));
        var document = new DiagramDocument<>(1, diagram, moved);
        check(automatic.manualPositions().isEmpty(), "Original layout must remain unchanged");
        check(document.diagram() == diagram, "Move must not change UML semantics");

        var renamed = new ClassNode(id, "Account", ClassKind.CLASS, List.of(), List.of(), List.of());
        var added = new ClassNode(new ElementId("class-002"), "Order", ClassKind.CLASS,
                List.of(), List.of(), List.of());
        var edited = new DiagramDocument<>(1,
                new ClassDiagram(List.of(renamed, added), List.of()), document.layout());
        check(edited.layout().manualPositions().get(id).equals(new Point(-120, 240)),
                "Rename and insertion must retain existing manual position");
        check(!edited.layout().manualPositions().containsKey(added.id()),
                "New node must remain available for automatic placement");

        var mutable = new HashMap<>(moved.manualPositions());
        var restored = new ClassLayout(mutable);
        mutable.clear();
        check(restored.equals(moved), "Layout must defensively copy restored data");
        check(moved.withoutPosition(id).equals(automatic), "Reset must release manual position");
        expectInvalid(() -> new Point(Double.NaN, 0));
        expectInvalid(() -> new Size(0, 10));
        expectInvalid(() -> new DiagramDocument<>(2, diagram, moved));
        LayoutState sequenceLayout = () -> DiagramKind.SEQUENCE;
        expectInvalid(() -> new DiagramDocument<>(1, diagram, sequenceLayout));
        System.out.println("Model contract checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectInvalid(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected IllegalArgumentException");
    }
}
