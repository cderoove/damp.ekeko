package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.SimpleProperty;

public class NumberLiteral extends Expression {
	@SimpleProperty(value = String.class)
	private String token;

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}