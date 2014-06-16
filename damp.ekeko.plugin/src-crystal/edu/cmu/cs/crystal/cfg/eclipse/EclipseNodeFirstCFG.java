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

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.WhileStatement;

/**
 * A CFG which places the control flow node at the merge point.
 * @see EclipseCFG for a complete analysis of where to place the control flow node.
 * 
 * @author Ciera Jaspan
 * 
 */
public class EclipseNodeFirstCFG extends EclipseCFG {
	public EclipseNodeFirstCFG() {
		super();
	}

	public EclipseNodeFirstCFG(MethodDeclaration method) {
		super(method);
	}

	@Override
	public boolean visit(DoStatement node) {
		EclipseCFGNode doBegin = nodeMap.get(node);
		EclipseCFGNode doEnd = new EclipseCFGNode(null);

		blockStack.pushUnlabeled(node, doEnd, doBegin);
		doBegin.setEnd(doEnd);

		return true;
	}

	@Override
	public void endVisit(DoStatement node) {
		EclipseCFGNode doEnd = nodeMap.get(node).getEnd();
		EclipseCFGNode cond = nodeMap.get(node.getExpression());
		EclipseCFGNode body = nodeMap.get(node.getBody());
		EclipseCFGNode doBegin = nodeMap.get(node);

		createEdge(doBegin, body.getStart());
		createEdge(body.getEnd(), cond.getStart());
		createBooleanEdge(cond.getEnd(), doBegin, true);
		createBooleanEdge(cond.getEnd(), doEnd, false);

		blockStack.popUnlabeled();

		doBegin.setName("do");
		doEnd.setName("od");
	}

	@Override
	public boolean visit(EnhancedForStatement node) {
		EclipseCFGNode eforBegin = nodeMap.get(node);
		EclipseCFGNode eforEnd = new EclipseCFGNode(null);

		blockStack.pushUnlabeled(node, eforEnd, eforBegin);

		eforBegin.setEnd(eforEnd);

		return true;
	}

	@Override
	public void endVisit(EnhancedForStatement node) {
		EclipseCFGNode list = nodeMap.get(node.getExpression());
		EclipseCFGNode body = nodeMap.get(node.getBody());
		EclipseCFGNode param = nodeMap.get(node.getParameter());
		EclipseCFGNode eforBegin = nodeMap.get(node);
		EclipseCFGNode eforEnd = eforBegin.getEnd();

		eforBegin.setStart(list.getStart());

		createEdge(list.getEnd(), eforBegin);
		createItrEdge(eforBegin, eforEnd, true);
		createItrEdge(eforBegin, param.getStart(), false);
		createEdge(param.getEnd(), body.getStart());
		createEdge(body.getEnd(), eforBegin);

		blockStack.popUnlabeled();

		eforBegin.setName("efor");
		eforEnd.setName("rofe");
	}

	@Override
	public boolean visit(ForStatement node) {
		EclipseCFGNode forBegin = nodeMap.get(node);
		EclipseCFGNode forEnd = new EclipseCFGNode(null);

		forBegin.setName("forBdummy");
		forEnd.setName("forEdummy");

		blockStack.pushUnlabeled(node, forEnd, forBegin);
		forBegin.setEnd(forEnd);

		return true;
	}

	@Override
	public void endVisit(ForStatement node) {
		EclipseCFGNode beginDummy = nodeMap.get(node);
		EclipseCFGNode cond = nodeMap.get(node.getExpression());
		EclipseCFGNode body = nodeMap.get(node.getBody());
		EclipseCFGNode endDummy = beginDummy.getEnd();
		EclipseCFGNode current, last = null;

		// hijack the node map
		// make our REAL for node, the old one was a dummy point for continues
		EclipseCFGNode forNode = new EclipseCFGNode(node);
		nodeMap.put(node, forNode);
		beginDummy.setASTNode(null);
		beginDummy.setEnd(beginDummy);

		// connect up initializers
		for (ASTNode init : (List<ASTNode>) node.initializers()) {
			current = nodeMap.get(init);
			if (last != null)
				createEdge(last.getEnd(), current.getStart());
			else
				forNode.setStart(current.getStart());
			last = current;
		}

		// connect initializers to the for node
		if (last != null)
			createEdge(last.getEnd(), forNode);

		// connect for node to the conditional
		if (cond != null) {
			createEdge(forNode, cond.getStart());
			createBooleanEdge(cond.getEnd(), endDummy, false);
			createBooleanEdge(cond.getEnd(), body.getStart(), true);
		} else {
			createEdge(forNode, body.getStart());
		}

		// body is now conteced to the beginDummy, which is where the continue
		// jumps will go to
		createEdge(body.getEnd(), beginDummy);

		last = beginDummy;
		for (ASTNode update : (List<ASTNode>) node.updaters()) {
			current = nodeMap.get(update);
			createEdge(last.getEnd(), current.getStart());
			last = current;
		}

		// the final back edge
		createEdge(last.getEnd(), forNode);

		blockStack.popUnlabeled();

		forNode.setName("for");
		forNode.setEnd(endDummy);
	}

	@Override
	public boolean visit(WhileStatement node) {
		EclipseCFGNode whileBegin = nodeMap.get(node);
		EclipseCFGNode whileEnd = new EclipseCFGNode(null);

		blockStack.pushUnlabeled(node, whileEnd, whileBegin);
		whileBegin.setEnd(whileEnd);

		return true;
	}

	@Override
	public void endVisit(WhileStatement node) {
		EclipseCFGNode cond = nodeMap.get(node.getExpression());
		EclipseCFGNode body = nodeMap.get(node.getBody());
		EclipseCFGNode whileBegin = nodeMap.get(node);
		EclipseCFGNode whileEnd = whileBegin.getEnd();

		createEdge(whileBegin, cond.getStart());
		createBooleanEdge(cond.getEnd(), whileEnd, false);
		createBooleanEdge(cond.getEnd(), body.getStart(), true);
		createEdge(body.getEnd(), whileBegin);

		blockStack.popUnlabeled();

		whileBegin.setName("while");
		whileEnd.setName("elihw");
	}

}
