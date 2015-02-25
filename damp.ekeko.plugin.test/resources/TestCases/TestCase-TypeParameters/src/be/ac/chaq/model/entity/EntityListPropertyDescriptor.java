package be.ac.chaq.model.entity;

import java.lang.reflect.Field;
import java.util.Collection;

public class EntityListPropertyDescriptor extends PropertyDescriptor {

	public EntityListPropertyDescriptor(String name, Class<? extends EntityState> owningClass, Class<?> valueType, Field field) {
		super(name, owningClass, valueType, field);
	}
	
	@Override
	public boolean canBeAssigned(Object value) {
		return value instanceof Collection<?>;
	}

}
