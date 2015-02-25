package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;

public class FieldAccess extends Expression {
	@EntityProperty(value = Expression.class)
	private EntityIdentifier expression;

	@EntityProperty(value = SimpleName.class)
	private EntityIdentifier name;

	public EntityIdentifier getExpression() {
		return expression;
	}

	public EntityIdentifier getName() {
		return name;
	}

	public void setExpression(EntityIdentifier expression) {
		this.expression = expression;
	}

	public void setName(EntityIdentifier name) {
		this.name = name;
	}
}