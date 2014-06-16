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
package edu.cmu.cs.crystal.cfg.eclipse;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import edu.cmu.cs.crystal.cfg.ICFGNode;
import edu.cmu.cs.crystal.flow.BooleanLabel;
import edu.cmu.cs.crystal.flow.ILabel;

/**
 * This class has several purposes:
 * 1) Wrap an ASTNode
 * 2) Be a node in a CFG, with incoming and outgoing edges
 * 3) Help us create a flat structure out of a heirarchical one.
 * 
 * 1 and 2 are obvious, so we'll discuss 3. When we build a CFG, we are walking a hierarchical
 * structure (the AST) and turning it into a flat structure (the CFG). When we get to a node, we must
 * evaluate the children and then the node itself. However, we might later need to plug this subgraph
 * into another part of the graph (like the else branch of an if). To do this, we need to know the starting
 * point of the subgraph. Our node can tell what it's first child is, but what about it's child's child?
 * 
 * To solve this, we have a 'startNode' which keeps track of the first node. Our startNode will be set
 * to our first child's start node. This bottoms out in a case where the start node is oneself.
 * 
 * We also need to know the ending node. In most cases, this is ourself. However, sometimes the node
 * breaks control flow entirely, and it can not be hooked up to another node in the normal flow. To
 * handle this, the endNode might be null.
 * 
 * @author ciera
 *
 */
public class EclipseCFGNode implements ICFGNode<ASTNode> {
    static protected int NEXT_ID;
    
	private ASTNode node;
	
	private EclipseCFGNode startNode;
	
	private EclipseCFGNode endNode;

	private Set<EclipseCFGEdge> inputs;
	
	private Set<EclipseCFGEdge> outputs;
	
	private String myId;
	private String myName;
	
	public EclipseCFGNode(ASTNode node) {
		this.node = node;
		inputs = new LinkedHashSet<EclipseCFGEdge>();
		outputs = new LinkedHashSet<EclipseCFGEdge>();
		startNode = this;
		endNode = this;
		myId = Integer.toString(NEXT_ID);
		NEXT_ID++;
	}

	public ASTNode getASTNode() {
		return node;
	}
	
	public void setASTNode(ASTNode node) {
		this.node = node;
	}

	public Set<EclipseCFGEdge> getInputs() {
		return inputs;
	}

	public Set<EclipseCFGEdge> getOutputs() {
		return outputs;
	}

	public EclipseCFGNode getEnd() {
		return endNode;
	}
	
	
	public EclipseCFGNode getStart() {
		return startNode;
	}

	/**
	 * Filters the output edges by the label we are looking for.
     * If we find a combination of BooleanLabels with anything else, 
     * we will throw an exception. We're not yet sure what this means,
     * and we'd like to investigate this case thoroughly.
	 * @param label the label to search for, using .equals()
	 * @return The output edges that have the ILabel label, or the empty set
	 * if non exist
	 */
	public Set<EclipseCFGEdge> getInputEdges(ILabel label) {
		Set<EclipseCFGEdge> filteredEdges = new HashSet<EclipseCFGEdge>();
		boolean hasBooleanLabel = false;
		for (EclipseCFGEdge edge : inputs) {
			if (edge.label instanceof BooleanLabel)
				hasBooleanLabel = true;
			else if (hasBooleanLabel)
				throw new RuntimeException("Node has input labels that are booleans and others: " + node);
			
			if (edge.label.equals(label))
				filteredEdges.add(edge);
		}
		
		return filteredEdges;
	}
	
	/**
	 * Filters the output edges by the label we are looking for.
	 * @param label the label to search for, using .equals()
	 * @return The output edges that have the ILabel label, or the empty set
	 * if non exist
	 */
	public Set<EclipseCFGEdge> getOutputEdges(ILabel label) {
		Set<EclipseCFGEdge> filteredEdges = new HashSet<EclipseCFGEdge>();

		for (EclipseCFGEdge edge : outputs) {		
			if (edge.label.equals(label))
				filteredEdges.add(edge);
		}
		
		return filteredEdges;
	}

	
	public void setEnd(EclipseCFGNode end) {
		endNode = end;
	}
	
	
	public void setStart(EclipseCFGNode start) {
		startNode = start;
	}
	
	public void addInputEdge(EclipseCFGEdge edge) {
		inputs.add(edge);
	}
	
	public void addOutputEdge(EclipseCFGEdge edge) {
		outputs.add(edge);
	}


	public void setName(String name) {
		myName = name;
	}
	
	public String toString() {
		return getName();
	}
	
	public String getName() {
		if (myName != null)
			return myName;
		if (node == null)
			return "";
		if (node.nodeClassForType(node.getNodeType()) == null)
			throw new RuntimeException("Got a node without a type?!?");
		return node.nodeClassForType(node.getNodeType()).getSimpleName();
	}
}
