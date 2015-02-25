package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;

public class SingleMemberAnnotation extends Annotation {
	@EntityProperty(value = Expression.class)
	private EntityIdentifier value;

	public EntityIdentifier getValue() {
		return value;
	}

	public void setValue(EntityIdentifier value) {
		this.value = value;
	}
}