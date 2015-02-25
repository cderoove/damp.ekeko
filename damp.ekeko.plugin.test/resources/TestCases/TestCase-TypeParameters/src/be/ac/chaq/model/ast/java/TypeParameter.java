package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;
import be.ac.chaq.model.entity.EntityProperty;

public class TypeParameter extends ASTNode {
	@EntityProperty(value = SimpleName.class)
	private EntityIdentifier name;

	@EntityListProperty(value = Type.class)
	private List<EntityIdentifier> typeBounds;

	public EntityIdentifier getName() {
		return name;
	}

	public List<EntityIdentifier> getTypebounds() {
		return typeBounds;
	}

	public void setName(EntityIdentifier name) {
		this.name = name;
	}

	public void setTypebounds(List<EntityIdentifier> typeBounds) {
		this.typeBounds = typeBounds;
	}
}