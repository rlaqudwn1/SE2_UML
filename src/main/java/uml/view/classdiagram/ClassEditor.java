package uml.view.classdiagram;

import java.util.List;
import java.util.Optional;
import uml.model.ElementId;
import uml.model.StyleColor;
import uml.view.Point;

/**
 * 마우스 입력을 편집 결과({@link ClassEditIntent})로 바꾼다. UI 프레임워크와 무관한 경계다.
 *
 * <p>UI는 마우스 이벤트를 월드 좌표로 바꿔 press → drag → release 순서로 넘기고,
 * 다시 그릴 때 {@link #previewScene()}과 {@link #guides()}를 렌더러에 넘긴다.
 *
 * <p>데이터는 한 방향으로만 흐른다. 편집기는 받은 장면을 직접 고치지 않고 편집 결과만
 * {@link ClassEditIntent.Listener}로 내보낸다. 반영된 새 장면은 받는 쪽이 {@link #setScene}으로 돌려준다.
 */
public interface ClassEditor {

    /** 배치·저장 담당이 반영을 마친 새 장면. 없어진 요소의 선택은 지우고 진행 중 드래그는 취소한다. */
    void setScene(ClassScene scene);

    /** 누름. 박스나 선택된 선의 꺾임점이면 드래그를 시작하고, 선이면 선택한다. */
    void press(Point point);

    /** 끌기. 미리보기만 바꾸고 편집 결과는 보내지 않는다. */
    void drag(Point point);

    /** 뗌. 위치가 실제로 바뀐 경우에만 편집 결과를 한 번 보낸다. */
    void release(Point point);

    /** Esc 등으로 드래그 취소. 편집 결과를 보내지 않는다. */
    void cancel();

    /** 선 위를 두 번 누르면 꺾임점 추가, 선택된 선의 꺾임점이면 삭제. */
    void doubleClick(Point point);

    /** 팔레트에서 색 선택. 선택된 박스가 없거나 같은 색이면 보내지 않는다. */
    void changeColor(StyleColor color);

    Optional<ElementId> selectedBox();

    Optional<ElementId> selectedEdge();

    /** 선택 표시와 드래그 미리보기를 반영한, 지금 그려야 할 장면. */
    ClassScene previewScene();

    /** 드래그 중 그릴 정렬 안내선. 드래그 중이 아니면 비어 있다. */
    List<SnapPolicy.Guide> guides();
}
