package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;

public class DoStatement extends Statement {
	@EntityProperty(value = Statement.class)
	private EntityIdentifier body;

	@EntityProperty(value = Expression.class)
	private EntityIdentifier expression;

	public EntityIdentifier getBody() {
		return body;
	}

	public EntityIdentifier getExpression() {
		return expression;
	}

	public void setBody(EntityIdentifier body) {
		this.body = body;
	}

	public void setExpression(EntityIdentifier expression) {
		this.expression = expression;
	}
}