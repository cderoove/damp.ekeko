package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;

public class CastExpression extends Expression {
	@EntityProperty(value = Type.class)
	private EntityIdentifier type;

	@EntityProperty(value = Expression.class)
	private EntityIdentifier expression;

	public EntityIdentifier getType() {
		return type;
	}

	public EntityIdentifier getExpression() {
		return expression;
	}

	public void setType(EntityIdentifier type) {
		this.type = type;
	}

	public void setExpression(EntityIdentifier expression) {
		this.expression = expression;
	}
}