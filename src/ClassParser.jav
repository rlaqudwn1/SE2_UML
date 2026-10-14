//전체 텍스트를 읽어 노드들과 관계들을 반환
public interface ClassParser {
	ParseResult parse(String text);
}