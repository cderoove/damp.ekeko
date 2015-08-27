package damp.ekeko.soot.icfg;

import java.util.Iterator;
import java.util.LinkedList;


import com.google.common.base.Objects;

import soot.SootMethod;
import soot.Unit;

public class Context {
	
	private LinkedList<Unit> callStack;
	private Unit reachedNode;
	
	public Context() {
		callStack = new LinkedList<>();
	}

	public Context(Unit reachedNode) {
		this.reachedNode = reachedNode;
		callStack = new LinkedList<>();
	}
		
	public Context(Unit reachedNode, LinkedList<Unit> callStack) {
		this.reachedNode = reachedNode;
		this.callStack = callStack;
	}

	@SuppressWarnings("unchecked")
	public Context copy() {
		return new Context(reachedNode, (LinkedList<Unit>) callStack.clone());
	}
	
	
	public Unit top() {
		return callStack.peek();
	}

	public Unit pop() {
		return callStack.poll();
	}
	
	public void push(Unit call) {
		callStack.addFirst(call);
	}
	
	public boolean isEmpty() {
		return callStack.isEmpty();
	}
	
	public boolean contains(Unit call) {
		return callStack.contains(call);
	}
	
	//intra-procedural successor
	public Context level(Unit newReachedNode) {
		Context newContext = copy();
		newContext.reachedNode = newReachedNode;
		return newContext;
	}
	
	//inter-procedural successor (call into new method)
	public Context descend(Unit newReachedNode, Unit callSite) {
		Context newContext = copy();
		newContext.push(callSite);
		newContext.reachedNode = newReachedNode;
		return newContext;
	}
	
	public Context ascend(Unit newReachedNode) {
		Context newContext = copy();
		Unit originalCallSite = newContext.pop();
		newContext.reachedNode = newReachedNode;
		return newContext;
	}
	

	public Unit getReachedNode() {
		return reachedNode;
	}

	public void setReachedNode(Unit reachedNode) {
		this.reachedNode = reachedNode;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof Context) {
			Context other = (Context) o;
			if(Objects.equal(reachedNode, other.reachedNode)) {
				if(callStack.size() == other.callStack.size()) {
					Iterator<Unit> myIterator = callStack.iterator();
					Iterator<Unit> otherIterator = other.callStack.iterator();
					while(myIterator.hasNext()) {
						if(!myIterator.next().equals(otherIterator.next())) {
							return false;
						}
					}
					return true;
				}
			}
		}
		return false;
		
	}
	
	

	
	
}
