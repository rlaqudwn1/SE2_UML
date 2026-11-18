package uml.view.classdiagram;

import java.util.Objects;

/**
 * 장면 불변식 위반 한 건. {@code uml.model.sequence.Violation}과 같은 모양이다.
 * 공통 타입으로 합치자는 제안은 docs/CLASS_VIEW_CONTRACT.md에 적었다.
 */
public record Violation(String message) {

    public Violation {
        Objects.requireNonNull(message, "message");
    }
}
