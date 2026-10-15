package uml.view.classdiagram;

import java.util.Objects;

/**
 * 속성 또는 메서드 한 줄. text는 어댑터가 완성한 표시 문자열이다(예: "- name: String").
 * 렌더러는 가시성 기호나 타입 문법을 해석하지 않는다.
 *
 * @param underline static 멤버면 true (UML 표기: 밑줄)
 * @param italic    추상 메서드면 true (UML 표기: 기울임)
 */
public record MemberLine(String text, boolean underline, boolean italic) {

    public MemberLine {
        Objects.requireNonNull(text, "text");
        if (text.isBlank()) {
            throw new IllegalArgumentException("member text must not be blank");
        }
    }

    public static MemberLine of(String text) {
        return new MemberLine(text, false, false);
    }
}
