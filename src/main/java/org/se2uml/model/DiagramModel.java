package org.se2uml.model;

import java.util.Objects;

/** Engine-independent contracts shared by diagram teams. Java 17+. */
public final class DiagramModel {
    private DiagramModel() {}

    public enum DiagramKind { CLASS, SEQUENCE }

    /** Stable internal identity; never derive identity from a display name. */
    public record ElementId(String value) {
        public ElementId {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Element ID must not be blank");
            }
        }
    }

    public interface SemanticDiagram {
        DiagramKind kind();
    }

    /** Each team defines its own layout constraints behind this contract. */
    public interface LayoutState {
        DiagramKind kind();
    }

    public record DiagramDocument<M extends SemanticDiagram, L extends LayoutState>(
            int schemaVersion, M diagram, L layout) {
        public DiagramDocument {
            if (schemaVersion != 1) {
                throw new IllegalArgumentException("Unsupported schema version");
            }
            Objects.requireNonNull(diagram, "diagram");
            Objects.requireNonNull(layout, "layout");
            if (diagram.kind() != layout.kind()) {
                throw new IllegalArgumentException("Diagram and layout kinds must match");
            }
        }
    }

    /** World coordinates: x right, y down; independent of zoom and pan. */
    public record Point(double x, double y) {
        public Point {
            if (!Double.isFinite(x) || !Double.isFinite(y)) {
                throw new IllegalArgumentException("Coordinates must be finite");
            }
        }
    }

    public record Size(double width, double height) {
        public Size {
            if (!Double.isFinite(width) || !Double.isFinite(height)
                    || width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Size must be finite and positive");
            }
        }
    }

    /** Persistence boundary; JSON/file/database adapters are implemented later. */
    public interface DocumentCodec<M extends SemanticDiagram, L extends LayoutState> {
        String encode(DiagramDocument<M, L> document);
        DiagramDocument<M, L> decode(String serialized);
    }
}
