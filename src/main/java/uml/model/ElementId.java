package uml.model;

import java.util.Objects;

/** 이름이나 텍스트가 바뀌어도 같은 요소를 가리키는 안정적인 식별자. */
public record ElementId(String value) {

    public ElementId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ElementId value must not be blank");
        }
    }

    public static ElementId of(String value) {
        return new ElementId(value);
    }
}
