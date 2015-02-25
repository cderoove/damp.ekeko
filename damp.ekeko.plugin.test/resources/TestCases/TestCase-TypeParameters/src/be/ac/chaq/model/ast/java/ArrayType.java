package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;

public class ArrayType extends Type {
	@EntityProperty(value = Type.class)
	private EntityIdentifier componentType;

	public EntityIdentifier getComponenttype() {
		return componentType;
	}

	public void setComponenttype(EntityIdentifier componentType) {
		this.componentType = componentType;
	}
}