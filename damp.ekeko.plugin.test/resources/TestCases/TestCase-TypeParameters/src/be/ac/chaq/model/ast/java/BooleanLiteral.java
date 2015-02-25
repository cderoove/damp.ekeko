package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.SimpleProperty;

public class BooleanLiteral extends Expression {
	@SimpleProperty(value = Boolean.class)
	private Boolean booleanValue;

	public Boolean getBooleanvalue() {
		return booleanValue;
	}

	public void setBooleanvalue(Boolean booleanValue) {
		this.booleanValue = booleanValue;
	}
}