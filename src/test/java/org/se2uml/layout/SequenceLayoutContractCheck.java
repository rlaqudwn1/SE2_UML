package org.se2uml.layout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.se2uml.layout.SequenceLayoutEngine.*;

/** Executable DTO contract checks; no layout algorithm or semantic model is implemented here. */
public final class SequenceLayoutContractCheck {
    private static final LayoutOptions OPTIONS = new LayoutOptions(20, 10, 15, 5);
    private static final ParticipantBox FIRST = new ParticipantBox("first", 80, 30);
    private static final ParticipantBox SECOND = new ParticipantBox("second", 90, 40);
    private static final LayoutRow ROW = new LayoutRow("row", List.of("first", "second"), 120, 20);

    public static void main(String[] args) {
        requestSnapshotsPreserveOrderAndHints();
        inputValidationRejectsInvalidGeometryAndReferences();
        resultSnapshotsAreDeeplyImmutable();
        diagnosticsDistinguishWarningsAndErrors();
        System.out.println("Sequence layout contract checks passed");
    }

    private static void requestSnapshotsPreserveOrderAndHints() {
        var references = new ArrayList<>(List.of("second", "first", "second"));
        var selfRow = new LayoutRow("self", references, 60, 20);
        references.clear();
        check(selfRow.participantIds().equals(List.of("second", "first", "second")),
                "Anchor reference order and duplicates must survive snapshotting");

        var participants = new ArrayList<>(List.of(SECOND, FIRST));
        var rows = new ArrayList<>(List.of(selfRow, ROW));
        var participantHints = new HashMap<String, PositionHint>();
        var rowHints = new HashMap<String, PositionHint>();
        participantHints.put("second", new PositionHint(-120, true));
        rowHints.put("self", new PositionHint(240, false));
        var manual = new ManualLayout(participantHints, rowHints);
        var request = new LayoutRequest(participants, rows, manual, OPTIONS);
        participants.clear();
        rows.clear();
        participantHints.clear();
        rowHints.clear();

        check(request.participants().equals(List.of(SECOND, FIRST)), "Caller order must be retained");
        check(request.rows().equals(List.of(selfRow, ROW)), "Row order must be retained");
        check(request.manualLayout().participantX().get("second").equals(new PositionHint(-120, true)),
                "Pinned coordinates must remain exact in the request");
        check(request.manualLayout().rowY().get("self").equals(new PositionHint(240, false)),
                "Preferred coordinates must remain distinct from pins");
        expectUnsupported(() -> request.participants().clear());
        expectUnsupported(() -> request.rows().clear());
        expectUnsupported(() -> selfRow.participantIds().clear());
        expectUnsupported(() -> manual.participantX().clear());
        expectUnsupported(() -> manual.rowY().clear());
        check(ManualLayout.empty().participantX().isEmpty() && ManualLayout.empty().rowY().isEmpty(),
                "Empty manual layout must leave every element available for automatic placement");
        new LayoutRequest(List.of(), List.of(), ManualLayout.empty(), new LayoutOptions(0, 0, 0, 0));
    }

    private static void inputValidationRejectsInvalidGeometryAndReferences() {
        for (var invalid : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            expectInvalid(() -> new Point(invalid, 0));
            expectInvalid(() -> new Point(0, invalid));
            expectInvalid(() -> new PositionHint(invalid, true));
            expectInvalid(() -> new ParticipantBox("p", invalid, 1));
            expectInvalid(() -> new LayoutRow("r", List.of(), 1, invalid));
            expectInvalid(() -> new Bounds(0, 0, invalid, 1));
            expectInvalid(() -> new LayoutOptions(1, invalid, 1, 1));
        }
        expectInvalid(() -> new ParticipantBox(" ", 1, 1));
        expectInvalid(() -> new LayoutRow("r", List.of(""), 1, 1));
        expectInvalid(() -> new ParticipantBox("p", 0, 1));
        expectInvalid(() -> new ParticipantBox("p", 1, -1));
        expectInvalid(() -> new LayoutRow("r", List.of(), -1, 1));
        expectInvalid(() -> new Bounds(0, 0, 1, -1));
        expectInvalid(() -> new Bounds(Double.MAX_VALUE, 0, Double.MAX_VALUE, 1));
        expectInvalid(() -> new LayoutOptions(-1, 0, 0, 0));
        expectInvalid(() -> request(List.of(FIRST, FIRST), List.of(ROW), ManualLayout.empty()));
        expectInvalid(() -> request(List.of(FIRST, SECOND), List.of(ROW, ROW), ManualLayout.empty()));
        expectInvalid(() -> request(List.of(FIRST), List.of(ROW), ManualLayout.empty()));
        expectInvalid(() -> request(List.of(FIRST, SECOND), List.of(ROW),
                new ManualLayout(Map.of("missing", new PositionHint(0, true)), Map.of())));
        expectInvalid(() -> request(List.of(FIRST, SECOND), List.of(ROW),
                new ManualLayout(Map.of(), Map.of("missing", new PositionHint(0, true)))));
        // IDs have separate participant/row namespaces, and geometry may use negative coordinates.
        request(List.of(FIRST), List.of(new LayoutRow("first", List.of("first"), 1, 1)),
                new ManualLayout(Map.of("first", new PositionHint(-10, true)), Map.of()));
        new Bounds(-10, -20, 0, 0);
    }

    private static void resultSnapshotsAreDeeplyImmutable() {
        var participantBounds = new HashMap<>(Map.of(
                "first", new Bounds(10, 10, 80, 30), "second", new Bounds(155, 10, 90, 40)));
        var rowBounds = new HashMap<>(Map.of("row", new Bounds(10, 65, 235, 20)));
        var points = new ArrayList<>(List.of(new Point(50, 75), new Point(200, 75)));
        var anchors = new HashMap<String, List<Point>>();
        anchors.put("row", points);
        var diagnostics = new ArrayList<Diagnostic>();
        var result = new LayoutResult(participantBounds, rowBounds, anchors,
                new Bounds(0, 0, 250, 100), diagnostics);
        participantBounds.clear();
        rowBounds.clear();
        points.clear();
        anchors.clear();
        diagnostics.add(new Diagnostic(Severity.ERROR, "changed", "Caller mutation", null));
        check(result.participantBounds().size() == 2 && result.rowBounds().size() == 1,
                "Result bounds must be snapshots");
        check(result.rowAnchors().get("row").equals(List.of(new Point(50, 75), new Point(200, 75))),
                "Nested anchor lists must be snapshots preserving order");
        check(result.diagnostics().isEmpty(), "Diagnostics must be a snapshot");
        expectUnsupported(() -> result.participantBounds().clear());
        expectUnsupported(() -> result.rowBounds().clear());
        expectUnsupported(() -> result.rowAnchors().clear());
        expectUnsupported(() -> result.rowAnchors().get("row").clear());
        expectUnsupported(() -> result.diagnostics().clear());
    }

    private static void diagnosticsDistinguishWarningsAndErrors() {
        var warning = new Diagnostic(Severity.WARNING, "hint-adjusted", "Preferred position adjusted", "row");
        var error = new Diagnostic(Severity.ERROR, "pin-conflict", "Pinned positions conflict", null);
        check(!result(List.of(warning)).hasErrors(), "Warnings must not mark placement as failed");
        check(result(List.of(warning, error)).hasErrors(), "Errors must mark placement as failed");
        expectInvalid(() -> new Diagnostic(Severity.ERROR, "", "Message", null));
        expectInvalid(() -> new Diagnostic(Severity.ERROR, "code", " ", null));
        expectInvalid(() -> new Diagnostic(Severity.ERROR, "code", "Message", " "));
    }

    private static LayoutRequest request(List<ParticipantBox> participants, List<LayoutRow> rows,
                                         ManualLayout manual) {
        return new LayoutRequest(participants, rows, manual, OPTIONS);
    }

    private static LayoutResult result(List<Diagnostic> diagnostics) {
        return new LayoutResult(Map.of(), Map.of(), Map.of(), new Bounds(0, 0, 0, 0), diagnostics);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectInvalid(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    private static void expectUnsupported(Runnable action) {
        try { action.run(); }
        catch (UnsupportedOperationException expected) { return; }
        throw new AssertionError("Expected immutable collection");
    }
}
