package uml.view.classdiagram;

import java.util.Objects;
import uml.model.ElementId;
import uml.model.Style;
import uml.view.Bounds;

/**
 * 화면에 그릴 클래스 박스 하나.
 *
 * @param id       클래스의 안정적인 ID. 의미 모델의 ID를 그대로 쓴다.
 * @param bounds   배치가 끝난 위치와 크기. 크기는 {@link ClassBoxMeasurer} 결과여야 한다.
 * @param style    색과 글자 크기. 색을 박스 어디에 적용할지는 합의 사항이다.
 * @param selected 선택 표시 여부. 화면 상태이며 저장·Undo 대상이 아니다.
 */
public record BoxView(ElementId id, BoxContent content, Bounds bounds, Style style, boolean selected) {

    public BoxView {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(style, "style");
    }
}
