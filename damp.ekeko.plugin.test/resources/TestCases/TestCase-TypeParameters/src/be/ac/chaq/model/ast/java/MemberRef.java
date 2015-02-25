package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;

public class MemberRef extends ASTNode implements IDocElement {
	@EntityProperty(value = Name.class)
	private EntityIdentifier qualifier;

	@EntityProperty(value = SimpleName.class)
	private EntityIdentifier name;

	public EntityIdentifier getQualifier() {
		return qualifier;
	}

	public EntityIdentifier getName() {
		return name;
	}

	public void setQualifier(EntityIdentifier qualifier) {
		this.qualifier = qualifier;
	}

	public void setName(EntityIdentifier name) {
		this.name = name;
	}
}