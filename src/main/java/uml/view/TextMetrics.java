package uml.view;

/**
 * 문자열이 실제로 차지하는 크기를 잰다. 글자 수가 아니라 글꼴 기준이다(한글·영문, i·W의 폭이 다르다).
 * 화면과 이미지 내보내기가 같은 구현을 써야 박스 크기가 어긋나지 않는다. 시퀀스 렌더러도 쓸 수 있다.
 * 구현은 UI 프레임워크가 정해진 뒤 만든다.
 */
public interface TextMetrics {

    double width(String text, FontSpec font);

    /** 한 줄이 차지하는 높이(줄 간격 포함). */
    double lineHeight(FontSpec font);

    /** 줄 위쪽에서 글자 기준선(baseline)까지의 거리. */
    double ascent(FontSpec font);
}
