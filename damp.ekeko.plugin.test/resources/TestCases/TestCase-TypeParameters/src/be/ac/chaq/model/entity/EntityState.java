package be.ac.chaq.model.entity;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.ac.chaq.change.Change;
import be.ac.chaq.model.snapshot.Snapshot;

public abstract class EntityState  {
	
	
	private static Map<Class<? extends EntityState>, HashMap<String, PropertyDescriptor>> class2propertyDescriptors;
	
	static {
		class2propertyDescriptors = new HashMap<Class<? extends EntityState>, HashMap<String, PropertyDescriptor>>();
	}
	
	private static void registerPropertyDescriptor(Map<String, PropertyDescriptor> propertyDescriptors, PropertyDescriptor pd) {
		//"" is default value of name attribute in annotation
		if(pd.getName().equals(""))
			pd.setName(pd.getField().getName());
		PropertyDescriptor old = propertyDescriptors.put(pd.getName(), pd);
		if(old != null)
			throw new RuntimeException("Encountered two property descriptors in a class hierarchy with the same name:" + old + " and " + pd);
	}
	
	public static Map<String, PropertyDescriptor> getPropertyDescriptorsMap(Class<? extends EntityState> entityStateClass) {
		HashMap<String, PropertyDescriptor> propertyDescriptors = class2propertyDescriptors.get(entityStateClass);
		if(propertyDescriptors != null)
			return propertyDescriptors;
		propertyDescriptors = new HashMap<String,PropertyDescriptor>();
		for(Field field : getAllFields(entityStateClass)) {
			for(Annotation annotation : field.getAnnotations()) {
				//TODO: refactor to a static method PropertyDescriptor.fromAnnotationOnField(Annotation, Field, Owner)
				Class<? extends Annotation> annotationType = annotation.annotationType();
				if(annotationType == EntityProperty.class) {
					EntityProperty entityPropertyAnnotation = (EntityProperty) annotation;
					field.setAccessible(true);
					EntityPropertyDescriptor entityPropertyDescriptor = new EntityPropertyDescriptor(entityPropertyAnnotation.name(), entityStateClass, entityPropertyAnnotation.value(), field);
					registerPropertyDescriptor(propertyDescriptors,entityPropertyDescriptor);
					break;
				}
				if(annotationType == SimpleProperty.class) {
					SimpleProperty simplePropertyAnnotation = (SimpleProperty) annotation;
					field.setAccessible(true);
					SimplePropertyDescriptor simplePropertyDescriptor = new SimplePropertyDescriptor(simplePropertyAnnotation.name(), entityStateClass, simplePropertyAnnotation.value(), field);
					registerPropertyDescriptor(propertyDescriptors,simplePropertyDescriptor);
					break;
				}
				if(annotationType == EntityListProperty.class) {
					EntityListProperty entityListPropertyAnnotation = (EntityListProperty) annotation;
					field.setAccessible(true);
					EntityListPropertyDescriptor entityListPropertyDescriptor = new EntityListPropertyDescriptor(entityListPropertyAnnotation.name(),entityStateClass, entityListPropertyAnnotation.value(), field);
					registerPropertyDescriptor(propertyDescriptors,entityListPropertyDescriptor);
					break;
				}
			}
		}
		class2propertyDescriptors.put(entityStateClass, propertyDescriptors);
		return propertyDescriptors;
	}
	
	public static Collection<PropertyDescriptor> getPropertyDescriptors(Class<? extends EntityState> entityStateClass) {
		return getPropertyDescriptorsMap(entityStateClass).values();
	}
	
	public static PropertyDescriptor getPropertyDescriptor(Class<? extends EntityState> entityStateClass, String name) {
		return getPropertyDescriptorsMap(entityStateClass).get(name);
	}
	
	
	private static List<Field> getAllFields(Class<?> type) {
		List<Field> result = new ArrayList<Field>();
		Class<?> i = type;
	    while (i != null && i != Object.class) {
	        for (Field field : i.getDeclaredFields()) {
	            if (!field.isSynthetic()) {
	                result.add(field);
	            }
	        }
	        i = i.getSuperclass();
	    }
	    return result;
	}
	
	//public abstract Object getProperty(PropertyDescriptor descriptor);
	//public abstract void setProperty(PropertyDescriptor property, Object newValue);
	
	private EntityIdentifier id;
	private Change appliedChange;
	private EntityState predecessor;
	private Snapshot snapshotForLookup;

	public EntityState() {
	}
	
	public void setAppliedChange(Change c)  {
		if(appliedChange == null)
			appliedChange = c;
		else
			throw new RuntimeException("EntityState already has a Change object as its appliedChange.");
	}
	
	public Change getAppliedChange() {
		return appliedChange;
	}
	
	public EntityIdentifier getID() {
		return id;
	}
	
	public void setID(EntityIdentifier id) {
		this.id = id;
	}
	
	public EntityState lookup(EntityIdentifier id) {
		return getSnapshotForLookup().lookup(id);
	}

	//should be overridden for subclasses that have additional, non-property fields
	protected void initializeFieldsFrom(EntityState s) {
		this.appliedChange = null;
		this.id = s.id;
		this.predecessor = s.predecessor;
		this.snapshotForLookup = s.getSnapshotForLookup();
	}
	private void initializePropertiesFrom(EntityState s) {
		for(PropertyDescriptor pd : EntityState.getPropertyDescriptors(s.getClass())) {
			this.setProperty(pd, s.getProperty(pd));
		}
	}

	public EntityState shallowClone() {
		Class<? extends EntityState> clazz = this.getClass();
		EntityState newInstance;
		try {
			newInstance = clazz.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		newInstance.initializeFieldsFrom(this);
		newInstance.initializePropertiesFrom(this);
		return newInstance;
	}
	
	public Snapshot getSnapshotForLookup() {
		return snapshotForLookup;
	}
	
	public void setSnapshotForLookup(Snapshot snapshotForLookup) {
		this.snapshotForLookup = snapshotForLookup;
	}
	
	public PropertyDescriptor getPropertyDescriptorNamed(String name) {
		PropertyDescriptor pd = getPropertyDescriptor(this.getClass(), name);
		if(pd == null)
			throw new RuntimeException("EntityState does not have a property named" + name);
		return pd;
	}
	
	public Object getPropertyNamed(String name) {
		PropertyDescriptor pd = getPropertyDescriptorNamed(name);
		return getProperty(pd);
	}
	
	public void setPropertyNamed(String name, Object value) {
		PropertyDescriptor pd = getPropertyDescriptor(this.getClass(), name);
		if(pd == null)
			throw new RuntimeException("EntityState does not have a property named" + name);
		setProperty(pd, value);
	}
	
	public Object getProperty(PropertyDescriptor descriptor) {
		try {
			Field field = descriptor.getField();
			Object value = field.get(this);
			assert(descriptor.canBeAssigned(value));
			return value;
		} catch(Exception e) {
			throw new RuntimeException("Cannot access given property " + descriptor);
		}
	}
	
	public void setProperty(PropertyDescriptor descriptor, Object value)  {
		try {
			//assert(descriptor.canBeAssigned(value));
			Field field = descriptor.getField();
			field.set(this, value);
		} catch(Exception e) {
			throw new RuntimeException("Cannot set property " + descriptor + "to value " + value);
		}
		
	}

	public EntityState getPredecessor() {
		return predecessor;
	}

	public void setPredecessor(EntityState predecessor) {
		this.predecessor = predecessor;
	}
	
}