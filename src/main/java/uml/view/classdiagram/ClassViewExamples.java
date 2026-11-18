package uml.view.classdiagram;

import java.util.List;
import uml.model.ElementId;
import uml.model.Style;
import uml.view.Bounds;
import uml.view.Point;

/**
 * 팀원과 인터페이스를 맞춰볼 때 쓰는 예시 장면. 아래 Mermaid 코드를 어댑터가 변환했다고 가정한다.
 *
 * <pre>
 * classDiagram
 *     class Person { &lt;&lt;abstract&gt;&gt; #String name; +getName() String }
 *     Person &lt;|-- Student
 *     Student "0..*" --&gt; "1..*" Course : 수강
 *     University *-- Course
 *     Course --&gt; Course : 선수과목
 * </pre>
 *
 * 위치와 크기는 배치·측정 결과라고 가정한 고정값이다.
 */
public final class ClassViewExamples {

    public static final ElementId PERSON = ElementId.of("class-person");
    public static final ElementId STUDENT = ElementId.of("class-student");
    public static final ElementId COURSE = ElementId.of("class-course");
    public static final ElementId UNIVERSITY = ElementId.of("class-university");
    public static final ElementId INHERIT = ElementId.of("rel-student-person");
    public static final ElementId ENROLL = ElementId.of("rel-student-course");
    public static final ElementId COMPOSE = ElementId.of("rel-university-course");
    public static final ElementId PREREQUISITE = ElementId.of("rel-course-course");

    private ClassViewExamples() {
    }

    public static ClassScene university() {
        Style s = Style.DEFAULT;
        BoxContent person = new BoxContent("Person", List.of(), true,
                List.of(MemberLine.of("# name: String")),
                List.of(MemberLine.of("+ getName(): String")));
        BoxContent student = BoxContent.of("Student",
                List.of(MemberLine.of("- studentId: int")),
                List.of(MemberLine.of("+ enroll(c: Course): void"),
                        new MemberLine("+ count(): int", true, false)));
        BoxContent course = BoxContent.of("Course", List.of(MemberLine.of("- title: String")), List.of());
        BoxContent university = BoxContent.of("University", List.of(),
                List.of(MemberLine.of("+ open(c: Course): void")));

        return new ClassScene(
                List.of(
                        new BoxView(PERSON, person, new Bounds(40, 40, 168, 84), s, false),
                        new BoxView(STUDENT, student, new Bounds(40, 240, 216, 104), s, false),
                        new BoxView(UNIVERSITY, university, new Bounds(420, 40, 208, 64), s, false),
                        new BoxView(COURSE, course, new Bounds(420, 240, 136, 64), s, false)),
                List.of(
                        edge(INHERIT, STUDENT, PERSON, LineStyle.SOLID, EndMarker.NONE, EndMarker.HOLLOW_TRIANGLE,
                                List.of(), "", "", "", s),
                        edge(ENROLL, STUDENT, COURSE, LineStyle.SOLID, EndMarker.NONE, EndMarker.OPEN_ARROW,
                                List.of(new Point(320, 330)), "0..*", "1..*", "수강", s),
                        edge(COMPOSE, UNIVERSITY, COURSE, LineStyle.SOLID, EndMarker.FILLED_DIAMOND, EndMarker.NONE,
                                List.of(), "", "", "", s),
                        edge(PREREQUISITE, COURSE, COURSE, LineStyle.SOLID, EndMarker.NONE, EndMarker.OPEN_ARROW,
                                List.of(), "", "", "선수과목", s)));
    }

    private static EdgeView edge(ElementId id, ElementId source, ElementId target, LineStyle line,
                                 EndMarker sourceEnd, EndMarker targetEnd, List<Point> waypoints,
                                 String sourceLabel, String targetLabel, String label, Style s) {
        return new EdgeView(id, source, target, line, sourceEnd, targetEnd,
                waypoints, sourceLabel, targetLabel, label, s, false);
    }
}
