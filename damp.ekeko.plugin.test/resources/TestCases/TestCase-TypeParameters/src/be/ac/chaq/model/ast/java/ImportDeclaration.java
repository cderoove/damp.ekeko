package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;
import be.ac.chaq.model.entity.SimpleProperty;

public class ImportDeclaration extends ASTNode {
	@SimpleProperty(value = Boolean.class, name = "static")
	private Boolean isStatic;

	@EntityProperty(value = Name.class)
	private EntityIdentifier name;

	@SimpleProperty(value = Boolean.class)
	private Boolean onDemand;

	public Boolean isStatic() {
		return isStatic;
	}

	public EntityIdentifier getName() {
		return name;
	}

	public Boolean getOndemand() {
		return onDemand;
	}

	public void setIsStatic(Boolean isStatic) {
		this.isStatic = isStatic;
	}

	public void setName(EntityIdentifier name) {
		this.name = name;
	}

	public void setOndemand(Boolean onDemand) {
		this.onDemand = onDemand;
	}
}