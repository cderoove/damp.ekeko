package be.ac.chaq.model.entity;

public interface IHierarchicalEntityState {
	
	public void setParent(EntityIdentifier parent, PropertyDescriptor locationInParent);
	
	public EntityIdentifier getParent();

}
