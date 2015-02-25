package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;
import be.ac.chaq.model.entity.EntityProperty;

public class CompilationUnit extends ASTNode {

	@EntityProperty(value = PackageDeclaration.class, name = "package")
	private EntityIdentifier packageDeclaration;

	@EntityListProperty(value = ImportDeclaration.class)
	private List<EntityIdentifier> imports;

	@EntityListProperty(value = AbstractTypeDeclaration.class)
	private List<EntityIdentifier> types;

	public EntityIdentifier getPackage() {
		return packageDeclaration;
	}

	public List<EntityIdentifier> getImports() {
		return imports;
	}

	public List<EntityIdentifier> getTypes() {
		return types;
	}

	public void setPackage(EntityIdentifier packageDeclaration) {
		this.packageDeclaration = packageDeclaration;
	}

	public void setImports(List<EntityIdentifier> imports) {
		this.imports = imports;
	}

	public void setTypes(List<EntityIdentifier> types) {
		this.types = types;
	}
}