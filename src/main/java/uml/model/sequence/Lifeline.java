package uml.model.sequence;

import java.util.Objects;
import uml.model.DiagramElement;
import uml.model.ElementId;
import uml.model.Style;

/** 시퀀스 다이어그램의 참여자. {@code order}는 왼쪽부터의 순번이며 좌표가 아니다. */
public record Lifeline(ElementId id, String participantName, int order, Style style)
        implements DiagramElement {

    public Lifeline {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(participantName, "participantName");
        Objects.requireNonNull(style, "style");
        if (participantName.isBlank()) {
            throw new IllegalArgumentException("participantName must not be blank");
        }
        if (order <= 0) {
            throw new IllegalArgumentException("order must be positive: " + order);
        }
    }
}
