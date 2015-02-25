package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;
import be.ac.chaq.model.entity.EntityProperty;
import be.ac.chaq.model.entity.SimpleProperty;

public class SingleVariableDeclaration extends VariableDeclaration {
	@EntityListProperty(value = Modifier.class)
	private List<EntityIdentifier> modifiers;

	@EntityProperty(value = Type.class)
	private EntityIdentifier type;

	@SimpleProperty(value = Boolean.class)
	private Boolean varargs;

	public List<EntityIdentifier> getModifiers() {
		return modifiers;
	}

	public EntityIdentifier getType() {
		return type;
	}

	public Boolean getVarargs() {
		return varargs;
	}

	public void setModifiers(List<EntityIdentifier> modifiers) {
		this.modifiers = modifiers;
	}

	public void setType(EntityIdentifier type) {
		this.type = type;
	}

	public void setVarargs(Boolean varargs) {
		this.varargs = varargs;
	}
}