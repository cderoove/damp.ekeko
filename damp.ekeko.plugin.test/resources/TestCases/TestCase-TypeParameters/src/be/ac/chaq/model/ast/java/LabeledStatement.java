package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;

public class LabeledStatement extends Statement {
	@EntityProperty(value = SimpleName.class)
	private EntityIdentifier label;

	@EntityProperty(value = Statement.class)
	private EntityIdentifier body;

	public EntityIdentifier getLabel() {
		return label;
	}

	public EntityIdentifier getBody() {
		return body;
	}

	public void setLabel(EntityIdentifier label) {
		this.label = label;
	}

	public void setBody(EntityIdentifier body) {
		this.body = body;
	}
}