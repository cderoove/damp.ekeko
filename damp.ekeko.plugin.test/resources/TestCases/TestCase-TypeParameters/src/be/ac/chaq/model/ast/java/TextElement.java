package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.SimpleProperty;

public class TextElement extends ASTNode implements IDocElement {
	@SimpleProperty(value = String.class)
	private String text;

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}