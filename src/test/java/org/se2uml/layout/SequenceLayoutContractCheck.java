package org.se2uml.layout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.se2uml.layout.SequenceLayoutEngine.*;

/** Executable contract checks using hand-written geometry, never a layout algorithm. */
public final class SequenceLayoutContractCheck {
    private static final LayoutOptions OPTIONS = new LayoutOptions(20, 10, 15, 5);
    private static final ParticipantBox FIRST = new ParticipantBox("first", 80, 30);
    private static final ParticipantBox SECOND = new ParticipantBox("second", 90, 40);
    private static final LayoutRow ROW = new LayoutRow("row", List.of(
            new AnchorSpec("first", 10, 15, 10), new AnchorSpec("second", 30, 20, 15)),
            120, 40, List.of(new MinimumSpan(0, 1, 150)));
    private static final LayoutRow SELF = new LayoutRow("self", List.of(
            new AnchorSpec("first", 5, 0, 30), new AnchorSpec("first", 15, 0, 30)),
            80, 20, List.of(new MinimumSpan(0, 1, 0)));

    public static void main(String[] args) {
        requestSnapshotsPreserveOrderAndHints();
        inputValidationRejectsInvalidGeometryAndReferences();
        resultSnapshotsAreDeeplyImmutable();
        successfulGeometryIsCheckedAgainstItsRequest();
        diagnosticsHaveTypedTargetsAndFailureStatus();
        System.out.println("Sequence layout contract checks passed");
    }

    private static LayoutRequest fixtureRequest() {
        return request(List.of(FIRST, SECOND), List.of(ROW, SELF),
                new ManualLayout(Map.of("first", new PositionHint(20, true)),
                        Map.of("row", new PositionHint(90, true))));
    }

    private static LayoutResult fixtureResult() {
        return new LayoutResult(Map.of("first", new Bounds(20, 20, 80, 30),
                        "second", new Bounds(180, 20, 90, 40)),
                Map.of("row", new Bounds(45, 90, 195, 40), "self", new Bounds(60, 140, 80, 20)),
                Map.of("row", List.of(new Point(60, 100), new Point(225, 120)),
                        "self", List.of(new Point(60, 145), new Point(60, 155))),
                new Bounds(0, 0, 300, 180), List.of());
    }

    private static void requestSnapshotsPreserveOrderAndHints() {
        var anchors = new ArrayList<>(SELF.anchors());
        var spans = new ArrayList<>(SELF.minimumSpans());
        var self = new LayoutRow("self", anchors, 80, 20, spans);
        anchors.clear();
        spans.clear();
        check(self.anchors().equals(SELF.anchors()), "Repeated participant references and offsets must survive");
        check(self.minimumSpans().equals(SELF.minimumSpans()), "Span constraints must be snapshots");
        var participants = new ArrayList<>(List.of(SECOND, FIRST));
        var rows = new ArrayList<>(List.of(self, ROW));
        var px = new HashMap<>(Map.of("second", new PositionHint(-120, true)));
        var ry = new HashMap<>(Map.of("self", new PositionHint(240, false)));
        var manual = new ManualLayout(px, ry);
        var input = request(participants, rows, manual);
        participants.clear(); rows.clear(); px.clear(); ry.clear();
        check(input.participants().equals(List.of(SECOND, FIRST)), "Participant order must survive");
        check(input.rows().equals(List.of(self, ROW)), "Row order must survive");
        check(manual.participantX().get("second").equals(new PositionHint(-120, true)), "Pin must survive");
        check(manual.rowY().get("self").equals(new PositionHint(240, false)), "Preference must survive");
        expectUnsupported(() -> input.participants().clear());
        expectUnsupported(() -> input.rows().clear());
        expectUnsupported(() -> self.anchors().clear());
        expectUnsupported(() -> self.minimumSpans().clear());
        expectUnsupported(() -> manual.participantX().clear());
        expectUnsupported(() -> manual.rowY().clear());
    }

    private static void inputValidationRejectsInvalidGeometryAndReferences() {
        for (var invalid : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            expectInvalid(() -> new Point(invalid, 0));
            expectInvalid(() -> new Point(0, invalid));
            expectInvalid(() -> new PositionHint(invalid, true));
            expectInvalid(() -> new ParticipantBox("p", invalid, 1));
            expectInvalid(() -> new AnchorSpec("p", invalid, 0, 0));
            expectInvalid(() -> new AnchorSpec("p", 0, invalid, 0));
            expectInvalid(() -> new AnchorSpec("p", 0, 0, invalid));
            expectInvalid(() -> new MinimumSpan(0, 1, invalid));
            expectInvalid(() -> new Bounds(0, 0, invalid, 1));
            expectInvalid(() -> new LayoutOptions(1, invalid, 1, 1));
        }
        expectInvalid(() -> new ParticipantBox(" ", 1, 1));
        expectInvalid(() -> new ParticipantBox("p", 0, 1));
        expectInvalid(() -> new ParticipantBox("p", 1, -1));
        expectInvalid(() -> new AnchorSpec("", 0, 0, 0));
        expectInvalid(() -> new AnchorSpec("p", -1, 0, 0));
        expectInvalid(() -> new AnchorSpec("p", 0, -1, 0));
        expectInvalid(() -> new AnchorSpec("p", 0, 0, -1));
        expectInvalid(() -> new MinimumSpan(-1, 0, 0));
        expectInvalid(() -> new MinimumSpan(0, 1, -1));
        expectInvalid(() -> new LayoutRow("r", List.of(new AnchorSpec("first", 21, 0, 0)), 1, 20, List.of()));
        expectInvalid(() -> new LayoutRow("r", ROW.anchors(), 1, 40, List.of(new MinimumSpan(0, 2, 1))));
        expectInvalid(() -> new Bounds(0, 0, 1, -1));
        expectInvalid(() -> new Bounds(Double.MAX_VALUE, 0, Double.MAX_VALUE, 1));
        expectInvalid(() -> new LayoutOptions(-1, 0, 0, 0));
        expectInvalid(() -> request(List.of(FIRST, FIRST), List.of(), ManualLayout.empty()));
        expectInvalid(() -> request(List.of(FIRST, SECOND), List.of(ROW, ROW), ManualLayout.empty()));
        expectInvalid(() -> request(List.of(FIRST), List.of(ROW), ManualLayout.empty()));
        expectInvalid(() -> request(List.of(FIRST), List.of(),
                new ManualLayout(Map.of("missing", new PositionHint(0, true)), Map.of())));
        expectInvalid(() -> request(List.of(FIRST), List.of(),
                new ManualLayout(Map.of(), Map.of("missing", new PositionHint(0, true)))));
        // Participant and row IDs deliberately have separate namespaces.
        request(List.of(FIRST), List.of(new LayoutRow("first", List.of(), 1, 1, List.of())), ManualLayout.empty());
        new Bounds(-10, -20, 0, 0);
    }

    private static void resultSnapshotsAreDeeplyImmutable() {
        var base = fixtureResult();
        var participants = new HashMap<>(base.participantBounds());
        var rows = new HashMap<>(base.rowBounds());
        var points = new ArrayList<>(base.rowAnchors().get("row"));
        var anchors = new HashMap<>(base.rowAnchors());
        anchors.put("row", points);
        var diagnostics = new ArrayList<Diagnostic>();
        var result = new LayoutResult(participants, rows, anchors, base.canvasBounds(), diagnostics);
        participants.clear(); rows.clear(); points.clear(); anchors.clear();
        diagnostics.add(new Diagnostic(Severity.ERROR, "changed", "Caller mutation", null));
        check(result.equals(base), "Result and nested lists must be detached snapshots");
        expectUnsupported(() -> result.participantBounds().clear());
        expectUnsupported(() -> result.rowBounds().clear());
        expectUnsupported(() -> result.rowAnchors().clear());
        expectUnsupported(() -> result.rowAnchors().get("row").clear());
        expectUnsupported(() -> result.diagnostics().clear());
    }

    private static void successfulGeometryIsCheckedAgainstItsRequest() {
        var input = fixtureRequest();
        var good = fixtureResult();
        valid(input, good, "Asymmetric extents, different Y offsets and repeated self anchors are valid");
        valid(request(List.of(), List.of(), ManualLayout.empty()),
                emptyResult(List.of()), "Empty input accepts empty canvas");
        invalid(input, emptyResult(List.of()), "Empty geometry cannot claim success for nonempty input", "PARTICIPANT_IDS");
        var p = new HashMap<>(good.participantBounds());
        p.remove("first"); invalid(input, geometry(good, p, good.rowBounds(), good.rowAnchors(), good.canvasBounds()), "Missing ID", "PARTICIPANT_IDS");
        p = new HashMap<>(good.participantBounds()); p.put("extra", new Bounds(0, 0, 1, 1));
        invalid(input, geometry(good, p, good.rowBounds(), good.rowAnchors(), good.canvasBounds()), "Extra ID", "PARTICIPANT_IDS");
        p = new HashMap<>(good.participantBounds()); p.put("first", new Bounds(20, 20, 81, 30));
        invalid(input, geometry(good, p, good.rowBounds(), good.rowAnchors(), good.canvasBounds()), "Participant size", "PARTICIPANT_SIZE");
        p = new HashMap<>(good.participantBounds()); p.put("first", new Bounds(21, 20, 80, 30));
        invalid(input, geometry(good, p, good.rowBounds(), good.rowAnchors(), good.canvasBounds()), "Exact pin", "PIN_MOVED");
        p = new HashMap<>(good.participantBounds()); p.put("first", new Bounds(20 + 1e-8, 20, 80, 30));
        invalid(input, geometry(good, p, good.rowBounds(), good.rowAnchors(), good.canvasBounds()),
                "Pins remain exact below geometry comparison tolerance", "PIN_MOVED");
        p = new HashMap<>(good.participantBounds()); p.put("second", new Bounds(100, 20, 90, 40));
        invalid(input, geometry(good, p, good.rowBounds(), good.rowAnchors(), good.canvasBounds()), "Participant spacing", "PARTICIPANT_SPACING");
        var r = new HashMap<>(good.rowBounds()); r.remove("self");
        invalid(input, geometry(good, good.participantBounds(), r, good.rowAnchors(), good.canvasBounds()),
                "Missing row ID", "ROW_IDS");
        r = new HashMap<>(good.rowBounds()); r.put("extra", new Bounds(0, 0, 1, 1));
        invalid(input, geometry(good, good.participantBounds(), r, good.rowAnchors(), good.canvasBounds()),
                "Extra row ID", "ROW_IDS");
        r = new HashMap<>(good.rowBounds()); r.put("self", new Bounds(60, 125, 80, 20));
        invalid(input, geometry(good, good.participantBounds(), r, good.rowAnchors(), good.canvasBounds()), "Row spacing", "ROW_SPACING");
        r = new HashMap<>(good.rowBounds()); r.put("row", new Bounds(45, 60, 195, 40));
        invalid(input, geometry(good, good.participantBounds(), r, good.rowAnchors(), good.canvasBounds()), "Header spacing and row pin", "HEADER_GAP");
        r = new HashMap<>(good.rowBounds()); r.put("self", new Bounds(60, 140, 79, 20));
        invalid(input, geometry(good, good.participantBounds(), r, good.rowAnchors(), good.canvasBounds()), "Measured row minimum size", "ROW_SIZE");
        r = new HashMap<>(good.rowBounds()); r.put("row", new Bounds(50, 90, 190, 40));
        invalid(input, geometry(good, good.participantBounds(), r, good.rowAnchors(), good.canvasBounds()), "Left extent crosses row boundary", "ANCHOR_EXTENTS");
        r = new HashMap<>(good.rowBounds()); r.put("row", new Bounds(45, 90, 190, 40));
        invalid(input, geometry(good, good.participantBounds(), r, good.rowAnchors(), good.canvasBounds()), "Right extent crosses row boundary", "ANCHOR_EXTENTS");
        var a = new HashMap<>(good.rowAnchors()); a.put("row", List.of(new Point(60, 100)));
        invalid(input, geometry(good, good.participantBounds(), good.rowBounds(), a, good.canvasBounds()), "Anchor count", "ANCHOR_COUNT");
        a = new HashMap<>(good.rowAnchors()); a.put("row", List.of(new Point(225, 100), new Point(60, 120)));
        invalid(input, geometry(good, good.participantBounds(), good.rowBounds(), a, good.canvasBounds()), "Anchor reference order", "ANCHOR_X");
        a = new HashMap<>(good.rowAnchors()); a.put("row", List.of(new Point(60, 101), new Point(225, 120)));
        invalid(input, geometry(good, good.participantBounds(), good.rowBounds(), a, good.canvasBounds()), "Anchor Y offset", "ANCHOR_Y");
        a = new HashMap<>(good.rowAnchors()); a.remove("self");
        invalid(input, geometry(good, good.participantBounds(), good.rowBounds(), a, good.canvasBounds()), "Anchor map ID set", "ANCHOR_IDS");
        var wider = new LayoutRow("row", ROW.anchors(), 120, 40, List.of(new MinimumSpan(0, 1, 170)));
        invalid(request(input.participants(), List.of(wider, SELF), input.manualLayout()), good, "Minimum anchor span", "MINIMUM_SPAN");
        invalid(input, geometry(good, good.participantBounds(), good.rowBounds(), good.rowAnchors(), new Bounds(0, 0, 270, 160)), "Canvas margin", "CANVAS_MARGIN");
        var preferred = request(input.participants(), input.rows(), new ManualLayout(
                Map.of("first", new PositionHint(19, false)), input.manualLayout().rowY()));
        invalid(preferred, good, "Adjusted preference requires matching warning", "PREFERRED_WARNING_MISSING");
        var warning = new Diagnostic(Severity.WARNING, "POSITION_ADJUSTED", "Preference adjusted",
                new ElementRef(ElementKind.PARTICIPANT, "first"));
        valid(preferred, new LayoutResult(good.participantBounds(), good.rowBounds(), good.rowAnchors(),
                good.canvasBounds(), List.of(warning)), "Typed adjustment warning permits moved preference");
    }

    private static void diagnosticsHaveTypedTargetsAndFailureStatus() {
        var participant = new ElementRef(ElementKind.PARTICIPANT, "first");
        var row = new ElementRef(ElementKind.ROW, "first");
        check(!participant.equals(row), "Same ID in different namespaces must identify distinct elements");
        var warning = new Diagnostic(Severity.WARNING, "POSITION_ADJUSTED", "Preferred position adjusted", participant);
        var error = new Diagnostic(Severity.ERROR, "PIN_CONFLICT", "Pinned positions conflict", row);
        check(!emptyResult(List.of(warning)).hasErrors(), "Warnings do not mark failure");
        var partial = emptyResult(List.of(error));
        check(partial.hasErrors(), "ERROR result must be treated as failed even with empty geometry");
        check(!SequenceLayoutValidator.validate(fixtureRequest(), partial).isEmpty(), "Failure must never validate as success");
        var good = fixtureResult();
        var wrongNamespace = new Diagnostic(Severity.WARNING, "note", "Unknown row target", row);
        invalid(fixtureRequest(), new LayoutResult(good.participantBounds(), good.rowBounds(), good.rowAnchors(),
                good.canvasBounds(), List.of(wrongNamespace)), "Diagnostic target uses namespace, not ID alone", "DIAGNOSTIC_TARGET");
        expectInvalid(() -> new ElementRef(ElementKind.ROW, " "));
        expectInvalid(() -> new Diagnostic(Severity.ERROR, "", "Message", null));
        expectInvalid(() -> new Diagnostic(Severity.ERROR, "code", " ", null));
    }

    private static LayoutRequest request(List<ParticipantBox> participants, List<LayoutRow> rows, ManualLayout manual) {
        return new LayoutRequest(participants, rows, manual, OPTIONS);
    }

    private static LayoutResult emptyResult(List<Diagnostic> diagnostics) {
        return new LayoutResult(Map.of(), Map.of(), Map.of(), new Bounds(0, 0, 0, 0), diagnostics);
    }

    private static LayoutResult geometry(LayoutResult base, Map<String, Bounds> participants,
            Map<String, Bounds> rows, Map<String, List<Point>> anchors, Bounds canvas) {
        return new LayoutResult(participants, rows, anchors, canvas, base.diagnostics());
    }

    private static void valid(LayoutRequest input, LayoutResult result, String message) {
        var errors = SequenceLayoutValidator.validate(input, result);
        check(errors.isEmpty(), message + ": " + errors);
    }

    private static void invalid(LayoutRequest input, LayoutResult result, String message, String expectedCode) {
        var errors = SequenceLayoutValidator.validate(input, result);
        check(!errors.isEmpty() && errors.stream().allMatch(d -> d.severity() == Severity.ERROR), message);
        check(errors.stream().anyMatch(d -> d.code().equals(expectedCode)),
                message + " must report " + expectedCode + ": " + errors);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectInvalid(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    private static void expectUnsupported(Runnable action) {
        try { action.run(); } catch (UnsupportedOperationException expected) { return; }
        throw new AssertionError("Expected immutable collection");
    }
}
