package uml.model.sequence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import uml.model.ElementId;
import uml.model.Style;
import uml.model.StyleColor;

class SequenceDiagramModelTest {

    private static final Style S = Style.DEFAULT;
    private static final ElementId A = ElementId.of("a");
    private static final ElementId B = ElementId.of("b");

    private static Lifeline la() { return new Lifeline(A, "A", 1, S); }
    private static Lifeline lb() { return new Lifeline(B, "B", 2, S); }
    private static Message msg(String id, ElementId from, ElementId to, int order) {
        return new Message(ElementId.of(id), from, to, order, "t", MessageType.SYNC, S);
    }

    private static String rejection(List<Lifeline> l, List<Message> m, List<ActivationBar> a, List<Note> n) {
        return assertThrows(IllegalArgumentException.class,
                () -> new SequenceDiagramModel(l, m, a, n)).getMessage();
    }

    @Test
    void exampleHasExpectedShape() {
        SequenceDiagramModel m = SequenceExamples.aliceAndBob();
        assertEquals(2, m.lifelines().size());
        assertEquals(4, m.messages().size());
        assertEquals(1, m.activations().size());
        assertEquals(1, m.notes().size());
        assertTrue(m.messages().get(1).isSelfCall());
        assertTrue(SequenceDiagramModel.check(
                m.lifelines(), m.messages(), m.activations(), m.notes()).isEmpty());
    }

    @Test
    void duplicateElementIdIsRejected() {
        String msg = rejection(List.of(la(), lb()), List.of(msg("a", A, B, 1)), List.of(), List.of());
        assertEquals("duplicate element id: a", msg);
    }

    @Test
    void duplicateLifelineOrderIsRejected() {
        String msg = rejection(List.of(la(), new Lifeline(B, "B", 1, S)), List.of(), List.of(), List.of());
        assertEquals("duplicate lifeline order: 1", msg);
    }

    @Test
    void unknownLifelineInMessageIsRejected() {
        String msg = rejection(List.of(la()), List.of(msg("m1", A, ElementId.of("ghost"), 1)),
                List.of(), List.of());
        assertEquals("message m1 to references unknown lifeline: ghost", msg);
    }

    @Test
    void unknownLifelineInNoteIsRejected() {
        Note note = new Note(ElementId.of("n"), 1, NotePlacement.LEFT_OF,
                List.of(ElementId.of("ghost")), "x", S);
        String msg = rejection(List.of(la()), List.of(), List.of(), List.of(note));
        assertEquals("note n references unknown lifeline: ghost", msg);
    }

    @Test
    void duplicateOrderBetweenMessagesIsRejected() {
        String msg = rejection(List.of(la(), lb()),
                List.of(msg("m1", A, B, 1), msg("m2", B, A, 1)), List.of(), List.of());
        assertEquals("duplicate order: 1", msg);
    }

    @Test
    void duplicateOrderBetweenMessageAndNoteIsRejected() {
        Note note = new Note(ElementId.of("n"), 1, NotePlacement.LEFT_OF, List.of(A), "x", S);
        String msg = rejection(List.of(la(), lb()), List.of(msg("m1", A, B, 1)), List.of(), List.of(note));
        assertEquals("duplicate order: 1", msg);
    }

    @Test
    void activationOrdersMustBeMessageOrders() {
        List<Lifeline> ls = List.of(la(), lb());
        List<Message> ms = List.of(msg("m1", A, B, 2));
        assertEquals("activation x startOrder is not a message order: 1",
                rejection(ls, ms, List.of(new ActivationBar(ElementId.of("x"), B, 1, 2, S)), List.of()));
        assertEquals("activation x endOrder is not a message order: 9",
                rejection(ls, ms, List.of(new ActivationBar(ElementId.of("x"), B, 2, 9, S)), List.of()));
    }

    @Test
    void activationOnUnknownLifelineIsRejected() {
        String msg = rejection(List.of(la()), List.of(msg("m1", A, A, 1)),
                List.of(new ActivationBar(ElementId.of("x"), B, 1, 1, S)), List.of());
        assertEquals("activation x references unknown lifeline: b", msg);
    }

    @Test
    void checkCollectsAllViolationsWithoutThrowing() {
        List<Violation> v = SequenceDiagramModel.check(
                List.of(la(), la()),
                List.of(msg("m1", A, ElementId.of("ghost"), 1)),
                List.of(), List.of());
        assertEquals(List.of(
                new Violation("duplicate element id: a"),
                new Violation("duplicate lifeline order: 1"),
                new Violation("message m1 to references unknown lifeline: ghost")), v);
    }

    @Test
    void valueObjectsRejectInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> new ElementId(" "));
        assertThrows(IllegalArgumentException.class, () -> new Style(StyleColor.RED, 0));
        assertThrows(IllegalArgumentException.class, () -> new Lifeline(A, "A", 0, S));
        assertThrows(IllegalArgumentException.class, () -> new Lifeline(A, " ", 1, S));
        assertThrows(IllegalArgumentException.class, () -> msg("m", A, B, 0));
        assertThrows(IllegalArgumentException.class, () -> new ActivationBar(ElementId.of("x"), B, 0, 1, S));
        assertThrows(IllegalArgumentException.class, () -> new ActivationBar(ElementId.of("x"), B, 5, 1, S));
        assertThrows(NullPointerException.class, () -> new Message(
                ElementId.of("m"), A, B, 1, null, MessageType.SYNC, S));
    }

    @Test
    void noteLifelinesDependOnPlacementAndMustBeDistinct() {
        assertThrows(IllegalArgumentException.class, () ->
                new Note(ElementId.of("n"), 1, NotePlacement.LEFT_OF, List.of(A, B), "x", S));
        assertThrows(IllegalArgumentException.class, () ->
                new Note(ElementId.of("n"), 1, NotePlacement.OVER, List.of(), "x", S));
        assertThrows(IllegalArgumentException.class, () ->
                new Note(ElementId.of("n"), 1, NotePlacement.OVER, List.of(A, A), "x", S));
    }

    @Test
    void modelCopiesEverySourceList() {
        List<Lifeline> lifelines = new ArrayList<>(List.of(la(), lb()));
        List<Message> messages = new ArrayList<>(List.of(msg("m1", A, B, 1)));
        List<ActivationBar> activations = new ArrayList<>(List.of(new ActivationBar(ElementId.of("act"), B, 1, 1, S)));
        List<ElementId> noteLifelines = new ArrayList<>(List.of(A));
        List<Note> notes = new ArrayList<>(List.of(
                new Note(ElementId.of("n"), 2, NotePlacement.LEFT_OF, noteLifelines, "x", S)));

        SequenceDiagramModel m = new SequenceDiagramModel(lifelines, messages, activations, notes);

        lifelines.clear();
        messages.clear();
        activations.clear();
        notes.clear();
        noteLifelines.add(B);

        assertEquals(2, m.lifelines().size());
        assertEquals(1, m.messages().size());
        assertEquals(1, m.activations().size());
        assertEquals(1, m.notes().size());
        assertEquals(List.of(A), m.notes().getFirst().lifelines());
    }

    @Test
    void exposedListsAreUnmodifiable() {
        SequenceDiagramModel m = new SequenceDiagramModel(
                new ArrayList<>(List.of(la(), lb())), new ArrayList<>(List.of(msg("m1", A, B, 1))),
                new ArrayList<>(), new ArrayList<>());
        assertThrows(UnsupportedOperationException.class, () -> m.lifelines().clear());
        assertThrows(UnsupportedOperationException.class, () -> m.messages().clear());
        assertThrows(UnsupportedOperationException.class, () -> m.activations().add(null));
        assertThrows(UnsupportedOperationException.class, () -> m.notes().add(null));
    }

    @Test
    void layoutHintsCopyTheirMaps() {
        Map<ElementId, Integer> gaps = new HashMap<>(Map.of(A, 10));
        Map<ElementId, Integer> msgGaps = new HashMap<>(Map.of(B, 5));
        SequenceLayoutHints hints = new SequenceLayoutHints(gaps, msgGaps);
        gaps.put(B, 20);
        msgGaps.clear();
        assertEquals(Map.of(A, 10), hints.lifelineGapAdjust());
        assertEquals(Map.of(B, 5), hints.messageGapAdjust());
    }

    @Test
    void geometryCopiesItsMap() {
        Map<ElementId, SequenceGeometry.Bounds> map = new HashMap<>();
        map.put(A, new SequenceGeometry.Bounds(0, 0, 10, 10));
        SequenceGeometry g = new SequenceGeometry(map);
        map.clear();
        assertEquals(1, g.bounds().size());
        assertSame(g.bounds(), g.bounds());
    }

    @Test
    void semanticModelComponentsMatchAllowlist() {
        Map<Class<?>, Set<String>> allowed = Map.of(
                Lifeline.class, Set.of("id", "participantName", "order", "style"),
                Message.class, Set.of("id", "from", "to", "order", "text", "type", "style"),
                ActivationBar.class, Set.of("id", "lifelineId", "startOrder", "endOrder", "style"),
                Note.class, Set.of("id", "order", "placement", "lifelines", "text", "style"),
                SequenceDiagramModel.class, Set.of("lifelines", "messages", "activations", "notes"),
                ElementId.class, Set.of("value"),
                Style.class, Set.of("color", "fontSize"));
        allowed.forEach((type, names) -> {
            Set<String> actual = new java.util.HashSet<>();
            for (RecordComponent c : type.getRecordComponents()) {
                actual.add(c.getName());
            }
            assertEquals(names, actual, type.getSimpleName());
        });
    }
}
