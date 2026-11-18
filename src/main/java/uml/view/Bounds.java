package uml.view;

/** 왼쪽 위 꼭짓점 (x, y)와 폭·높이로 나타낸 사각형. 빈 영역을 위해 폭·높이 0은 허용한다. */
public record Bounds(double x, double y, double width, double height) {

    public Bounds {
        if (!Double.isFinite(x) || !Double.isFinite(y)
                || !Double.isFinite(width) || !Double.isFinite(height)) {
            throw new IllegalArgumentException("bounds must be finite");
        }
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("bounds size must not be negative: " + width + "x" + height);
        }
    }
}
