package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;
import be.ac.chaq.model.entity.EntityProperty;

public class ParameterizedType extends Type {
	@EntityProperty(value = Type.class)
	private EntityIdentifier type;

	@EntityListProperty(value = Type.class)
	private List<EntityIdentifier> typeArguments;

	public EntityIdentifier getType() {
		return type;
	}

	public List<EntityIdentifier> getTypearguments() {
		return typeArguments;
	}

	public void setType(EntityIdentifier type) {
		this.type = type;
	}

	public void setTypearguments(List<EntityIdentifier> typeArguments) {
		this.typeArguments = typeArguments;
	}
}