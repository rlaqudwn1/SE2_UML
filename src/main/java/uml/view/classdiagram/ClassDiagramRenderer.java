package uml.view.classdiagram;

import java.util.List;
import uml.model.ElementId;
import uml.view.Bounds;
import uml.view.Canvas2D;
import uml.view.Point;

/**
 * 클래스 다이어그램을 {@link Canvas2D}에 그린다. 장면이 바뀌면 매번 전체를 다시 그린다.
 *
 * <p>렌더러가 하지 않는 일: 문법 해석, 위치 결정, 저장, 장면 변경.
 */
public interface ClassDiagramRenderer {

    /** 장면 전체를 그린다. 입력을 바꾸지 않는다. */
    void render(ClassScene scene, Canvas2D canvas);

    /**
     * 관계선의 실제 경로: [source 테두리 위 점, 꺾임점..., target 테두리 위 점].
     * 끝점은 박스 중심이 아니라 테두리에서 끝나야 화살표가 박스에 가려지지 않는다.
     * 클릭 판정과 라벨 위치도 이 경로를 쓴다.
     */
    List<Point> edgePath(ClassScene scene, ElementId edgeId);

    /** 박스·선·끝 모양·라벨을 모두 포함하는 경계. 이미지 내보내기 크기를 정할 때 쓴다. */
    Bounds contentBounds(ClassScene scene);
}
