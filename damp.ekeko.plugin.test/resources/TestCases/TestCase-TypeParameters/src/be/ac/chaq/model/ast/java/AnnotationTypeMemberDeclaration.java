package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;

public class AnnotationTypeMemberDeclaration extends BodyDeclaration {
	@EntityProperty(value = Type.class)
	private EntityIdentifier type;

	@EntityProperty(value = SimpleName.class)
	private EntityIdentifier name;

	@EntityProperty(value = Expression.class, name = "default")
	private EntityIdentifier defaultExpression;

	public EntityIdentifier getType() {
		return type;
	}

	public EntityIdentifier getName() {
		return name;
	}

	public EntityIdentifier getDefaultExpression() {
		return defaultExpression;
	}

	public void setType(EntityIdentifier type) {
		this.type = type;
	}

	public void setName(EntityIdentifier name) {
		this.name = name;
	}

	public void setDefault(EntityIdentifier defaultExpression) {
		this.defaultExpression = defaultExpression;
	}
}