package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;

public class TypeDeclarationStatement extends Statement {
	@EntityProperty(value = AbstractTypeDeclaration.class)
	private EntityIdentifier declaration;

	public EntityIdentifier getDeclaration() {
		return declaration;
	}

	public void setDeclaration(EntityIdentifier declaration) {
		this.declaration = declaration;
	}
}