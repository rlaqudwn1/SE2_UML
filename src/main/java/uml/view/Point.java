package uml.view;

/**
 * 다이어그램 좌표의 한 점. 확대·스크롤과 무관한 월드 좌표이며 x는 오른쪽, y는 아래로 증가한다.
 * 음수 좌표도 허용한다.
 */
public record Point(double x, double y) {

    public Point {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("point must be finite: " + x + ", " + y);
        }
    }
}
