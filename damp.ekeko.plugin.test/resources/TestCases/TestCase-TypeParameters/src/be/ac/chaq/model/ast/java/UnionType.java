package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;

public class UnionType extends Type {
	@EntityListProperty(value = Type.class)
	private List<EntityIdentifier> types;

	public List<EntityIdentifier> getTypes() {
		return types;
	}

	public void setTypes(List<EntityIdentifier> types) {
		this.types = types;
	}
}