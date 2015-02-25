package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;

public class ThisExpression extends Expression {
	@EntityProperty(value = Name.class)
	private EntityIdentifier qualifier;

	public EntityIdentifier getQualifier() {
		return qualifier;
	}

	public void setQualifier(EntityIdentifier qualifier) {
		this.qualifier = qualifier;
	}
}