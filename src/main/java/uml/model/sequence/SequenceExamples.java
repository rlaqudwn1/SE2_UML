package uml.model.sequence;

import java.util.List;
import uml.model.ElementId;
import uml.model.Style;

/** 조사 노트의 Alice/Bob 예제(loop 제외)를 모델로 옮긴 예시 데이터. */
public final class SequenceExamples {

    private SequenceExamples() {
    }

    public static SequenceDiagramModel aliceAndBob() {
        ElementId alice = ElementId.of("lifeline-alice");
        ElementId bob = ElementId.of("lifeline-bob");
        Style s = Style.DEFAULT;
        return new SequenceDiagramModel(
                List.of(
                        new Lifeline(alice, "Alice", 1, s),
                        new Lifeline(bob, "Bob", 2, s)),
                List.of(
                        new Message(ElementId.of("msg-1"), alice, bob, 1, "요청", MessageType.SYNC, s),
                        new Message(ElementId.of("msg-2"), bob, bob, 2, "자기 호출", MessageType.RETURN, s),
                        new Message(ElementId.of("msg-3"), bob, alice, 3, "응답", MessageType.SYNC, s),
                        new Message(ElementId.of("msg-4"), bob, alice, 5, "완료", MessageType.RETURN, s)),
                List.of(new ActivationBar(ElementId.of("act-1"), bob, 1, 5, s)),
                List.of(new Note(ElementId.of("note-1"), 4, NotePlacement.OVER,
                        List.of(alice, bob), "메모", s)));
    }
}
