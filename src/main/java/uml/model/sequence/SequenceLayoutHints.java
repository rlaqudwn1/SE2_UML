package uml.model.sequence;

import java.util.Map;
import uml.model.ElementId;

/**
 * 사용자가 조정한 간격 보정값. 좌표가 아니라 기본 배치에 더하는 값이며, 키는 요소 ID다.
 * 보정값이 없는 요소는 기본 간격을 쓴다.
 */
public record SequenceLayoutHints(
        Map<ElementId, Integer> lifelineGapAdjust,
        Map<ElementId, Integer> messageGapAdjust) {

    public static final SequenceLayoutHints NONE = new SequenceLayoutHints(Map.of(), Map.of());

    public SequenceLayoutHints {
        lifelineGapAdjust = Map.copyOf(lifelineGapAdjust);
        messageGapAdjust = Map.copyOf(messageGapAdjust);
    }
}
