package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;
import be.ac.chaq.model.entity.EntityProperty;

public class SwitchStatement extends Statement {
	@EntityProperty(value = Expression.class)
	private EntityIdentifier expression;

	@EntityListProperty(value = Statement.class)
	private List<EntityIdentifier> statements;

	public EntityIdentifier getExpression() {
		return expression;
	}

	public List<EntityIdentifier> getStatements() {
		return statements;
	}

	public void setExpression(EntityIdentifier expression) {
		this.expression = expression;
	}

	public void setStatements(List<EntityIdentifier> statements) {
		this.statements = statements;
	}
}