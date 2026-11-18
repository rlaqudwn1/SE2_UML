package uml.view.classdiagram;

import java.util.Objects;
import uml.model.ElementId;
import uml.model.StyleColor;
import uml.view.Point;

/**
 * 사용자가 마우스로 한 편집 한 건. 편집기는 이것만 내보내고 장면·저장·이력을 직접 바꾸지 않는다.
 *
 * <ul>
 *   <li>마우스를 뗄 때 한 번만 보낸다. 드래그 중 움직임은 보내지 않는다 → 편집 1건 = Undo 1단계.</li>
 *   <li>좌표는 절대 좌표다. 이동량(오프셋)이 아니다.</li>
 *   <li>UML 의미(클래스 이름, 관계 종류)는 바꾸지 않는다. 위치·꺾임점·색만 다룬다.</li>
 *   <li>받는 쪽(배치·저장·Undo 담당)이 반영한 뒤 새 {@link ClassScene}을 편집기에 다시 넘긴다.</li>
 * </ul>
 */
public sealed interface ClassEditIntent {

    /** 박스를 옮김. topLeft는 스냅이 적용된 왼쪽 위 좌표이며 사용자 지정 위치로 저장할 값이다. */
    record MoveClass(ElementId classId, Point topLeft) implements ClassEditIntent {
        public MoveClass {
            Objects.requireNonNull(classId, "classId");
            Objects.requireNonNull(topLeft, "topLeft");
        }
    }

    /** 선 중간에 꺾임점 추가(FR-10). index는 waypoints 안에 들어갈 위치다. */
    record AddWaypoint(ElementId edgeId, int index, Point position) implements ClassEditIntent {
        public AddWaypoint {
            Objects.requireNonNull(edgeId, "edgeId");
            nonNegative(index);
            Objects.requireNonNull(position, "position");
        }
    }

    /** 기존 꺾임점을 드래그(FR-9, FR-10). */
    record MoveWaypoint(ElementId edgeId, int index, Point position) implements ClassEditIntent {
        public MoveWaypoint {
            Objects.requireNonNull(edgeId, "edgeId");
            nonNegative(index);
            Objects.requireNonNull(position, "position");
        }
    }

    record RemoveWaypoint(ElementId edgeId, int index) implements ClassEditIntent {
        public RemoveWaypoint {
            Objects.requireNonNull(edgeId, "edgeId");
            nonNegative(index);
        }
    }

    /** 클래스 박스 색 변경(FR-7). */
    record SetBoxColor(ElementId classId, StyleColor color) implements ClassEditIntent {
        public SetBoxColor {
            Objects.requireNonNull(classId, "classId");
            Objects.requireNonNull(color, "color");
        }
    }

    /** 편집 결과를 받는 쪽(배치·저장·Undo 담당)이 구현한다. */
    @FunctionalInterface
    interface Listener {
        void onEdit(ClassEditIntent intent);
    }

    private static void nonNegative(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index must not be negative: " + index);
        }
    }
}
