package uml.model.sequence;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import uml.model.DiagramModel;
import uml.model.ElementId;

/**
 * 시퀀스 다이어그램의 의미 모델. 실행 순서는 {@code order}로만 표현하고 좌표는 갖지 않는다.
 * 불변식을 만족하지 않으면 생성할 수 없다.
 */
public record SequenceDiagramModel(
        List<Lifeline> lifelines,
        List<Message> messages,
        List<ActivationBar> activations,
        List<Note> notes) implements DiagramModel {

    public SequenceDiagramModel {
        lifelines = List.copyOf(lifelines);
        messages = List.copyOf(messages);
        activations = List.copyOf(activations);
        notes = List.copyOf(notes);
        List<Violation> violations = check(lifelines, messages, activations, notes);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(violations.getFirst().message());
        }
    }

    /** 생성하지 않고 모든 위반을 모아 돌려준다. 위반이 없으면 빈 목록. 목록과 원소는 null이 아니어야 한다. */
    public static List<Violation> check(
            List<Lifeline> lifelines,
            List<Message> messages,
            List<ActivationBar> activations,
            List<Note> notes) {
        Objects.requireNonNull(lifelines, "lifelines");
        Objects.requireNonNull(messages, "messages");
        Objects.requireNonNull(activations, "activations");
        Objects.requireNonNull(notes, "notes");

        List<Violation> out = new ArrayList<>();
        checkUniqueIds(lifelines, messages, activations, notes, out);

        Set<ElementId> lifelineIds = new HashSet<>();
        Set<Integer> lifelineOrders = new HashSet<>();
        for (Lifeline l : lifelines) {
            lifelineIds.add(l.id());
            if (!lifelineOrders.add(l.order())) {
                out.add(new Violation("duplicate lifeline order: " + l.order()));
            }
        }

        Set<Integer> messageOrders = new HashSet<>();
        Set<Integer> usedOrders = new HashSet<>();
        for (Message m : messages) {
            requireLifeline(lifelineIds, m.from(), "message " + m.id().value() + " from", out);
            requireLifeline(lifelineIds, m.to(), "message " + m.id().value() + " to", out);
            messageOrders.add(m.order());
            if (!usedOrders.add(m.order())) {
                out.add(new Violation("duplicate order: " + m.order()));
            }
        }
        for (Note n : notes) {
            for (ElementId ref : n.lifelines()) {
                requireLifeline(lifelineIds, ref, "note " + n.id().value(), out);
            }
            if (!usedOrders.add(n.order())) {
                out.add(new Violation("duplicate order: " + n.order()));
            }
        }
        for (ActivationBar a : activations) {
            String where = "activation " + a.id().value();
            requireLifeline(lifelineIds, a.lifelineId(), where, out);
            if (!messageOrders.contains(a.startOrder())) {
                out.add(new Violation(where + " startOrder is not a message order: " + a.startOrder()));
            }
            if (!messageOrders.contains(a.endOrder())) {
                out.add(new Violation(where + " endOrder is not a message order: " + a.endOrder()));
            }
        }
        return List.copyOf(out);
    }

    private static void checkUniqueIds(
            List<Lifeline> lifelines, List<Message> messages,
            List<ActivationBar> activations, List<Note> notes, List<Violation> out) {
        Set<ElementId> seen = new HashSet<>();
        lifelines.forEach(e -> addId(seen, e.id(), out));
        messages.forEach(e -> addId(seen, e.id(), out));
        activations.forEach(e -> addId(seen, e.id(), out));
        notes.forEach(e -> addId(seen, e.id(), out));
    }

    private static void addId(Set<ElementId> seen, ElementId id, List<Violation> out) {
        if (!seen.add(id)) {
            out.add(new Violation("duplicate element id: " + id.value()));
        }
    }

    private static void requireLifeline(
            Set<ElementId> lifelineIds, ElementId ref, String where, List<Violation> out) {
        if (!lifelineIds.contains(ref)) {
            out.add(new Violation(where + " references unknown lifeline: " + ref.value()));
        }
    }
}
