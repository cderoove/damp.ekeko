package be.ac.chaq.model.entity;

import java.lang.reflect.Field;
import java.util.Collection;

public class SimplePropertyDescriptor extends PropertyDescriptor {

	public SimplePropertyDescriptor(String name, Class<? extends EntityState> owningClass, Class<?> valueType, Field field) {
		super(name, owningClass, valueType, field);
	}
	
	@Override
	public boolean canBeAssigned(Object value) {
		return getValueType().isAssignableFrom(value.getClass());
	}

}
