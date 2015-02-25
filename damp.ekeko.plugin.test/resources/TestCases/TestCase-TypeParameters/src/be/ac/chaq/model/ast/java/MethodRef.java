package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;
import be.ac.chaq.model.entity.EntityProperty;

public class MethodRef extends ASTNode implements IDocElement {
	@EntityProperty(value = Name.class)
	private EntityIdentifier qualifier;

	@EntityProperty(value = SimpleName.class)
	private EntityIdentifier name;

	@EntityListProperty(value = MethodRefParameter.class)
	private List<EntityIdentifier> parameters;

	public EntityIdentifier getQualifier() {
		return qualifier;
	}

	public EntityIdentifier getName() {
		return name;
	}

	public List<EntityIdentifier> getParameters() {
		return parameters;
	}

	public void setQualifier(EntityIdentifier qualifier) {
		this.qualifier = qualifier;
	}

	public void setName(EntityIdentifier name) {
		this.name = name;
	}

	public void setParameters(List<EntityIdentifier> parameters) {
		this.parameters = parameters;
	}
}