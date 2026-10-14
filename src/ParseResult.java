import java.util.List;

//리턴 객체
//노드들과 노드 관계들을 반환
public record ParseResult(
	List<ClassNode> classes,
	List<ClassRelation> relations
) {
	public ParseResult {
		classes = List.copyOf(classes);
		relations = List.copyOf(relations);
	}
}