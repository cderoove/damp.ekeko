package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;

public class Block extends Statement {
	@EntityListProperty(value = Statement.class)
	private List<EntityIdentifier> statements;

	public List<EntityIdentifier> getStatements() {
		return statements;
	}

	public void setStatements(List<EntityIdentifier> statements) {
		this.statements = statements;
	}
}