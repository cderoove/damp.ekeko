package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;
import be.ac.chaq.model.entity.EntityProperty;
import be.ac.chaq.model.entity.SimpleProperty;

public class MethodDeclaration extends BodyDeclaration {
	@SimpleProperty(value = Boolean.class)
	private Boolean constructor;

	@EntityListProperty(value = TypeParameter.class)
	private List<EntityIdentifier> typeParameters;

	@EntityProperty(value = Type.class)
	private EntityIdentifier returnType2;

	@EntityProperty(value = SimpleName.class)
	private EntityIdentifier name;

	@EntityListProperty(value = SingleVariableDeclaration.class)
	private List<EntityIdentifier> parameters;

	@SimpleProperty(value = Integer.class)
	private Integer extraDimensions;

	@EntityListProperty(value = Name.class)
	private List<EntityIdentifier> thrownExceptions;

	@EntityProperty(value = Block.class)
	private EntityIdentifier body;

	public Boolean getConstructor() {
		return constructor;
	}

	public List<EntityIdentifier> getTypeparameters() {
		return typeParameters;
	}

	public EntityIdentifier getReturntype2() {
		return returnType2;
	}

	public EntityIdentifier getName() {
		return name;
	}

	public List<EntityIdentifier> getParameters() {
		return parameters;
	}

	public Integer getExtradimensions() {
		return extraDimensions;
	}

	public List<EntityIdentifier> getThrownexceptions() {
		return thrownExceptions;
	}

	public EntityIdentifier getBody() {
		return body;
	}

	public void setConstructor(Boolean constructor) {
		this.constructor = constructor;
	}

	public void setTypeparameters(List<EntityIdentifier> typeParameters) {
		this.typeParameters = typeParameters;
	}

	public void setReturntype2(EntityIdentifier returnType2) {
		this.returnType2 = returnType2;
	}

	public void setName(EntityIdentifier name) {
		this.name = name;
	}

	public void setParameters(List<EntityIdentifier> parameters) {
		this.parameters = parameters;
	}

	public void setExtradimensions(Integer extraDimensions) {
		this.extraDimensions = extraDimensions;
	}

	public void setThrownexceptions(List<EntityIdentifier> thrownExceptions) {
		this.thrownExceptions = thrownExceptions;
	}

	public void setBody(EntityIdentifier body) {
		this.body = body;
	}
}