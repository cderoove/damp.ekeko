package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;
import be.ac.chaq.model.entity.EntityProperty;

public class TryStatement extends Statement {
	@EntityProperty(value = Block.class)
	private EntityIdentifier body;

	@EntityListProperty(value = CatchClause.class)
	private List<EntityIdentifier> catchClauses;

	@EntityProperty(value = Block.class, name = "finally")
	private EntityIdentifier finallyBlock;
	
	@EntityListProperty(value = VariableDeclarationExpression.class)
	private List<EntityIdentifier> resources;

	public EntityIdentifier getBody() {
		return body;
	}

	public List<EntityIdentifier> getCatchclauses() {
		return catchClauses;
	}

	public EntityIdentifier getFinally() {
		return finallyBlock;
	}

	public void setBody(EntityIdentifier body) {
		this.body = body;
	}

	public void setCatchclauses(List<EntityIdentifier> catchClauses) {
		this.catchClauses = catchClauses;
	}

	public void setFinally(EntityIdentifier finallyBlock) {
		this.finallyBlock = finallyBlock;
	}

	public List<EntityIdentifier> getResources() {
		return resources;
	}

	public void setResources(List<EntityIdentifier> resources) {
		this.resources = resources;
	}
}