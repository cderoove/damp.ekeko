package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;
import be.ac.chaq.model.entity.EntityProperty;


public class AbstractTypeDeclaration extends BodyDeclaration {

	@EntityProperty(value = SimpleName.class)
	protected EntityIdentifier name;
	@EntityListProperty(value = BodyDeclaration.class)
	protected List<EntityIdentifier> bodyDeclarations;

	public EntityIdentifier getName() {
		return name;
	}

	public List<EntityIdentifier> getBodyDeclarations() {
		return bodyDeclarations;
	}

	public void setName(EntityIdentifier name) {
		this.name = name;
	}

	public void setBodyDeclarations(List<EntityIdentifier> bodyDeclarations) {
		this.bodyDeclarations = bodyDeclarations;
	}

}