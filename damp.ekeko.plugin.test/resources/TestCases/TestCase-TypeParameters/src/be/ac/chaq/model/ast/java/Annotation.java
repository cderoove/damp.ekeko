package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;


public class Annotation extends Expression {

	@EntityProperty(value = Name.class)
	protected EntityIdentifier typeName;

	public EntityIdentifier getTypename() {
		return typeName;
	}

	public void setTypename(EntityIdentifier typeName) {
		this.typeName = typeName;
	}

}