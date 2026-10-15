package uml.view.classdiagram;

import java.util.Objects;
import uml.model.ElementId;
import uml.view.Point;

/**
 * 클릭한 점 아래에 무엇이 있는지 찾는다(히트 테스트).
 *
 * <p>우선순위 제안: 선택된 선의 꺾임점 → 박스(위에 그려진 것 우선) → 관계선 → 빈 곳.
 * 선은 가늘어서 tolerance(월드 좌표 단위) 이내면 선 위로 본다.
 */
public interface ClassHitTester {

    Hit hitTest(ClassScene scene, Point point, double tolerance);

    /** 클릭 결과. switch로 종류별 처리를 빠짐없이 하도록 sealed로 둔다. */
    sealed interface Hit permits Nothing, BoxHit, EdgeHit, WaypointHit {
    }

    record Nothing() implements Hit {
    }

    record BoxHit(ElementId boxId) implements Hit {
        public BoxHit {
            Objects.requireNonNull(boxId, "boxId");
        }
    }

    /**
     * 선 위를 클릭함. segmentIndex는 경로의 몇 번째 구간인지(0부터)이며,
     * 여기에 꺾임점을 추가하면 {@code waypoints}의 segmentIndex 위치에 들어간다.
     */
    record EdgeHit(ElementId edgeId, int segmentIndex) implements Hit {
        public EdgeHit {
            Objects.requireNonNull(edgeId, "edgeId");
            if (segmentIndex < 0) {
                throw new IllegalArgumentException("segmentIndex must not be negative: " + segmentIndex);
            }
        }
    }

    /** 기존 꺾임점을 잡음. index는 {@code EdgeView.waypoints} 안의 위치. */
    record WaypointHit(ElementId edgeId, int index) implements Hit {
        public WaypointHit {
            Objects.requireNonNull(edgeId, "edgeId");
            if (index < 0) {
                throw new IllegalArgumentException("index must not be negative: " + index);
            }
        }
    }
}
