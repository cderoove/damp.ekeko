package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;

public class TypeLiteral extends Expression {
	@EntityProperty(value = Type.class)
	private EntityIdentifier type;

	public EntityIdentifier getType() {
		return type;
	}

	public void setType(EntityIdentifier type) {
		this.type = type;
	}
}