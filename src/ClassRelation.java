public class ClassRelation {

	private final ClassNode source;
	private final ClassNode target;
	//private final RelationType type;
	private final String name;

	public ClassRelation(
			ClassNode source,
			ClassNode target,
			//RelationType type,
			String name) {
		this.source = source;
		this.target = target;
		//this.type = Objects.requireNonNull(type, "type");
		this.name = name;
	}

	public ClassNode getSource() {
		return source;
	}

	public ClassNode getTarget() {
		return target;
	}
	/*
	public RelationType getType() {
		return type;
	}
	*/

	public String getName() {
		return name;
	}
}