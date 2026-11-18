package uml.model.sequence;

import java.util.Objects;
import uml.model.DiagramElement;
import uml.model.ElementId;
import uml.model.Style;

/** 두 Lifeline 사이의 메시지. 자기 호출은 {@code from}과 {@code to}가 같은 경우다. */
public record Message(
        ElementId id, ElementId from, ElementId to, int order,
        String text, MessageType type, Style style) implements DiagramElement {

    public Message {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(style, "style");
        if (order <= 0) {
            throw new IllegalArgumentException("order must be positive: " + order);
        }
    }

    public boolean isSelfCall() {
        return from.equals(to);
    }
}
