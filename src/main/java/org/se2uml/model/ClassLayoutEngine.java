package org.se2uml.model;

import java.util.List;
import java.util.Map;
import org.se2uml.model.DiagramModel.*;

/** Adapter boundary for an engine selected by the team; no Dagre/ELK dependency here. */
public interface ClassLayoutEngine {
    Result layout(Request request);

    /**
     * Caller supplies measured sizes for every class. Engine must honor manual positions,
     * place unpinned classes, then route edges and calculate bounds from final positions.
     */
    record Request(ClassDiagram diagram, ClassLayout savedLayout,
            Map<ElementId, Size> measuredSizes) {
        public Request { measuredSizes = Map.copyOf(measuredSizes); }
    }

    /** Bounds use top-left origin; negative world coordinates are allowed. */
    record Rectangle(Point topLeft, Size size) {}

    record EdgeRoute(ElementId relationshipId, List<Point> points) {
        public EdgeRoute { points = List.copyOf(points); }
    }

    /** Derived render data, not the semantic model or persisted manual layout. */
    record Result(Map<ElementId, Rectangle> nodes, List<EdgeRoute> edges,
            Rectangle bounds, List<Diagnostic> diagnostics) {
        public Result {
            nodes = Map.copyOf(nodes);
            edges = List.copyOf(edges);
            diagnostics = List.copyOf(diagnostics);
        }
    }

    enum Severity { WARNING, ERROR }
    record Diagnostic(Severity severity, String code, List<ElementId> elements,
            String message) {
        public Diagnostic { elements = List.copyOf(elements); }
    }
}
