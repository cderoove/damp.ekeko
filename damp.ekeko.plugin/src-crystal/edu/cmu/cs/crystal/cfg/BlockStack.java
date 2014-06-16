/**
 * Copyright (c) 2006, 2007, 2008 Marwan Abi-Antoun, Jonathan Aldrich, Nels E. Beckman,
 * Kevin Bierhoff, David Dickey, Ciera Jaspan, Thomas LaToza, Gabriel Zenarosa, and others.
 *
 * This file is part of Crystal.
 *
 * Crystal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Crystal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Crystal.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.cmu.cs.crystal.cfg;

import java.util.Stack;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 * Protocol to using block Stack is: call push/pop labeled and push/pop breakable in pairs.
 * Between the pair, you may make more push/pop calls. This is a valid set of calls:
 * pushBreakable()
 * pushLabeled()
 * popLabeled()
 * pushLabeled()
 * pushBreakable()
 * popBreakable()
 * popLabeled()
 * popBreakeable()
 * 
 *  The getContinue and getBreakPoint may be called at any time.
 * @author ciera
 *
 * @param <Node>
 */
public class BlockStack<Node extends ICFGNode> implements Cloneable {
	public class Block {
		Block(ASTNode owner, Node breakPoint, Node continuePoint, String label) {
			this.owner = owner;
			this.continuePoint = continuePoint;
			this.breakPoint = breakPoint;
			this.label = label;
		}
		ASTNode owner;
		Node continuePoint;
		Node breakPoint;
		String label;
		
		public Node getPoint(boolean doBreak) {
			return doBreak ? breakPoint : continuePoint;
		}
		
		public String toString() {
			String str = "";
			
			if (label != null)
				str += "Label: " + label + " ";
			else
				str += "unlabeled ";
			
			if (breakPoint != null)
				str += "break: " + breakPoint.toString() + " ";
			else
				str += "no break ";

			
			if (continuePoint != null)
				str += "continue: " + continuePoint.toString() + " ";
			else
				str += "no continue ";
			
			return str;
		}
	}

	Stack<Block> blockStack;
	String nextLabel;
	
	public BlockStack() {
		blockStack = new Stack<Block>();
	}

	/**
	 * Push on a labeled statement. Owner is the statement which is labeled. It will
	 * be used as the break point by default. By default, no continue will be set.
	 * @param label
	 * @param owner
	 */
	public void pushLabeled(String label, ASTNode owner) {
		Block block = new Block(owner, null, null, label);
		blockStack.push(block);
	}
	
	/**
	 * Push on a statement. If this statement already is on the stack (it was labeled),
	 * this this will override the break/continue points. If the owner is not on the stack,
	 * then it will simply push on with no label known.
	 * @param owner The owning statement
	 * @param breakPoint The break point; may be null
	 * @param continuePoint The continue point; may be null
	 */
	public void pushUnlabeled(ASTNode owner, Node breakPoint, Node continuePoint) {
		Block block;
		
		if (blockStack.isEmpty() || blockStack.peek().owner != owner) {
			block = new Block(owner, breakPoint, continuePoint, null);
			blockStack.push(block);
		}
		else {
			block = blockStack.peek();
			block.breakPoint = breakPoint;
			block.continuePoint = continuePoint;
		}
	}
	
	/**
	 * Pop the top of the stack.
	 */
	public void popLabeled() {
		assert !blockStack.isEmpty() : "The CFG messed up, this block stack is empty!";
		blockStack.pop();
	}
	
	/**
	 * Request a pop from a breakable node. This will only actually pop if it will
	 * not be popped by popLabeled.
	 */
	public void popUnlabeled() {
		assert !blockStack.isEmpty() : "The CFG messed up, this block stack is empty!";
		if (blockStack.peek().label == null)
			blockStack.pop();
	}
	
	/**
	 * override the values only if this owner is already on the stack (that is, it
	 * got on the stack because it was labeled.
	 * @param owner
	 * @param breakPoint
	 * @param continuePoint
	 * @return true if it was able to override the the values, false if the owner was not already on the stack
	 */
	public boolean overrideIfExists(ASTNode owner, Node breakPoint, Node continuePoint) {
		if (!blockStack.isEmpty() && blockStack.peek().owner == owner) {
			Block block = blockStack.peek();
			block.breakPoint = breakPoint;
			block.continuePoint = continuePoint;
			return true;
		}
		else
			return false;
	}
	
	public Node getBreakPoint(String label) {
		return getNextPoint(true, label);
	}
	
	public Node getContinuePoint(String label) {
		return getNextPoint(false, label);
	}
	
	private Node getNextPoint(boolean doBreak, String label) {
		Block block;
		boolean found = false;
		//not as simple as it seems.
		//when we get to the label, we need the nearest thing before the label!
		//ugh...
		
		int ndx = blockStack.size() - 1;
		do {
			block = blockStack.get(ndx);
			ndx--;
			
			if (block.getPoint(doBreak) != null) {
				if (label == null || label.equals(block.label)) {
					found = true;
					break;
				}
			}
		}
		while (ndx >= 0);
		
		if (!found)
			return null;
		else
			return block.getPoint(doBreak);
	}
	
	public BlockStack<Node> clone() {
		BlockStack<Node> cloneStack = new BlockStack<Node>();
		//theoretically, should not need to do a deep copy. Shallow should
		//be adaquate.
		cloneStack.blockStack = (Stack<Block>) this.blockStack.clone();
		
		return cloneStack;
	}
}
