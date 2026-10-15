package uml.view.classdiagram;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import uml.model.ElementId;

/**
 * 렌더러가 한 번 그리는 데 필요한 전부. 위치는 배치 담당이 정한 값이며 렌더러는 위치를 정하지 않는다.
 * 의미 모델이나 저장할 사용자 위치와 별개인 "그리기용 스냅샷"이다.
 *
 * <p>boxes의 순서가 그리는 순서다. 뒤에 있는 박스가 위에 그려지고 클릭 판정도 우선한다.
 * 불변식을 만족하지 않으면 생성할 수 없다.
 */
public record ClassScene(List<BoxView> boxes, List<EdgeView> edges) {

    public static final ClassScene EMPTY = new ClassScene(List.of(), List.of());

    public ClassScene {
        boxes = List.copyOf(boxes);
        edges = List.copyOf(edges);
        List<Violation> violations = check(boxes, edges);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(violations.getFirst().message());
        }
    }

    /** 생성하지 않고 모든 위반을 모아 돌려준다. 위반이 없으면 빈 목록. */
    public static List<Violation> check(List<BoxView> boxes, List<EdgeView> edges) {
        Objects.requireNonNull(boxes, "boxes");
        Objects.requireNonNull(edges, "edges");
        List<Violation> out = new ArrayList<>();
        Set<ElementId> seen = new HashSet<>();
        Set<ElementId> boxIds = new HashSet<>();
        for (BoxView b : boxes) {
            addId(seen, b.id(), out);
            boxIds.add(b.id());
        }
        for (EdgeView e : edges) {
            addId(seen, e.id(), out);
            requireBox(boxIds, e.source(), "edge " + e.id().value() + " source", out);
            requireBox(boxIds, e.target(), "edge " + e.id().value() + " target", out);
        }
        return List.copyOf(out);
    }

    public Optional<BoxView> box(ElementId id) {
        return boxes.stream().filter(b -> b.id().equals(id)).findFirst();
    }

    public Optional<EdgeView> edge(ElementId id) {
        return edges.stream().filter(e -> e.id().equals(id)).findFirst();
    }

    private static void addId(Set<ElementId> seen, ElementId id, List<Violation> out) {
        if (!seen.add(id)) {
            out.add(new Violation("duplicate element id: " + id.value()));
        }
    }

    private static void requireBox(Set<ElementId> boxIds, ElementId ref, String where, List<Violation> out) {
        if (!boxIds.contains(ref)) {
            out.add(new Violation(where + " references unknown class: " + ref.value()));
        }
    }
}
