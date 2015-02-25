package be.ac.chaq.model.snapshot;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import be.ac.chaq.model.entity.ComponentIdentifier;
import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityState;

public class Snapshot {

	private Date timeStamp;
	
	private ComponentIdentifier component;

	private Map<EntityIdentifier, EntityState> identifierToState;
	
	private Set<Snapshot> predecessors;
	
	private Set<Snapshot> successors;
	
	public Snapshot() {
		identifierToState = new HashMap<EntityIdentifier, EntityState>();	
		predecessors = new HashSet<Snapshot>();
		successors = new HashSet<Snapshot>();
	}
	
	public Snapshot createSuccessor() {
		Snapshot successor = new Snapshot();
		successor.addPredecessor(this);
		this.addSuccessor(successor);
		successor.identifierToState = new HashMap<EntityIdentifier, EntityState>(this.identifierToState);
		return successor;
	}
	
	public Date getTimeStamp() {
		return timeStamp;
	}
	
	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	public boolean addSuccessor(Snapshot successor) {
		return successors.add(successor);
	}
	
	public boolean addPredecessor(Snapshot predecessor) {
		return predecessors.add(predecessor);
	}

	public ComponentIdentifier getComponent() {
		return component;
	}

	public void setComponent(ComponentIdentifier component) {
		this.component = component;
	}

	public Set<Snapshot> getPredecessors() {
		return predecessors;
	}

	public Set<Snapshot> getSuccessors() {
		return successors;
	}

	public EntityState lookup(EntityIdentifier id) {
		return identifierToState.get(id);
	}
	
	public boolean exists(EntityIdentifier id) {
		return identifierToState.containsKey(id);		
	}
	
	public void update(EntityState s) {
		identifierToState.put(s.getID(), s);
		s.setSnapshotForLookup(this);
	}
	
	public Collection<EntityState> getEntities() {
		return identifierToState.values();
	}
	
	
}
