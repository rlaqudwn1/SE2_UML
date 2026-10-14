package org.se2uml.layout;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Java 25 contract for a sequence diagram's placement algorithm.
 *
 * <p>This interface does not implement an algorithm or define UML syntax, messages,
 * lifelines, or a semantic diagram model. An adapter owned by the model/rendering
 * team supplies opaque IDs, measured sizes, horizontal anchor references, and
 * already ordered rows. Text measurement and self-call shape measurement belong
 * to that adapter; repeated participant IDs in a row are allowed.
 *
 * <p>Implementations must preserve participant left-to-right order and row
 * top-to-bottom order. Manual hints are separate from the ordered input and
 * never change semantic order. Apply feasible hints, preserve pinned coordinates
 * exactly, and report an ERROR when pins make a valid placement impossible.
 * Unpinned hints may move to satisfy spacing, with a POSITION_ADJUSTED WARNING
 * targeting the moved element. See {@link SequenceLayoutValidator} for numeric
 * tolerance and independent request/result validation.
 * Implementations must not silently reorder input or ignore an infeasible pin.
 *
 * <p>Coordinates use canvas units, with x increasing rightwards and y downwards.
 * Boxes use their top-left corner. Participant x hints refer to box left edges;
 * row y hints refer to row top edges. Row anchors are supplied in the same order
 * as the row's anchor specifications, including duplicate participants. Their x
 * coordinate is the referenced participant's horizontal center; their y
 * coordinate is the row top plus the measured offsetY. Rendering interprets these
 * anchors without this contract assigning UML meaning to them.
 */
public interface SequenceLayoutEngine {
    /** Computes geometry without mutating input, persisting state, or rendering. */
    LayoutResult layout(LayoutRequest request);

    record ParticipantBox(String id, double width, double height) {
        public ParticipantBox {
            requireId(id);
            positive(width, "width");
            positive(height, "height");
        }
    }

    /**
     * Measured anchor at a participant center. Extents reserve horizontal occupancy
     * on each side of that center; offsetY is measured downwards from the row top.
     * The adapter supplies these numbers without exposing semantic element kinds.
     */
    record AnchorSpec(String participantId, double offsetY, double leftExtent,
                      double rightExtent) {
        public AnchorSpec {
            requireId(participantId);
            nonNegative(offsetY, "offsetY");
            nonNegative(leftExtent, "leftExtent");
            nonNegative(rightExtent, "rightExtent");
        }
    }

    /** Minimum absolute horizontal distance between two indices in a row's anchors. */
    record MinimumSpan(int firstAnchorIndex, int secondAnchorIndex, double minDistance) {
        public MinimumSpan {
            if (firstAnchorIndex < 0 || secondAnchorIndex < 0
                    || firstAnchorIndex == secondAnchorIndex) {
                throw new IllegalArgumentException("Span indices must be nonnegative and distinct");
            }
            nonNegative(minDistance, "minDistance");
        }
    }

    /**
     * Generic measured row. Width is the minimum total bounds width, independent
     * of minimumSpans; height is the minimum bounds height. Duplicate participants
     * and different offsets permit measured self-call shapes. Each anchor's full
     * horizontal occupancy must fit inside the final row bounds.
     */
    record LayoutRow(String id, List<AnchorSpec> anchors, double width, double height,
                     List<MinimumSpan> minimumSpans) {
        public LayoutRow {
            requireId(id);
            anchors = List.copyOf(anchors);
            minimumSpans = List.copyOf(minimumSpans);
            positive(width, "width");
            positive(height, "height");
            for (var anchor : anchors) {
                if (anchor.offsetY() > height) {
                    throw new IllegalArgumentException("Anchor offsetY exceeds measured row height");
                }
            }
            for (var span : minimumSpans) {
                if (span.firstAnchorIndex() >= anchors.size()
                        || span.secondAnchorIndex() >= anchors.size()) {
                    throw new IllegalArgumentException("Span index exceeds row anchor count");
                }
            }
        }
    }

    /** A preferred coordinate, or an exact coordinate when pinned is true. */
    record PositionHint(double coordinate, boolean pinned) {
        public PositionHint { finite(coordinate, "coordinate"); }
    }

    /** Persist these hints independently from the teammate-owned diagram model. */
    record ManualLayout(Map<String, PositionHint> participantX, Map<String, PositionHint> rowY) {
        public ManualLayout {
            participantX = Map.copyOf(participantX);
            rowY = Map.copyOf(rowY);
            participantX.keySet().forEach(SequenceLayoutEngine::requireId);
            rowY.keySet().forEach(SequenceLayoutEngine::requireId);
        }

        public static ManualLayout empty() { return new ManualLayout(Map.of(), Map.of()); }
    }

    /** Minimum clear space between boxes; margin is required around canvas content. */
    record LayoutOptions(double participantGap, double rowGap, double headerGap, double margin) {
        public LayoutOptions {
            nonNegative(participantGap, "participantGap");
            nonNegative(rowGap, "rowGap");
            nonNegative(headerGap, "headerGap");
            nonNegative(margin, "margin");
        }
    }

    /** Caller-owned adapter input, not a sequence diagram semantic model. */
    record LayoutRequest(List<ParticipantBox> participants, List<LayoutRow> rows,
                         ManualLayout manualLayout, LayoutOptions options) {
        public LayoutRequest {
            participants = List.copyOf(participants);
            rows = List.copyOf(rows);
            Objects.requireNonNull(manualLayout, "manualLayout");
            Objects.requireNonNull(options, "options");
            var participantIds = new HashSet<String>();
            for (var participant : participants) {
                if (!participantIds.add(participant.id())) {
                    throw new IllegalArgumentException("Duplicate participant ID: " + participant.id());
                }
            }
            var rowIds = new HashSet<String>();
            for (var row : rows) {
                if (!rowIds.add(row.id())) {
                    throw new IllegalArgumentException("Duplicate row ID: " + row.id());
                }
                requireKnown(row.anchors().stream().map(AnchorSpec::participantId).toList(),
                        participantIds, "row participant");
            }
            requireKnown(manualLayout.participantX().keySet(), participantIds, "participant hint");
            requireKnown(manualLayout.rowY().keySet(), rowIds, "row hint");
        }
    }

    record Point(double x, double y) {
        public Point {
            finite(x, "x");
            finite(y, "y");
        }
    }

    /** Zero size is allowed for an empty canvas, but all extents must remain finite. */
    record Bounds(double x, double y, double width, double height) {
        public Bounds {
            finite(x, "x");
            finite(y, "y");
            nonNegative(width, "width");
            nonNegative(height, "height");
            finite(x + width, "right edge");
            finite(y + height, "bottom edge");
        }
    }

    enum Severity { WARNING, ERROR }

    enum ElementKind { PARTICIPANT, ROW }

    /** IDs are scoped by kind: a participant and a row may share the same ID. */
    record ElementRef(ElementKind kind, String id) {
        public ElementRef {
            Objects.requireNonNull(kind, "kind");
            requireId(id);
        }
    }

    /** target is optional: null denotes a diagram-wide diagnostic. */
    record Diagnostic(Severity severity, String code, String message, ElementRef target) {
        public Diagnostic {
            Objects.requireNonNull(severity, "severity");
            requireId(code);
            requireId(message);
        }
    }

    /**
     * Final geometry. A successful result contains exactly one box per input ID
     * and one anchor list per row, with the input reference count and order.
     * Participant boxes retain their measured size. Row bounds must be at least
     * their measured size, contain their anchors and asymmetric extents, and satisfy
     * minimumSpans. Anchor y is row y plus offsetY. All boxes/anchors fit in
     * canvasBounds and obey LayoutOptions spacing: participantGap horizontally,
     * rowGap vertically, headerGap between the lowest participant box and the
     * first row, and margin around all content. Maps do not carry ordering;
     * callers use the ordered request lists and look up geometry by ID.
     * With ERROR diagnostics geometry may be partial and must not be treated as
     * a valid placement. Empty input may return empty maps and a zero-size canvas.
     */
    record LayoutResult(Map<String, Bounds> participantBounds, Map<String, Bounds> rowBounds,
                        Map<String, List<Point>> rowAnchors, Bounds canvasBounds,
                        List<Diagnostic> diagnostics) {
        public LayoutResult {
            participantBounds = Map.copyOf(participantBounds);
            rowBounds = Map.copyOf(rowBounds);
            participantBounds.keySet().forEach(SequenceLayoutEngine::requireId);
            rowBounds.keySet().forEach(SequenceLayoutEngine::requireId);
            var copiedAnchors = new java.util.HashMap<String, List<Point>>();
            rowAnchors.forEach((id, points) -> {
                requireId(id);
                copiedAnchors.put(id, List.copyOf(points));
            });
            rowAnchors = Map.copyOf(copiedAnchors);
            Objects.requireNonNull(canvasBounds, "canvasBounds");
            diagnostics = List.copyOf(diagnostics);
        }

        /** Only inspects diagnostic severity; it does not validate geometry. */
        public boolean hasErrors() {
            return diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == Severity.ERROR);
        }
    }

    private static void requireId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ID or text must not be blank");
        }
    }

    private static void finite(double value, String name) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }

    private static void positive(double value, String name) {
        finite(value, name);
        if (value <= 0) throw new IllegalArgumentException(name + " must be positive");
    }

    private static void nonNegative(double value, String name) {
        finite(value, name);
        if (value < 0) throw new IllegalArgumentException(name + " must not be negative");
    }

    private static void requireKnown(Iterable<String> references, java.util.Set<String> ids,
                                     String name) {
        for (var reference : references) {
            if (!ids.contains(reference)) {
                throw new IllegalArgumentException("Unknown " + name + " ID: " + reference);
            }
        }
    }
}
