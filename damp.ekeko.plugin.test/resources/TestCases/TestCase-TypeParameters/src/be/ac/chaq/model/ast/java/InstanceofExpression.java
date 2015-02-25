package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;

public class InstanceofExpression extends Expression {
	@EntityProperty(value = Expression.class)
	private EntityIdentifier leftOperand;

	@EntityProperty(value = Type.class)
	private EntityIdentifier rightOperand;

	public EntityIdentifier getLeftoperand() {
		return leftOperand;
	}

	public EntityIdentifier getRightoperand() {
		return rightOperand;
	}

	public void setLeftoperand(EntityIdentifier leftOperand) {
		this.leftOperand = leftOperand;
	}

	public void setRightoperand(EntityIdentifier rightOperand) {
		this.rightOperand = rightOperand;
	}
}