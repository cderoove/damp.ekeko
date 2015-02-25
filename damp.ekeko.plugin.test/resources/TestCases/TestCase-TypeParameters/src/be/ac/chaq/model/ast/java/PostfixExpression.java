package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;
import be.ac.chaq.model.entity.SimpleProperty;

public class PostfixExpression extends Expression {

	public static class Operator {

	}

	@EntityProperty(value = Expression.class)
	private EntityIdentifier operand;

	@SimpleProperty(value = Operator.class)
	private Operator operator;

	public EntityIdentifier getOperand() {
		return operand;
	}

	public Operator getOperator() {
		return operator;
	}

	public void setOperand(EntityIdentifier operand) {
		this.operand = operand;
	}

	public void setOperator(Operator operator) {
		this.operator = operator;
	}
}