package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;

public class MemberValuePair extends ASTNode {
	@EntityProperty(value = SimpleName.class)
	private EntityIdentifier name;

	@EntityProperty(value = Expression.class)
	private EntityIdentifier value;

	public EntityIdentifier getName() {
		return name;
	}

	public EntityIdentifier getValue() {
		return value;
	}

	public void setName(EntityIdentifier name) {
		this.name = name;
	}

	public void setValue(EntityIdentifier value) {
		this.value = value;
	}
}