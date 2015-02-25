package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;
import be.ac.chaq.model.entity.SimpleProperty;

public class Assignment extends Expression {

	public static class Operator {

	}

	@EntityProperty(value = Expression.class)
	private EntityIdentifier leftHandSide;

	@SimpleProperty(value = Operator.class)
	private Operator operator;

	@EntityProperty(value = Expression.class)
	private EntityIdentifier rightHandSide;

	public EntityIdentifier getLefthandside() {
		return leftHandSide;
	}

	public Operator getOperator() {
		return operator;
	}

	public EntityIdentifier getRighthandside() {
		return rightHandSide;
	}

	public void setLefthandside(EntityIdentifier leftHandSide) {
		this.leftHandSide = leftHandSide;
	}

	public void setOperator(Operator operator) {
		this.operator = operator;
	}

	public void setRighthandside(EntityIdentifier rightHandSide) {
		this.rightHandSide = rightHandSide;
	}
}