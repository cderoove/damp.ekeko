package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;
import be.ac.chaq.model.entity.EntityProperty;

public class MethodInvocation extends Expression {
	@EntityProperty(value = Expression.class)
	private EntityIdentifier expression;

	@EntityListProperty(value = Type.class)
	private List<EntityIdentifier> typeArguments;

	@EntityProperty(value = SimpleName.class)
	private EntityIdentifier name;

	@EntityListProperty(value = Expression.class)
	private List<EntityIdentifier> arguments;

	public EntityIdentifier getExpression() {
		return expression;
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

	public void setExpression(EntityIdentifier expression) {
		this.expression = expression;
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