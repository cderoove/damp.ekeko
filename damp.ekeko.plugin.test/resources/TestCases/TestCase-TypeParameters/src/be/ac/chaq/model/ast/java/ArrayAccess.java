package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;

public class ArrayAccess extends Expression {
	private @EntityProperty(value = Expression.class) 
	EntityIdentifier array;

	@EntityProperty(value = Expression.class)
	private EntityIdentifier index;
		
	public EntityIdentifier getArray() {
		return array;
	}

	public EntityIdentifier getIndex() {
		return index;
	}

	public void setArray(EntityIdentifier array) {
		this.array = array;
	}

	public void setIndex(EntityIdentifier index) {
		this.index = index;
	}
	
	EntityIdentifier<Expression> exampleField;
	


}