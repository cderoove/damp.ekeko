package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;

public class IfStatement extends Statement {
	@EntityProperty(value = Expression.class)
	private EntityIdentifier expression;

	@EntityProperty(value = Statement.class)
	private EntityIdentifier thenStatement;

	@EntityProperty(value = Statement.class)
	private EntityIdentifier elseStatement;

	public EntityIdentifier getExpression() {
		return expression;
	}

	public EntityIdentifier getThenstatement() {
		return thenStatement;
	}

	public EntityIdentifier getElsestatement() {
		return elseStatement;
	}

	public void setExpression(EntityIdentifier expression) {
		this.expression = expression;
	}

	public void setThenstatement(EntityIdentifier thenStatement) {
		this.thenStatement = thenStatement;
	}

	public void setElsestatement(EntityIdentifier elseStatement) {
		this.elseStatement = elseStatement;
	}
}