package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityProperty;
import be.ac.chaq.model.entity.EntityState;
import be.ac.chaq.model.entity.IHierarchicalEntityState;
import be.ac.chaq.model.entity.PropertyDescriptor;
import be.ac.chaq.model.entity.SimpleProperty;

public class ASTNode extends EntityState implements IHierarchicalEntityState {
	
	//TODO: figure out a way to persist and recover the link to the original JDT ASTNode to ease transformations
	
	@EntityProperty(value=ASTNode.class)
	private EntityIdentifier parent;
	
	@SimpleProperty(value=PropertyDescriptor.class)
	private PropertyDescriptor locationInParent;
	
	public void setParent(EntityIdentifier parent, PropertyDescriptor locationInParent) {
		this.parent = parent;
		this.locationInParent = locationInParent;
	}

	public EntityIdentifier getParent() {
		return this.parent;
	}
	
}