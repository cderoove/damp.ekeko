package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;
import be.ac.chaq.model.entity.EntityProperty;

public class EnumConstantDeclaration extends BodyDeclaration {
	@EntityProperty(value = SimpleName.class)
	private EntityIdentifier name;

	@EntityListProperty(value = Expression.class)
	private List<EntityIdentifier> arguments;

	@EntityProperty(value = AnonymousClassDeclaration.class)
	private EntityIdentifier anonymousClassDeclaration;

	public EntityIdentifier getName() {
		return name;
	}

	public List<EntityIdentifier> getArguments() {
		return arguments;
	}

	public EntityIdentifier getAnonymousclassdeclaration() {
		return anonymousClassDeclaration;
	}

	public void setName(EntityIdentifier name) {
		this.name = name;
	}

	public void setArguments(List<EntityIdentifier> arguments) {
		this.arguments = arguments;
	}

	public void setAnonymousclassdeclaration(
			EntityIdentifier anonymousClassDeclaration) {
		this.anonymousClassDeclaration = anonymousClassDeclaration;
	}
}