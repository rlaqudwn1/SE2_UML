package uml.model.sequence;

/** 메시지 종류의 단일 출처. Mermaid 화살표 문법과의 변환은 파서·렌더러가 맡는다. */
public enum MessageType {
    SYNC,
    ASYNC,
    RETURN,
    CREATE,
    DESTROY
}
