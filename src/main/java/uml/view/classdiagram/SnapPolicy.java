package uml.view.classdiagram;

import java.util.List;
import java.util.Objects;
import uml.view.Bounds;
import uml.view.Point;

/**
 * 드래그 중인 박스를 격자나 다른 박스의 가장자리·중심선에 맞춘다(FR-2).
 * 드래그하는 동안 매번 호출되며, 결과는 미리보기와 놓을 때의 최종 위치에 쓴다.
 */
public interface SnapPolicy {

    /**
     * @param moving 마우스를 따라 옮긴, 스냅 전 박스 경계
     * @param others 움직이지 않는 다른 박스들의 경계
     */
    SnapResult snap(Bounds moving, List<Bounds> others, SnapOptions options);

    /**
     * @param gridSize  격자 간격. 0이면 격자 스냅을 끈다.
     * @param threshold 이 거리 이내면 다른 박스에 맞춘다. 0이면 정렬 스냅을 끈다.
     */
    record SnapOptions(double gridSize, double threshold) {

        public static final SnapOptions OFF = new SnapOptions(0, 0);

        public SnapOptions {
            if (!Double.isFinite(gridSize) || gridSize < 0) {
                throw new IllegalArgumentException("gridSize must not be negative: " + gridSize);
            }
            if (!Double.isFinite(threshold) || threshold < 0) {
                throw new IllegalArgumentException("threshold must not be negative: " + threshold);
            }
        }
    }

    enum Orientation {
        /** x = coordinate인 세로 안내선. */
        VERTICAL,
        /** y = coordinate인 가로 안내선. */
        HORIZONTAL
    }

    record Guide(Orientation orientation, double coordinate) {
        public Guide {
            Objects.requireNonNull(orientation, "orientation");
        }
    }

    /** topLeft는 스냅이 적용된 왼쪽 위 좌표, guides는 드래그 중 화면에 그릴 안내선. */
    record SnapResult(Point topLeft, List<Guide> guides) {
        public SnapResult {
            Objects.requireNonNull(topLeft, "topLeft");
            guides = List.copyOf(guides);
        }
    }
}
