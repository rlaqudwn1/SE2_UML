package uml.view;

import java.util.List;

/**
 * 렌더러가 그리는 바탕. 화면(Swing/JavaFX)과 PNG·JPG 내보내기가 각각 구현한다.
 * 렌더러가 이 인터페이스에만 그리면 화면과 내보낸 이미지가 같은 코드로 그려진다.
 * 좌표는 월드 좌표이며 확대·스크롤 변환은 구현 쪽 책임이다. 색은 "#RRGGBB" 문자열이다.
 */
public interface Canvas2D {

    void fillRect(Bounds bounds, String color);

    void strokeRect(Bounds bounds, String color, double lineWidth);

    void strokePolyline(List<Point> points, String color, double lineWidth, boolean dashed);

    void fillPolygon(List<Point> points, String color);

    void strokePolygon(List<Point> points, String color, double lineWidth);

    /** baselineLeft는 글자 기준선의 왼쪽 끝이다. */
    void drawText(String text, Point baselineLeft, FontSpec font, String color, boolean underline);
}
