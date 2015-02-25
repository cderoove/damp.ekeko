package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;
import be.ac.chaq.model.entity.SimpleProperty;

public class WildcardType extends Type {
	@EntityProperty(value = Type.class)
	private EntityIdentifier bound;

	@SimpleProperty(value = Boolean.class)
	private Boolean upperBound;

	public EntityIdentifier getBound() {
		return bound;
	}

	public Boolean getUpperbound() {
		return upperBound;
	}

	public void setBound(EntityIdentifier bound) {
		this.bound = bound;
	}

	public void setUpperbound(Boolean upperBound) {
		this.upperBound = upperBound;
	}
}