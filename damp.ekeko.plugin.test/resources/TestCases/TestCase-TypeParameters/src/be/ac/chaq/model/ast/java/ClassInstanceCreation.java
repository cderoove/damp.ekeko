package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;
import be.ac.chaq.model.entity.EntityProperty;

public class ClassInstanceCreation extends Expression {
	@EntityProperty(value = Expression.class)
	private EntityIdentifier expression;

	@EntityListProperty(value = Type.class)
	private List<EntityIdentifier> typeArguments;

	@EntityProperty(value = Type.class)
	private EntityIdentifier type;

	@EntityListProperty(value = Expression.class)
	private List<EntityIdentifier> arguments;

	@EntityProperty(value = AnonymousClassDeclaration.class)
	private EntityIdentifier anonymousClassDeclaration;

	public EntityIdentifier getExpression() {
		return expression;
	}

	public List<EntityIdentifier> getTypearguments() {
		return typeArguments;
	}

	public EntityIdentifier getType() {
		return type;
	}

	public List<EntityIdentifier> getArguments() {
		return arguments;
	}

	public EntityIdentifier getAnonymousclassdeclaration() {
		return anonymousClassDeclaration;
	}

	public void setExpression(EntityIdentifier expression) {
		this.expression = expression;
	}

	public void setTypearguments(List<EntityIdentifier> typeArguments) {
		this.typeArguments = typeArguments;
	}

	public void setType(EntityIdentifier type) {
		this.type = type;
	}

	public void setArguments(List<EntityIdentifier> arguments) {
		this.arguments = arguments;
	}

	public void setAnonymousclassdeclaration(
			EntityIdentifier anonymousClassDeclaration) {
		this.anonymousClassDeclaration = anonymousClassDeclaration;
	}
}