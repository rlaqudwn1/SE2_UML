package uml.model.sequence;

import java.util.Objects;

/** 모델 불변식 위반 한 건. 파서가 오류 목록을 사용자에게 보여줄 때 쓴다. */
public record Violation(String message) {

    public Violation {
        Objects.requireNonNull(message, "message");
    }
}
