package uml.view.classdiagram;

/**
 * 관계선 끝 모양. 렌더러는 UML 관계 종류를 해석하지 않고 이 모양만 그린다.
 * 관계 종류 → 모양 변환은 어댑터 한 곳에서 한다(예: 상속 → 부모 쪽 HOLLOW_TRIANGLE).
 */
public enum EndMarker {
    NONE,
    OPEN_ARROW,
    HOLLOW_TRIANGLE,
    HOLLOW_DIAMOND,
    FILLED_DIAMOND
}
