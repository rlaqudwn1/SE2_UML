package uml.model.sequence;

import java.util.Objects;
import uml.model.DiagramElement;
import uml.model.ElementId;
import uml.model.Style;

/** 한 Lifeline의 활성 구간. 두 order는 메시지의 order를 가리킨다. */
public record ActivationBar(
        ElementId id, ElementId lifelineId, int startOrder, int endOrder, Style style)
        implements DiagramElement {

    public ActivationBar {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(lifelineId, "lifelineId");
        Objects.requireNonNull(style, "style");
        if (startOrder <= 0 || endOrder < startOrder) {
            throw new IllegalArgumentException(
                    "invalid activation range: " + startOrder + ".." + endOrder);
        }
    }
}
