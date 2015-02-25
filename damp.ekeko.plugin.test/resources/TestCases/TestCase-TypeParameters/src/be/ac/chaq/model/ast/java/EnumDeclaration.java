package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;

public class EnumDeclaration extends AbstractTypeDeclaration {
	@EntityListProperty(value = Type.class)
	private List<EntityIdentifier> superInterfaceTypes;

	@EntityListProperty(value = EnumConstantDeclaration.class)
	private List<EntityIdentifier> enumConstants;

	public List<EntityIdentifier> getSuperinterfacetypes() {
		return superInterfaceTypes;
	}

	public List<EntityIdentifier> getEnumconstants() {
		return enumConstants;
	}

	public void setSuperinterfacetypes(
			List<EntityIdentifier> superInterfaceTypes) {
		this.superInterfaceTypes = superInterfaceTypes;
	}

	public void setEnumconstants(List<EntityIdentifier> enumConstants) {
		this.enumConstants = enumConstants;
	}
}