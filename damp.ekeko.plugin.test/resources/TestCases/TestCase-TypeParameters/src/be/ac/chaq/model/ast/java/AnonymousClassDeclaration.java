package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;

public class AnonymousClassDeclaration extends ASTNode {
	@EntityListProperty(value = BodyDeclaration.class)
	private List<EntityIdentifier> bodyDeclarations;

	public List<EntityIdentifier> getBodydeclarations() {
		return bodyDeclarations;
	}

	public void setBodydeclarations(List<EntityIdentifier> bodyDeclarations) {
		this.bodyDeclarations = bodyDeclarations;
	}
}