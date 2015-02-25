package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;

public class ContinueStatement extends Statement {
	@EntityProperty(value = SimpleName.class)
	private EntityIdentifier label;

	public EntityIdentifier getLabel() {
		return label;
	}

	public void setLabel(EntityIdentifier label) {
		this.label = label;
	}
}