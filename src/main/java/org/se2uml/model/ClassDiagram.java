package org.se2uml.model;

import java.util.List;
import java.util.Objects;
import org.se2uml.model.DiagramModel.*;

/** Class semantics only: no engine-specific objects or drawing coordinates. */
public record ClassDiagram(List<ClassNode> classes, List<Relationship> relationships)
        implements SemanticDiagram {
    public ClassDiagram {
        classes = List.copyOf(classes);
        relationships = List.copyOf(relationships);
    }

    @Override
    public DiagramKind kind() { return DiagramKind.CLASS; }

    public enum ClassKind { CLASS, ABSTRACT_CLASS, INTERFACE, ENUM }
    public enum Visibility { PUBLIC, PROTECTED, PACKAGE, PRIVATE }
    public enum RelationshipKind {
        ASSOCIATION, DEPENDENCY, GENERALIZATION, REALIZATION, AGGREGATION, COMPOSITION
    }

    public record ClassNode(ElementId id, String name, ClassKind classKind,
            List<String> stereotypes, List<Attribute> attributes, List<Operation> operations) {
        public ClassNode {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(classKind, "classKind");
            stereotypes = List.copyOf(stereotypes);
            attributes = List.copyOf(attributes);
            operations = List.copyOf(operations);
        }
    }

    public record Attribute(ElementId id, String name, String type,
            Visibility visibility, boolean isStatic) {}

    public record Parameter(String name, String type) {}

    public record Operation(ElementId id, String name, List<Parameter> parameters,
            String returnType, Visibility visibility, boolean isStatic, boolean isAbstract) {
        public Operation { parameters = List.copyOf(parameters); }
    }

    /** Empty role/multiplicity means unspecified; multiplicity is UML text such as 0..*. */
    public record RelationshipEnd(ElementId classId, String role, String multiplicity) {}

    /**
     * GENERALIZATION/REALIZATION: source child/implementer -> target parent/interface.
     * AGGREGATION/COMPOSITION: source whole (diamond) -> target part.
     * DEPENDENCY: source dependent -> target supplier.
     * ASSOCIATION: source/target identify ends; navigability is explicit below.
     */
    public record Relationship(ElementId id, RelationshipKind relationshipKind,
            RelationshipEnd source, RelationshipEnd target, String label,
            boolean navigableToSource, boolean navigableToTarget) {}
}
