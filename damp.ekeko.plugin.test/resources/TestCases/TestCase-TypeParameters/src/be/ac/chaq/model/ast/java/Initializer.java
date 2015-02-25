package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;

public class Initializer extends BodyDeclaration {
	@EntityProperty(value = Block.class)
	private EntityIdentifier body;

	
	public EntityIdentifier getBody() {
		return body;
	}

	public void setBody(EntityIdentifier body) {
		this.body = body;
	}
}