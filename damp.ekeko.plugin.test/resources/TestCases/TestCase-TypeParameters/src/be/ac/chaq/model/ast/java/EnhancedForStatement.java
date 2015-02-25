package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;

public class EnhancedForStatement extends Statement {
	@EntityProperty(value = SingleVariableDeclaration.class)
	private EntityIdentifier parameter;

	@EntityProperty(value = Expression.class)
	private EntityIdentifier expression;

	@EntityProperty(value = Statement.class)
	private EntityIdentifier body;

	public EntityIdentifier getParameter() {
		return parameter;
	}

	public EntityIdentifier getExpression() {
		return expression;
	}

	public EntityIdentifier getBody() {
		return body;
	}

	public void setParameter(EntityIdentifier parameter) {
		this.parameter = parameter;
	}

	public void setExpression(EntityIdentifier expression) {
		this.expression = expression;
	}

	public void setBody(EntityIdentifier body) {
		this.body = body;
	}
}