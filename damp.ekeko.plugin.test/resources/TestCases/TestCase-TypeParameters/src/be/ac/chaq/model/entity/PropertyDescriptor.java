package be.ac.chaq.model.entity;

import java.lang.reflect.Field;

public class PropertyDescriptor {
	
	private String name;
	
	private Class<? extends EntityState> owningClass;
	
	private Class<?> valueType;
	
	private Field field;
	
	public PropertyDescriptor(String name, Class<? extends EntityState> owningClass, Class<?> valueType, Field fld) {
		this.name = name;
		this.owningClass = owningClass;
		this.valueType = valueType;
		this.field = fld;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@SuppressWarnings("unchecked")
	public Class<? extends EntityState> getDeclaringClass() {
		return (Class<? extends EntityState>) field.getDeclaringClass();
	}
	
	public Class<? extends EntityState> getOwningClass() {
		return owningClass;
	}
	
	public void setOwningClass(Class<? extends EntityState> owningClass) {
		this.owningClass = owningClass;
	}

	public Class<?> getValueType() {
		return valueType;
	}

	public void setValueType(Class<?> valueType) {
		this.valueType = valueType;
	}

	public Field getField() {
		return field;
	}

	public void setField(Field field) {
		this.field = field;
	}

	public boolean canBeAssigned(Object value) {
		return false;
	}
	
	
}
