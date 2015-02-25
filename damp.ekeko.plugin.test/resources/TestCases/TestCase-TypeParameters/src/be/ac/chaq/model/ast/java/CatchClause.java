package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;

public class CatchClause extends ASTNode {
	@EntityProperty(value = SingleVariableDeclaration.class)
	private EntityIdentifier exception;

	@EntityProperty(value = Block.class)
	private EntityIdentifier body;

	public EntityIdentifier getException() {
		return exception;
	}

	public EntityIdentifier getBody() {
		return body;
	}

	public void setException(EntityIdentifier exception) {
		this.exception = exception;
	}

	public void setBody(EntityIdentifier body) {
		this.body = body;
	}
}