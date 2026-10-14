import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ClassNode {
	private final String name;
	private final List<ClassMember> members = new ArrayList<>();

	public ClassNode(String name) {
		this.name = Objects.requireNonNull(name, "name");
	}

	public String getName() {
		return name;
	}

	public List<ClassMember> getMembers() {
		return List.copyOf(members);
	}

	public void addMember(ClassMember member) {
		members.add(Objects.requireNonNull(member, "member"));
	}
}