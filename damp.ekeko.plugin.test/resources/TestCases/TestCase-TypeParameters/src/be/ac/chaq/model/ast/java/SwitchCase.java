package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;

public class SwitchCase extends Statement {
	@EntityProperty(value = Expression.class)
	private EntityIdentifier expression;

	public EntityIdentifier getExpression() {
		return expression;
	}

	public void setExpression(EntityIdentifier expression) {
		this.expression = expression;
	}
}