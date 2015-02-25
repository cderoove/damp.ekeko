package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;
import be.ac.chaq.model.entity.SimpleProperty;


public class VariableDeclaration extends ASTNode {

	@EntityProperty(value = SimpleName.class)
	protected EntityIdentifier name;
	@SimpleProperty(value = Integer.class)
	protected Integer extraDimensions;
	@EntityProperty(value = Expression.class)
	protected EntityIdentifier initializer;

	public EntityIdentifier getName() {
		return name;
	}

	public Integer getExtradimensions() {
		return extraDimensions;
	}

	public EntityIdentifier getInitializer() {
		return initializer;
	}

	public void setName(EntityIdentifier name) {
		this.name = name;
	}

	public void setExtradimensions(Integer extraDimensions) {
		this.extraDimensions = extraDimensions;
	}

	public void setInitializer(EntityIdentifier initializer) {
		this.initializer = initializer;
	}

}