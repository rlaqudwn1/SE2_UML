package org.se2uml.layout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.se2uml.layout.SequenceLayoutEngine.AnchorSpec;
import org.se2uml.layout.SequenceLayoutEngine.Bounds;
import org.se2uml.layout.SequenceLayoutEngine.Diagnostic;
import org.se2uml.layout.SequenceLayoutEngine.ElementKind;
import org.se2uml.layout.SequenceLayoutEngine.ElementRef;
import org.se2uml.layout.SequenceLayoutEngine.LayoutRequest;
import org.se2uml.layout.SequenceLayoutEngine.LayoutResult;
import org.se2uml.layout.SequenceLayoutEngine.PositionHint;
import org.se2uml.layout.SequenceLayoutEngine.Severity;

/**
 * Checks a placement against its measured request; does not compute a placement.
 * An empty list means the supplied successful result satisfies this contract.
 * If the result already contains ERROR diagnostics, those errors are returned
 * without success-geometry checks: partial error geometry is permitted, but is
 * never certified as valid. This checker cannot establish that a rejected request
 * was infeasible; that requires an engine or a separate feasibility proof.
 *
 * <p>Comparisons use an absolute tolerance of {@value #EPSILON} canvas units,
 * except pins, which require exact numeric equality (positive and negative zero
 * are equivalent). A preferred coordinate changed by more than that tolerance
 * requires a WARNING with code POSITION_ADJUSTED and the same typed target.
 * Header boxes need not share a y coordinate; headerGap is measured from the
 * lowest header bottom. No automatic origin or optimality policy is imposed.
 */
public final class SequenceLayoutValidator {
    public static final double EPSILON = 1e-6;

    private SequenceLayoutValidator() { }

    /** Returns immutable ERROR diagnostics for violations or existing error results. */
    public static List<Diagnostic> validate(LayoutRequest request, LayoutResult result) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(result, "result");
        if (result.hasErrors()) {
            return result.diagnostics().stream()
                    .filter(diagnostic -> diagnostic.severity() == Severity.ERROR).toList();
        }
        var failures = new ArrayList<Diagnostic>();
        var participantIds = new HashSet<String>();
        request.participants().forEach(participant -> participantIds.add(participant.id()));
        var rowIds = new HashSet<String>();
        request.rows().forEach(row -> rowIds.add(row.id()));
        checkIds(participantIds, result.participantBounds().keySet(), "PARTICIPANT_IDS", failures);
        checkIds(rowIds, result.rowBounds().keySet(), "ROW_IDS", failures);
        checkIds(rowIds, result.rowAnchors().keySet(), "ANCHOR_IDS", failures);
        for (var diagnostic : result.diagnostics()) {
            var target = diagnostic.target();
            if (target != null && !(target.kind() == ElementKind.PARTICIPANT
                    ? participantIds : rowIds).contains(target.id())) {
                fail(failures, "DIAGNOSTIC_TARGET", "Diagnostic target is absent from request", target);
            }
        }

        Bounds previousParticipant = null;
        double lowestHeader = Double.NEGATIVE_INFINITY;
        for (var participant : request.participants()) {
            var target = new ElementRef(ElementKind.PARTICIPANT, participant.id());
            var bounds = result.participantBounds().get(participant.id());
            if (bounds == null) continue; // IDs are reported above; keep inspecting available geometry.
            if (!close(bounds.width(), participant.width())
                    || !close(bounds.height(), participant.height())) {
                fail(failures, "PARTICIPANT_SIZE", "Participant measured size changed", target);
            }
            if (previousParticipant != null && !atLeast(bounds.x(),
                    right(previousParticipant) + request.options().participantGap())) {
                fail(failures, "PARTICIPANT_SPACING", "Participant order or gap violated", target);
            }
            previousParticipant = bounds;
            lowestHeader = Math.max(lowestHeader, bottom(bounds));
            checkHint(request.manualLayout().participantX().get(participant.id()), bounds.x(),
                    target, result.diagnostics(), failures);
        }

        Bounds previousRow = null;
        for (var row : request.rows()) {
            var target = new ElementRef(ElementKind.ROW, row.id());
            var bounds = result.rowBounds().get(row.id());
            var points = result.rowAnchors().get(row.id());
            if (points != null && points.size() != row.anchors().size()) {
                fail(failures, "ANCHOR_COUNT", "Anchor count differs from measured input", target);
            }
            if (bounds != null) {
                if (!atLeast(bounds.width(), row.width()) || !atLeast(bounds.height(), row.height())) {
                    fail(failures, "ROW_SIZE", "Row is smaller than measured minimum bounds", target);
                }
                if (previousRow != null && !atLeast(bounds.y(),
                        bottom(previousRow) + request.options().rowGap())) {
                    fail(failures, "ROW_SPACING", "Row order or gap violated", target);
                }
                if (previousRow == null && lowestHeader != Double.NEGATIVE_INFINITY
                        && !atLeast(bounds.y(), lowestHeader + request.options().headerGap())) {
                    fail(failures, "HEADER_GAP", "First row violates header gap", target);
                }
                previousRow = bounds;
                checkHint(request.manualLayout().rowY().get(row.id()), bounds.y(), target,
                        result.diagnostics(), failures);
            }
            if (points == null) continue;
            for (int index = 0; index < Math.min(points.size(), row.anchors().size()); index++) {
                AnchorSpec spec = row.anchors().get(index);
                var point = points.get(index);
                var participant = result.participantBounds().get(spec.participantId());
                if (participant != null && !close(point.x(), participant.x() + participant.width() / 2)) {
                    fail(failures, "ANCHOR_X", "Anchor " + index + " is not at participant center", target);
                }
                if (!insideCanvas(point.x() - spec.leftExtent(), point.y(),
                        point.x() + spec.rightExtent(), point.y(), request, result)) {
                    fail(failures, "CANVAS_MARGIN", "Measured anchor occupancy does not fit canvas with margin", target);
                }
                if (bounds != null) {
                    if (!close(point.y(), bounds.y() + spec.offsetY())) {
                        fail(failures, "ANCHOR_Y", "Anchor " + index + " changed its measured y offset", target);
                    }
                    if (!atLeast(point.x() - spec.leftExtent(), bounds.x())
                            || !atLeast(right(bounds), point.x() + spec.rightExtent())
                            || !atLeast(point.y(), bounds.y()) || !atLeast(bottom(bounds), point.y())) {
                        fail(failures, "ANCHOR_EXTENTS", "Anchor " + index + " occupancy exceeds row bounds", target);
                    }
                }
            }
            for (var span : row.minimumSpans()) {
                if (span.firstAnchorIndex() < points.size() && span.secondAnchorIndex() < points.size()
                        && !atLeast(Math.abs(points.get(span.firstAnchorIndex()).x()
                                - points.get(span.secondAnchorIndex()).x()), span.minDistance())) {
                    fail(failures, "MINIMUM_SPAN", "Anchor pair is closer than measured minimum distance", target);
                }
            }
        }
        checkCanvas(result.participantBounds(), ElementKind.PARTICIPANT, request, result, failures);
        checkCanvas(result.rowBounds(), ElementKind.ROW, request, result, failures);
        // Check points independently too, including unmatched/extra anchor lists.
        result.rowAnchors().forEach((id, points) -> {
            for (var point : points) {
                if (!insideCanvas(point.x(), point.y(), point.x(), point.y(), request, result)) {
                    fail(failures, "CANVAS_MARGIN", "Anchor does not fit canvas with margin",
                            new ElementRef(ElementKind.ROW, id));
                }
            }
        });
        return List.copyOf(failures);
    }

    private static void checkIds(Set<String> expected, Set<String> actual, String code,
                                 List<Diagnostic> failures) {
        if (!expected.equals(actual)) fail(failures, code, "Output IDs differ from request IDs", null);
    }

    private static void checkHint(PositionHint hint, double coordinate, ElementRef target,
                                  List<Diagnostic> diagnostics, List<Diagnostic> failures) {
        if (hint == null) return;
        if (hint.pinned()) {
            if (coordinate != hint.coordinate()) {
                fail(failures, "PIN_MOVED", "Pinned coordinate must be preserved exactly", target);
            }
        } else if (!close(coordinate, hint.coordinate()) && diagnostics.stream().noneMatch(diagnostic ->
                diagnostic.severity() == Severity.WARNING && diagnostic.code().equals("POSITION_ADJUSTED")
                        && target.equals(diagnostic.target()))) {
            fail(failures, "PREFERRED_WARNING_MISSING", "Moved preference requires POSITION_ADJUSTED warning", target);
        }
    }

    private static void checkCanvas(Map<String, Bounds> boxes, ElementKind kind, LayoutRequest request,
                                    LayoutResult result, List<Diagnostic> failures) {
        boxes.forEach((id, bounds) -> {
            if (!insideCanvas(bounds.x(), bounds.y(), right(bounds), bottom(bounds), request, result)) {
                fail(failures, "CANVAS_MARGIN", "Bounds do not fit canvas with margin", new ElementRef(kind, id));
            }
        });
    }

    private static boolean insideCanvas(double left, double top, double right, double bottom,
                                        LayoutRequest request, LayoutResult result) {
        var canvas = result.canvasBounds();
        double margin = request.options().margin();
        return atLeast(left, canvas.x() + margin) && atLeast(top, canvas.y() + margin)
                && atLeast(right(canvas) - margin, right) && atLeast(bottom(canvas) - margin, bottom);
    }

    private static double right(Bounds bounds) { return bounds.x() + bounds.width(); }
    private static double bottom(Bounds bounds) { return bounds.y() + bounds.height(); }
    private static boolean close(double actual, double expected) {
        return Double.isFinite(actual) && Double.isFinite(expected) && Math.abs(actual - expected) <= EPSILON;
    }
    private static boolean atLeast(double actual, double minimum) {
        // Reject arithmetic overflow as invalid geometry, including Infinity >= Infinity.
        return Double.isFinite(actual) && Double.isFinite(minimum) && actual - minimum >= -EPSILON;
    }
    private static void fail(List<Diagnostic> failures, String code, String message, ElementRef target) {
        failures.add(new Diagnostic(Severity.ERROR, code, message, target));
    }
}
