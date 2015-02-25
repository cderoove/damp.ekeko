package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;

public class ConditionalExpression extends Expression {
	@EntityProperty(value = Expression.class)
	private EntityIdentifier expression;

	@EntityProperty(value = Expression.class)
	private EntityIdentifier thenExpression;

	@EntityProperty(value = Expression.class)
	private EntityIdentifier elseExpression;

	public EntityIdentifier getExpression() {
		return expression;
	}

	public EntityIdentifier getThenexpression() {
		return thenExpression;
	}

	public EntityIdentifier getElseexpression() {
		return elseExpression;
	}

	public void setExpression(EntityIdentifier expression) {
		this.expression = expression;
	}

	public void setThenexpression(EntityIdentifier thenExpression) {
		this.thenExpression = thenExpression;
	}

	public void setElseexpression(EntityIdentifier elseExpression) {
		this.elseExpression = elseExpression;
	}
}