package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;

public class AssertStatement extends Statement {
	@EntityProperty(value = Expression.class)
	private EntityIdentifier expression;

	@EntityProperty(value = Expression.class)
	private EntityIdentifier message;

	public EntityIdentifier getExpression() {
		return expression;
	}

	public EntityIdentifier getMessage() {
		return message;
	}

	public void setExpression(EntityIdentifier expression) {
		this.expression = expression;
	}

	public void setMessage(EntityIdentifier message) {
		this.message = message;
	}
}