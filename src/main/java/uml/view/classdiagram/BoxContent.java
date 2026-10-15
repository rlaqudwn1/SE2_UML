package uml.view.classdiagram;

import java.util.List;
import java.util.Objects;

/**
 * 클래스 박스 3칸(이름 / 속성 / 메서드)에 들어갈 글자. 클래스 의미 모델이 아니라 화면용 데이터다.
 * 어댑터가 의미 모델에서 만들어 넘기며, 렌더러는 UML 문법을 해석하지 않는다.
 *
 * @param stereotypes  이름 위에 «»로 표시할 줄. 예: "interface"
 * @param abstractName 추상 클래스면 true. 이름을 기울임체로 그린다.
 */
public record BoxContent(
        String name, List<String> stereotypes, boolean abstractName,
        List<MemberLine> attributes, List<MemberLine> operations) {

    public BoxContent {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("class name must not be blank");
        }
        stereotypes = List.copyOf(stereotypes);
        attributes = List.copyOf(attributes);
        operations = List.copyOf(operations);
        for (String s : stereotypes) {
            if (s.isBlank()) {
                throw new IllegalArgumentException("stereotype must not be blank");
            }
        }
    }

    public static BoxContent of(String name, List<MemberLine> attributes, List<MemberLine> operations) {
        return new BoxContent(name, List.of(), false, attributes, operations);
    }
}
