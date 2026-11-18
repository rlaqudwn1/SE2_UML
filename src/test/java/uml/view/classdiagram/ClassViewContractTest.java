package uml.view.classdiagram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import uml.model.ElementId;
import uml.model.Style;
import uml.view.Bounds;
import uml.view.FontSpec;
import uml.view.Point;

/** 인터페이스 계약(입력 검증·불변성·편집 결과 종류)만 확인한다. 그리기·스냅 알고리즘은 아직 없다. */
class ClassViewContractTest {

    private static final Style S = Style.DEFAULT;
    private static final ElementId A = ElementId.of("a");
    private static final ElementId B = ElementId.of("b");

    private static BoxView box(ElementId id) {
        return new BoxView(id, BoxContent.of(id.value(), List.of(), List.of()), new Bounds(0, 0, 60, 30), S, false);
    }

    private static EdgeView edge(String id, ElementId source, ElementId target, List<Point> waypoints) {
        return new EdgeView(ElementId.of(id), source, target, LineStyle.SOLID, EndMarker.NONE, EndMarker.NONE,
                waypoints, "", "", "", S, false);
    }

    @Test
    void exampleSceneIsValid() {
        ClassScene scene = ClassViewExamples.university();
        assertEquals(4, scene.boxes().size());
        assertEquals(4, scene.edges().size());
        assertTrue(ClassScene.check(scene.boxes(), scene.edges()).isEmpty());
        assertTrue(scene.edge(ClassViewExamples.PREREQUISITE).orElseThrow().isSelfLoop());
    }

    @Test
    void duplicateIdIsRejectedAcrossBoxesAndEdges() {
        String msg = assertThrows(IllegalArgumentException.class, () -> new ClassScene(
                List.of(box(A), box(B)), List.of(edge("a", A, B, List.of())))).getMessage();
        assertEquals("duplicate element id: a", msg);
    }

    @Test
    void edgeToUnknownClassIsRejected() {
        String msg = assertThrows(IllegalArgumentException.class, () -> new ClassScene(
                List.of(box(A)), List.of(edge("e", A, ElementId.of("ghost"), List.of())))).getMessage();
        assertEquals("edge e target references unknown class: ghost", msg);
    }

    @Test
    void checkCollectsAllViolationsWithoutThrowing() {
        List<Violation> v = ClassScene.check(
                List.of(box(A), box(A)),
                List.of(edge("e", ElementId.of("x"), ElementId.of("y"), List.of())));
        assertEquals(List.of(
                new Violation("duplicate element id: a"),
                new Violation("edge e source references unknown class: x"),
                new Violation("edge e target references unknown class: y")), v);
    }

    @Test
    void sceneCopiesListsAndExposesUnmodifiableViews() {
        List<BoxView> boxes = new ArrayList<>(List.of(box(A), box(B)));
        List<Point> waypoints = new ArrayList<>(List.of(new Point(15, 30)));
        ClassScene scene = new ClassScene(boxes, List.of(edge("e", A, B, waypoints)));
        boxes.clear();
        waypoints.clear();
        assertEquals(2, scene.boxes().size());
        assertEquals(1, scene.edges().getFirst().waypoints().size());
        assertThrows(UnsupportedOperationException.class, () -> scene.boxes().clear());
        assertThrows(UnsupportedOperationException.class, () -> scene.edges().getFirst().waypoints().clear());
    }

    @Test
    void valuesRejectInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> new Point(Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> new Bounds(0, 0, -1, 10));
        assertThrows(IllegalArgumentException.class, () -> new FontSpec(0, false, false));
        assertThrows(IllegalArgumentException.class, () -> BoxContent.of(" ", List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> MemberLine.of(""));
        assertThrows(IllegalArgumentException.class, () -> new BoxMetrics(100, 60, 20, 20, 30));
        assertThrows(IllegalArgumentException.class, () -> new SnapPolicy.SnapOptions(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ClassHitTester.EdgeHit(A, -1));
        assertThrows(IllegalArgumentException.class, () -> new ClassEditIntent.RemoveWaypoint(A, -1));
        assertThrows(NullPointerException.class, () -> new BoxView(A, null, new Bounds(0, 0, 1, 1), S, false));
    }

    @Test
    void intentKindsAreFixedSoUndoHandlesEveryCase() {
        List<String> kinds = new ArrayList<>();
        for (Class<?> k : ClassEditIntent.class.getPermittedSubclasses()) {
            kinds.add(k.getSimpleName());
        }
        kinds.sort(null);
        assertEquals(List.of("AddWaypoint", "MoveClass", "MoveWaypoint", "RemoveWaypoint", "SetBoxColor"), kinds);
    }

    @Test
    void listenerReceivesIntentsInOrder() {
        List<ClassEditIntent> received = new ArrayList<>();
        ClassEditIntent.Listener listener = received::add;
        listener.onEdit(new ClassEditIntent.MoveClass(A, new Point(-20, 40)));
        listener.onEdit(new ClassEditIntent.AddWaypoint(ElementId.of("e"), 0, new Point(1, 1)));
        assertEquals(2, received.size());
        assertTrue(received.getFirst() instanceof ClassEditIntent.MoveClass);
    }
}
