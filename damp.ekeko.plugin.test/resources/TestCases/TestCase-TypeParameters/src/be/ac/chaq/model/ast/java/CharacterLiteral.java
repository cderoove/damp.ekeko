package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.SimpleProperty;

public class CharacterLiteral extends Expression {
	@SimpleProperty(value = String.class)
	private String escapedValue;

	public String getEscapedvalue() {
		return escapedValue;
	}

	public void setEscapedvalue(String escapedValue) {
		this.escapedValue = escapedValue;
	}
}