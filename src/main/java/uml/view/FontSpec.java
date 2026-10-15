package uml.view;

/**
 * 글자 모양. 글꼴 이름은 {@link TextMetrics} 구현이 정한다(다이어그램 전체 단일 글꼴).
 * size는 {@link uml.model.Style#fontSize()}를 그대로 쓴다.
 */
public record FontSpec(int size, boolean bold, boolean italic) {

    public FontSpec {
        if (size <= 0) {
            throw new IllegalArgumentException("font size must be positive: " + size);
        }
    }
}
