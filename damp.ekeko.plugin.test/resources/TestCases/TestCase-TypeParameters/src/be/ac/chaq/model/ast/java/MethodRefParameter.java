package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;
import be.ac.chaq.model.entity.SimpleProperty;

public class MethodRefParameter extends ASTNode {
	@EntityProperty(value = Type.class)
	private EntityIdentifier type;

	@SimpleProperty(value = Boolean.class)
	private Boolean varargs;

	@EntityProperty(value = SimpleName.class)
	private EntityIdentifier name;

	public EntityIdentifier getType() {
		return type;
	}

	public Boolean getVarargs() {
		return varargs;
	}

	public EntityIdentifier getName() {
		return name;
	}

	public void setType(EntityIdentifier type) {
		this.type = type;
	}

	public void setVarargs(Boolean varargs) {
		this.varargs = varargs;
	}

	public void setName(EntityIdentifier name) {
		this.name = name;
	}
}