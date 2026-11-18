package uml.view.classdiagram;

import uml.model.Style;

/**
 * 클래스 박스 크기를 글자에 맞춰 계산한다(FR-11 자동 크기).
 *
 * <p>배치 담당은 이 결과로 위치를 정한다. 측정은 렌더러 쪽 책임이고, 배치는 글꼴이나 문법을 해석하지 않는다.
 * 같은 입력에는 항상 같은 크기를 돌려줘야 한다(화면과 이미지 내보내기가 일치해야 하므로).
 */
public interface ClassBoxMeasurer {

    /** 글자 크기는 {@code style.fontSize()}를 쓴다. 빈 칸도 높이를 가져 3칸 모양이 유지된다. */
    BoxMetrics measure(BoxContent content, Style style);
}
