package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;
import be.ac.chaq.model.entity.EntityProperty;

public class PackageDeclaration extends ASTNode {
	@EntityProperty(value = Javadoc.class)
	private EntityIdentifier javadoc;

	@EntityListProperty(value = Annotation.class)
	private List<EntityIdentifier> annotations;

	@EntityProperty(value = Name.class)
	private EntityIdentifier name;

	public EntityIdentifier getJavadoc() {
		return javadoc;
	}

	public List<EntityIdentifier> getAnnotations() {
		return annotations;
	}

	public EntityIdentifier getName() {
		return name;
	}

	public void setJavadoc(EntityIdentifier javadoc) {
		this.javadoc = javadoc;
	}

	public void setAnnotations(List<EntityIdentifier> annotations) {
		this.annotations = annotations;
	}

	public void setName(EntityIdentifier name) {
		this.name = name;
	}
}