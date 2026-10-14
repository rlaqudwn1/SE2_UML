package uml.model;

/** 다이어그램의 모든 요소가 공유하는 식별자와 스타일. 화면 좌표는 포함하지 않는다. */
public interface DiagramElement {

    ElementId id();

    Style style();
}
