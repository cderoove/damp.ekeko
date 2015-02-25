package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;
import be.ac.chaq.model.entity.EntityProperty;
import be.ac.chaq.model.entity.SimpleProperty;

public class InfixExpression extends Expression {

	public static class Operator {

	}

	@EntityProperty(value = Expression.class)
	private EntityIdentifier leftOperand;

	@SimpleProperty(value = Operator.class)
	private Operator operator;

	@EntityProperty(value = Expression.class)
	private EntityIdentifier rightOperand;

	@EntityListProperty(value = Expression.class)
	private List<EntityIdentifier> extendedOperands;

	public EntityIdentifier getLeftoperand() {
		return leftOperand;
	}

	public Operator getOperator() {
		return operator;
	}

	public EntityIdentifier getRightoperand() {
		return rightOperand;
	}

	public List<EntityIdentifier> getExtendedoperands() {
		return extendedOperands;
	}

	public void setLeftoperand(EntityIdentifier leftOperand) {
		this.leftOperand = leftOperand;
	}

	public void setOperator(Operator operator) {
		this.operator = operator;
	}

	public void setRightoperand(EntityIdentifier rightOperand) {
		this.rightOperand = rightOperand;
	}

	public void setExtendedoperands(List<EntityIdentifier> extendedOperands) {
		this.extendedOperands = extendedOperands;
	}
}