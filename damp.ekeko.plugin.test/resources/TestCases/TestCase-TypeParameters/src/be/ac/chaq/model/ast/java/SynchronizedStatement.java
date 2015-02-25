package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;

public class SynchronizedStatement extends Statement {
	@EntityProperty(value = Expression.class)
	private EntityIdentifier expression;

	@EntityProperty(value = Block.class)
	private EntityIdentifier body;

	public EntityIdentifier getExpression() {
		return expression;
	}

	public EntityIdentifier getBody() {
		return body;
	}

	public void setExpression(EntityIdentifier expression) {
		this.expression = expression;
	}

	public void setBody(EntityIdentifier body) {
		this.body = body;
	}
}