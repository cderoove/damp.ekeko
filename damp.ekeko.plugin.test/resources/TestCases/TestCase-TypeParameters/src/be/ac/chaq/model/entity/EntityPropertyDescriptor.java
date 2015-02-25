package be.ac.chaq.model.entity;

import java.lang.reflect.Field;

public class EntityPropertyDescriptor extends PropertyDescriptor {

	public EntityPropertyDescriptor(String name, Class<? extends EntityState> owningClass, Class<? extends EntityState> valueType, Field field) {
		super(name, owningClass, valueType, field);
	}

	@Override
	public boolean canBeAssigned(Object value) {
		return value instanceof EntityIdentifier; 
	}
	

}
