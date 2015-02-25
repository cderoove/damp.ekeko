package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;

public class ArrayInitializer extends Expression {
	@EntityListProperty(value = Expression.class)
	private List<EntityIdentifier> expressions;

	public List<EntityIdentifier> getExpressions() {
		return expressions;
	}

	public void setExpressions(List<EntityIdentifier> expressions) {
		this.expressions = expressions;
	}
}