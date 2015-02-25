package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.SimpleProperty;

public class Modifier extends ASTNode {

	public static class ModifierKeyword {

	}

	@SimpleProperty(value = ModifierKeyword.class)
	private ModifierKeyword keyword;

	public ModifierKeyword getKeyword() {
		return keyword;
	}

	public void setKeyword(ModifierKeyword keyword) {
		this.keyword = keyword;
	}
}