package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;

public class SimpleType extends Type {
	@EntityProperty(value = Name.class)
	private EntityIdentifier name;

	public EntityIdentifier getName() {
		return name;
	}

	public void setName(EntityIdentifier name) {
		this.name = name;
	}
}