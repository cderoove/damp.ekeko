package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;
import be.ac.chaq.model.entity.EntityProperty;
import be.ac.chaq.model.entity.SimpleProperty;

public class TypeDeclaration extends AbstractTypeDeclaration {
	@SimpleProperty(value = Boolean.class, name = "interface")
	private Boolean isInterface;

	@EntityListProperty(value = TypeParameter.class)
	private List<EntityIdentifier> typeParameters;

	@EntityProperty(value = Type.class)
	private EntityIdentifier superclassType;

	@EntityListProperty(value = Type.class)
	private List<EntityIdentifier> superInterfaceTypes;

	public Boolean isInterface() {
		return isInterface;
	}

	public List<EntityIdentifier> getTypeParameters() {
		return typeParameters;
	}

	public EntityIdentifier getSuperclassType() {
		return superclassType;
	}

	public List<EntityIdentifier> getSuperInterfaceTypes() {
		return superInterfaceTypes;
	}

	public void setIsInterface(Boolean isInterface) {
		this.isInterface = isInterface;
	}

	public void setTypeParameters(List<EntityIdentifier> typeParameters) {
		this.typeParameters = typeParameters;
	}

	public void setSuperclassType(EntityIdentifier superclassType) {
		this.superclassType = superclassType;
	}

	public void setSuperInterfaceTypes(List<EntityIdentifier> superInterfaceTypes) {
		this.superInterfaceTypes = superInterfaceTypes;
	}

}