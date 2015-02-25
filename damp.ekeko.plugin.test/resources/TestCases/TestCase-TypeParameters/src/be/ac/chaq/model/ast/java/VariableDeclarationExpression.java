package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;
import be.ac.chaq.model.entity.EntityProperty;

public class VariableDeclarationExpression extends Expression {
	@EntityListProperty(value = Modifier.class)
	private List<EntityIdentifier> modifiers;

	@EntityProperty(value = Type.class)
	private EntityIdentifier type;

	@EntityListProperty(value = VariableDeclarationFragment.class)
	private List<EntityIdentifier> fragments;

	public List<EntityIdentifier> getModifiers() {
		return modifiers;
	}

	public EntityIdentifier getType() {
		return type;
	}

	public List<EntityIdentifier> getFragments() {
		return fragments;
	}

	public void setModifiers(List<EntityIdentifier> modifiers) {
		this.modifiers = modifiers;
	}

	public void setType(EntityIdentifier type) {
		this.type = type;
	}

	public void setFragments(List<EntityIdentifier> fragments) {
		this.fragments = fragments;
	}
}