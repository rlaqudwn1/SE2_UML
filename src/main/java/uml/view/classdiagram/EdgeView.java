package uml.view.classdiagram;

import java.util.List;
import java.util.Objects;
import uml.model.ElementId;
import uml.model.Style;
import uml.view.Point;

/**
 * 화면에 그릴 관계선 하나. 자기 자신과의 관계(source == target)도 허용한다.
 *
 * @param waypoints   중간 꺾임점(절대 좌표). 비어 있으면 직선이다. 양 끝점은 렌더러가
 *                    박스 테두리에서 계산하므로 넣지 않는다.
 * @param sourceLabel source 끝 근처 글자(다중성·역할). 없으면 "".
 * @param targetLabel target 끝 근처 글자. 없으면 "".
 * @param label       선 가운데 글자(관계 이름). 없으면 "".
 * @param selected    선택 표시 여부. 선택된 선만 꺾임점 손잡이를 보여준다.
 */
public record EdgeView(
        ElementId id, ElementId source, ElementId target,
        LineStyle line, EndMarker sourceEnd, EndMarker targetEnd,
        List<Point> waypoints, String sourceLabel, String targetLabel, String label,
        Style style, boolean selected) {

    public EdgeView {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(line, "line");
        Objects.requireNonNull(sourceEnd, "sourceEnd");
        Objects.requireNonNull(targetEnd, "targetEnd");
        waypoints = List.copyOf(waypoints);
        Objects.requireNonNull(sourceLabel, "sourceLabel");
        Objects.requireNonNull(targetLabel, "targetLabel");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(style, "style");
    }

    public boolean isSelfLoop() {
        return source.equals(target);
    }
}
