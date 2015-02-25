package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;
import be.ac.chaq.model.entity.EntityProperty;

public class SuperMethodInvocation extends Expression {
	@EntityProperty(value = Name.class)
	private EntityIdentifier qualifier;

	@EntityListProperty(value = Type.class)
	private List<EntityIdentifier> typeArguments;

	@EntityProperty(value = SimpleName.class)
	private EntityIdentifier name;

	@EntityListProperty(value = Expression.class)
	private List<EntityIdentifier> arguments;

	public EntityIdentifier getQualifier() {
		return qualifier;
	}

	public List<EntityIdentifier> getTypearguments() {
		return typeArguments;
	}

	public EntityIdentifier getName() {
		return name;
	}

	public List<EntityIdentifier> getArguments() {
		return arguments;
	}

	public void setQualifier(EntityIdentifier qualifier) {
		this.qualifier = qualifier;
	}

	public void setTypearguments(List<EntityIdentifier> typeArguments) {
		this.typeArguments = typeArguments;
	}

	public void setName(EntityIdentifier name) {
		this.name = name;
	}

	public void setArguments(List<EntityIdentifier> arguments) {
		this.arguments = arguments;
	}
}