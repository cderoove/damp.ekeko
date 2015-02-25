package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;
import be.ac.chaq.model.entity.EntityProperty;


public class BodyDeclaration extends ASTNode {

	@EntityProperty(value = Javadoc.class)
	protected EntityIdentifier javadoc;
	
	@EntityListProperty(value = Modifier.class)
	protected List<EntityIdentifier> modifiers;

	public EntityIdentifier getJavadoc() {
		return javadoc;
	}

	public List<EntityIdentifier> getModifiers() {
		return modifiers;
	}

	public void setJavadoc(EntityIdentifier javadoc) {
		this.javadoc = javadoc;
	}

	public void setModifiers(List<EntityIdentifier> modifiers) {
		this.modifiers = modifiers;
	}

}