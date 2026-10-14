package uml.model.sequence;

import java.util.List;
import java.util.Objects;
import uml.model.DiagramElement;
import uml.model.ElementId;
import uml.model.Style;

public record Note(
        ElementId id, int order, NotePlacement placement,
        List<ElementId> lifelines, String text, Style style) implements DiagramElement {

    public Note {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(placement, "placement");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(style, "style");
        lifelines = List.copyOf(lifelines);
        if (order <= 0) {
            throw new IllegalArgumentException("order must be positive: " + order);
        }
        if (new java.util.HashSet<>(lifelines).size() != lifelines.size()) {
            throw new IllegalArgumentException("note lifelines must be distinct: " + lifelines);
        }
        int expectedMax = placement == NotePlacement.OVER ? 2 : 1;
        if (lifelines.isEmpty() || lifelines.size() > expectedMax) {
            throw new IllegalArgumentException(
                    placement + " note needs 1.." + expectedMax + " lifelines, got " + lifelines.size());
        }
    }
}
