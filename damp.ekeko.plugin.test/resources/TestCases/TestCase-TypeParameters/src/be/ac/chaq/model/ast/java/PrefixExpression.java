package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;
import be.ac.chaq.model.entity.SimpleProperty;

public class PrefixExpression extends Expression {

	public static class Operator {

	}

	@SimpleProperty(value = Operator.class)
	private Operator operator;

	@EntityProperty(value = Expression.class)
	private EntityIdentifier operand;

	public Operator getOperator() {
		return operator;
	}

	public EntityIdentifier getOperand() {
		return operand;
	}

	public void setOperator(Operator operator) {
		this.operator = operator;
	}

	public void setOperand(EntityIdentifier operand) {
		this.operand = operand;
	}
}