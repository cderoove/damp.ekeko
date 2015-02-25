package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;
import be.ac.chaq.model.entity.EntityProperty;

public class SuperConstructorInvocation extends Statement {
	@EntityProperty(value = Expression.class)
	private EntityIdentifier expression;

	@EntityListProperty(value = Type.class)
	private List<EntityIdentifier> typeArguments;

	@EntityListProperty(value = Expression.class)
	private List<EntityIdentifier> arguments;

	public EntityIdentifier getExpression() {
		return expression;
	}

	public List<EntityIdentifier> getTypearguments() {
		return typeArguments;
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

	public void setArguments(List<EntityIdentifier> arguments) {
		this.arguments = arguments;
	}
}