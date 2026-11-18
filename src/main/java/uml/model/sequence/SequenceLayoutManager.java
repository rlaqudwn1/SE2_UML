package uml.model.sequence;

/** order와 {@link SequenceLayoutHints}에서 화면 위치를 계산한다. */
public interface SequenceLayoutManager {

    SequenceGeometry compute(SequenceDiagramModel model, SequenceLayoutHints hints);
}
