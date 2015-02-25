package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;
import be.ac.chaq.model.entity.EntityProperty;

public class ForStatement extends Statement {
	@EntityListProperty(value = Expression.class)
	private List<EntityIdentifier> initializers;

	@EntityProperty(value = Expression.class)
	private EntityIdentifier expression;

	@EntityListProperty(value = Expression.class)
	private List<EntityIdentifier> updaters;

	@EntityProperty(value = Statement.class)
	private EntityIdentifier body;

	public List<EntityIdentifier> getInitializers() {
		return initializers;
	}

	public EntityIdentifier getExpression() {
		return expression;
	}

	public List<EntityIdentifier> getUpdaters() {
		return updaters;
	}

	public EntityIdentifier getBody() {
		return body;
	}

	public void setInitializers(List<EntityIdentifier> initializers) {
		this.initializers = initializers;
	}

	public void setExpression(EntityIdentifier expression) {
		this.expression = expression;
	}

	public void setUpdaters(List<EntityIdentifier> updaters) {
		this.updaters = updaters;
	}

	public void setBody(EntityIdentifier body) {
		this.body = body;
	}
}