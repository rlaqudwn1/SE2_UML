package uml.view.classdiagram;

/**
 * 측정한 박스 크기와 칸별 높이. 세 칸 높이의 합은 전체 높이와 같다.
 * 배치 담당은 width·height로 위치를 정하고, 렌더러는 칸 높이로 구분선 위치를 정한다.
 */
public record BoxMetrics(
        double width, double height,
        double headerHeight, double attributeHeight, double operationHeight) {

    private static final double EPSILON = 1e-6;

    public BoxMetrics {
        positive(width, "width");
        positive(height, "height");
        positive(headerHeight, "headerHeight");
        positive(attributeHeight, "attributeHeight");
        positive(operationHeight, "operationHeight");
        if (Math.abs(headerHeight + attributeHeight + operationHeight - height) > EPSILON) {
            throw new IllegalArgumentException("compartment heights must add up to height");
        }
    }

    private static void positive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive: " + value);
        }
    }
}
