package uml.model.sequence;

import java.util.Map;
import uml.model.ElementId;

/** order와 보정값에서 계산한 화면 위치. 파생값이므로 저장하지 않는다. */
public record SequenceGeometry(Map<ElementId, Bounds> bounds) {

    public SequenceGeometry {
        bounds = Map.copyOf(bounds);
    }

    public record Bounds(int x, int y, int width, int height) {
    }
}
