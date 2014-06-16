/**
 * Copyright (c) 2006, 2007, 2008 Marwan Abi-Antoun, Jonathan Aldrich, Nels E. Beckman, Kevin
 * Bierhoff, David Dickey, Ciera Jaspan, Thomas LaToza, Gabriel Zenarosa, and others.
 * 
 * This file is part of Crystal.
 * 
 * Crystal is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Crystal is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with Crystal. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package edu.cmu.cs.crystal.cfg.eclipse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.WildcardType;

import edu.cmu.cs.crystal.cfg.BlockStack;
import edu.cmu.cs.crystal.cfg.ExceptionMap;
import edu.cmu.cs.crystal.cfg.ICFGNode;
import edu.cmu.cs.crystal.cfg.IControlFlowGraph;
import edu.cmu.cs.crystal.flow.BooleanLabel;
import edu.cmu.cs.crystal.flow.ExceptionalLabel;
import edu.cmu.cs.crystal.flow.ILabel;
import edu.cmu.cs.crystal.flow.IteratorLabel;
import edu.cmu.cs.crystal.flow.NormalLabel;
import edu.cmu.cs.crystal.flow.SwitchLabel;

/**
 * Builds a CFG on the Eclipse AST. This class has been tested at the method level only, it has not
 * been tested at higher or lower levels, though it would theoretically work.
 * 
 * There is an interesting problem regarding when the transfer function for a node appears if it is
 * a control flow statement. That is, when does the transfer statement for constructs like if and
 * while occur? We have produced three possibilities:
 * 
 * Kevin: Should appear at the point when the branching of control flow occurs. For example, the the
 * conditionals of if and while.
 * 
 * Nels: Should appear at the merge point of the control flow. This is the conditional for while,
 * and the node that merges the two if branches.
 * 
 * Ciera: Should appear after all the children have been visited. This is the merge point on if, and
 * it is the node after the conditional on while.
 * 
 * Then we decided that no one in their right mind should be using transfer functions on control
 * flow anyway, so we can do whatever we want. Since I'm writing this and I have a nice design for
 * the third option anyway, we're going with that.
 * 
 * Note: Since this was written someone (Thomas? Nels?) did need to transfer on the control flow. He
 * needed it at the merge point though. The subtype @link{EclipseNodeFirstCFG} handles that, and is
 * currently the default control flow graph used in Crystal.
 * 
 * Algorithm description: This class visits each node of the AST. When it visits a node, it maps the
 * ASTNode to the CFGNode that it is creating. This allows a parent node to pull it out later and
 * insert it into the graph. There are several other data structures to help us with control flow.
 * These are described individually later. - In the previsit method, we create the node and put it
 * in the map. - In the visit method, we prepare any data structures that will be needed by the
 * children. - In the endvisit method, we put together the CFG for this node and its children (with
 * the exception of any edges that were added by children through the data structures.
 * 
 * @author ciera
 */
public class EclipseCFG extends ASTVisitor implements IControlFlowGraph<ASTNode>, Cloneable {

	// build simple cfg first, many edges into and out of finally
	// then traverse using a DFS. When we encounter an exception tag, track it
	// to a finally.
	// mark the entrance/exit of the finally duplicate if necessary and store
	// remember to stop within a finally and use the "original" if we hit a
	// control flow statement that overrides normal flow
	// continue until no more tags have been traversed.

	// protected EclipseCFGNode normalExit;

	protected BlockStack<EclipseCFGNode> blockStack;

	protected ExceptionMap<ASTNode, EclipseCFGNode> exceptionMap;

	protected HashMap<ASTNode, EclipseCFGNode> nodeMap;

	/**
	 * Each call to endvisit will reset what the startNode and endNode is.
	 */
	protected EclipseCFGNode startNode;
	protected EclipseCFGNode uberReturn;
	protected Map<ITypeBinding, EclipseCFGNode> excpReturns;
	protected EclipseCFGNode endNode;
	protected String name;

	private EclipseCFGNode undeclExit;

	public EclipseCFG(MethodDeclaration method) {
		nodeMap = new HashMap<ASTNode, EclipseCFGNode>();
		blockStack = new BlockStack<EclipseCFGNode>();
		exceptionMap = new ExceptionMap<ASTNode, EclipseCFGNode>();
		EclipseCFGNode.NEXT_ID = 0;
		createGraph(method);
	}

	public EclipseCFG() {
		nodeMap = new HashMap<ASTNode, EclipseCFGNode>();
		blockStack = new BlockStack<EclipseCFGNode>();
		exceptionMap = new ExceptionMap<ASTNode, EclipseCFGNode>();
		EclipseCFGNode.NEXT_ID = 0;
	}

	public void createGraph(MethodDeclaration method) {
		name = method.getName().getFullyQualifiedName();
		method.accept(this);
	}

	public ICFGNode<ASTNode> getStartNode() {
		return startNode;
	}

	public ICFGNode<ASTNode> getEndNode() {
		return endNode;
	}

	public ICFGNode<ASTNode> getUberReturn() {
		return uberReturn;
	}

	public ICFGNode<ASTNode> getUndeclaredExit() {
		return undeclExit;
	}

	public Map<ITypeBinding, EclipseCFGNode> getExceptionalExits() {
		return excpReturns;
	}


	/**
	 * Why might source be null? We get source from an endNode of something else. endNodes are
	 * allowed to be null if a node decides to handle their own outgoing control flow. This happens
	 * in breaks, returns, continues, throws, etc.
	 */
	private void createEdge(EclipseCFGNode source, EclipseCFGNode sink, ILabel label) {
		if (source != null) {
			EclipseCFGEdge edge = new EclipseCFGEdge(source, sink, label);
			source.addOutputEdge(edge);
			sink.addInputEdge(edge);
		}
	}

	protected void createEdge(EclipseCFGNode source, EclipseCFGNode sink) {
		NormalLabel label = NormalLabel.getNormalLabel();
		createEdge(source, sink, label);
	}

	protected void createItrEdge(EclipseCFGNode source, EclipseCFGNode sink, boolean isEmpty) {
		IteratorLabel label = IteratorLabel.getItrLabel(isEmpty);
		createEdge(source, sink, label);
	}

	protected void createBooleanEdge(EclipseCFGNode source, EclipseCFGNode sink, boolean boolValue) {
		BooleanLabel label = BooleanLabel.getBooleanLabel(boolValue);
		createEdge(source, sink, label);
	}

	protected void createEdge(EclipseCFGNode source, EclipseCFGNode sink, ITypeBinding exception) {
		ExceptionalLabel label = new ExceptionalLabel(exception);
		createEdge(source, sink, label);
	}

	protected void createEdge(EclipseCFGNode source, EclipseCFGNode sink, Expression switchCase) {
		SwitchLabel label = new SwitchLabel(switchCase);
		createEdge(source, sink, label);
	}

	private void makeListEdges(EclipseCFGNode init, List<ASTNode> list, EclipseCFGNode parent) {
		EclipseCFGNode current, last = init;

		if (last != null) {
			parent.setStart(last.getStart());
		}

		for (ASTNode node : list) {
			current = nodeMap.get(node);
			if (last != null) {
				createEdge(last.getEnd(), current.getStart());
			}
			else
				parent.setStart(current.getStart());
			last = current;
		}

		if (last != null) {
			createEdge(last.getEnd(), parent);
		}
	}

	@Override
	public void preVisit(ASTNode node) {
		EclipseCFGNode cfgNode = new EclipseCFGNode(node);
		nodeMap.put(node, cfgNode);
	}

	/**
	 * Creates an edge DIRECTLY from source to the proper finally blocks, and then from the finally
	 * blocks DIRECTLY to the destination. By directly, this means that it won't call getEnd or
	 * getStart. It uses the exceptionToStopAt to determine which finally blocks to grab. It will
	 * also make the edge from the source to the finally block have this exception. A normal edge
	 * will be used when exceptionToStopAt is null
	 */
	protected void hookFinally(EclipseCFGNode source, ITypeBinding exceptionToStopAt,
	    EclipseCFGNode dest) {
		Stack<EclipseCFGNode> finallyStack = exceptionMap.getFinallyToException(exceptionToStopAt);
		EclipseCFGNode last = null, current = null, cloneCurrent = null, first = null;

		while (!finallyStack.isEmpty()) {
			current = finallyStack.pop();
			cloneCurrent = copySubgraph(current);

			if (first == null)
				first = cloneCurrent;

			if (last != null)
				createEdge(last.getEnd(), cloneCurrent.getStart());
			last = cloneCurrent;
		}

		if (first != null) { // there is a finally
			if (exceptionToStopAt == null)
				createEdge(source, first.getStart());
			else
				createEdge(source, first.getStart(), exceptionToStopAt);
			createEdge(last.getEnd(), dest);
		}
		else {
			// no finally
			if (exceptionToStopAt == null)
				createEdge(source, dest);
			else
				createEdge(source, dest, exceptionToStopAt);
		}
	}

	private EclipseCFGNode copySubgraph(EclipseCFGNode current) {
		HashMap<EclipseCFGNode, EclipseCFGNode> cloneMap =
		    new HashMap<EclipseCFGNode, EclipseCFGNode>();
		EclipseCFGNode cloneNode;

		copySubgraphRecur(current.getStart(), current.getEnd(), cloneMap);

		cloneNode = cloneMap.get(current);
		return cloneNode;
	}

	/**
	 * @param current
	 * @return
	 */
	private EclipseCFGNode copySubgraphRecur(EclipseCFGNode current, EclipseCFGNode stopNode,
	    HashMap<EclipseCFGNode, EclipseCFGNode> cloneMap) {
		//base case: we've already copied this node (or are in the process of it)
		//so just return the clone.
		if (cloneMap.containsKey(current))
			return cloneMap.get(current);
		
		ASTNode node = current.getASTNode();
		EclipseCFGNode clone = new EclipseCFGNode(node);
		EclipseCFGNode start = current.getStart();
		EclipseCFGNode end = current.getEnd();

		clone.setName(current.getName());
		cloneMap.put(current, clone);

		if (current == stopNode || node instanceof ReturnStatement
		    || node instanceof ThrowStatement || node instanceof BreakStatement
		    || node instanceof ContinueStatement) {
			for (EclipseCFGEdge edge : current.getOutputs()) {
				EclipseCFGEdge cloneEdge =
				    new EclipseCFGEdge(clone, edge.getSink(), edge.getLabel());
				edge.getSink().addInputEdge(cloneEdge);
				clone.addOutputEdge(cloneEdge);
			}
		}
		else {
			for (EclipseCFGEdge edge : current.getOutputs()) {
				EclipseCFGNode cloneSink = copySubgraphRecur(edge.getSink(), stopNode, cloneMap);
				EclipseCFGEdge cloneEdge = new EclipseCFGEdge(clone, cloneSink, edge.getLabel());

				cloneSink.addInputEdge(cloneEdge);
				clone.addOutputEdge(cloneEdge);
			}
		}

		if (cloneMap.containsKey(start))
			start = cloneMap.get(start);
		if (cloneMap.containsKey(end))
			end = cloneMap.get(end);

		clone.setStart(start);
		clone.setEnd(end);

		return clone;
	}

	/* CONTROL FLOW */
	@Override
	public void endVisit(AssertStatement node) {
		EclipseCFGNode assertNode = nodeMap.get(node);
		EclipseCFGNode expNode = nodeMap.get(node.getExpression());
		EclipseCFGNode messageNode = nodeMap.get(node.getMessage());
		EclipseCFGNode falsePath = new EclipseCFGNode(null);
		ITypeBinding binding = node.getAST().resolveWellKnownType("java.lang.Throwable");
		EclipseCFGNode catchNode = exceptionMap.getCatchNode(binding);

		assertNode.setStart(expNode.getStart());
		createBooleanEdge(expNode.getEnd(), assertNode, true);

		falsePath.setName("POP!");

		if (messageNode != null) {
			createBooleanEdge(expNode.getEnd(), messageNode.getStart(), false);
			createEdge(messageNode.getEnd(), falsePath);
		}
		else {
			createBooleanEdge(expNode.getEnd(), falsePath, false);
		}

		hookFinally(falsePath, binding, catchNode);

	}

	@Override
	public boolean visit(BreakStatement node) {
		EclipseCFGNode breakStmnt = nodeMap.get(node);
		blockStack.overrideIfExists(node, breakStmnt, null);
		return true;
	}

	@Override
	public void endVisit(BreakStatement node) {
		EclipseCFGNode breakStmnt = nodeMap.get(node);
		String label = (node.getLabel() != null) ? node.getLabel().getIdentifier() : null;

		EclipseCFGNode breakPoint = blockStack.getBreakPoint(label);
		breakStmnt.setName("break");
		hookFinally(breakStmnt, null, breakPoint);
		breakStmnt.setEnd(null);
	}

	@Override
	public void endVisit(ContinueStatement node) {
		EclipseCFGNode continueStmnt = nodeMap.get(node);
		String label = (node.getLabel() != null) ? node.getLabel().getIdentifier() : null;

		EclipseCFGNode continuePoint = blockStack.getContinuePoint(label);
		continueStmnt.setName("continue");
		hookFinally(continueStmnt, null, continuePoint);
		continueStmnt.setEnd(null);
	}

	@Override
	public void endVisit(ConditionalExpression node) {
		EclipseCFGNode ifExp = nodeMap.get(node);
		EclipseCFGNode cond = nodeMap.get(node.getExpression());
		EclipseCFGNode thenClause = nodeMap.get(node.getThenExpression());
		EclipseCFGNode elseClause = nodeMap.get(node.getElseExpression());

		createBooleanEdge(cond.getEnd(), thenClause.getStart(), true);
		createEdge(thenClause.getEnd(), ifExp);
		createBooleanEdge(cond.getEnd(), elseClause.getStart(), false);
		createEdge(elseClause.getEnd(), ifExp);

		ifExp.setStart(cond.getStart());
		ifExp.setName("? :");
	}

	@Override
	public boolean visit(DoStatement node) {
		EclipseCFGNode doEnd = nodeMap.get(node);
		EclipseCFGNode doBegin = new EclipseCFGNode(null);

		blockStack.pushUnlabeled(node, doEnd, doBegin);
		doEnd.setStart(doBegin);

		return true;
	}

	@Override
	public void endVisit(DoStatement node) {
		EclipseCFGNode doEnd = nodeMap.get(node);
		EclipseCFGNode cond = nodeMap.get(node.getExpression());
		EclipseCFGNode body = nodeMap.get(node.getBody());
		EclipseCFGNode doBegin = doEnd.getStart();

		createEdge(doBegin, body.getStart());
		createEdge(body.getEnd(), cond.getStart());
		createBooleanEdge(cond.getEnd(), body.getStart(), true);
		createBooleanEdge(cond.getEnd(), doEnd, false);

		blockStack.popUnlabeled();

		doBegin.setName("do");
		doEnd.setName("od");
	}

	@Override
	public boolean visit(EnhancedForStatement node) {
		EclipseCFGNode eforEnd = nodeMap.get(node);
		EclipseCFGNode eforBegin = new EclipseCFGNode(null);

		blockStack.pushUnlabeled(node, eforEnd, eforBegin);
		eforEnd.setStart(eforBegin);

		return true;
	}

	@Override
	public void endVisit(EnhancedForStatement node) {
		EclipseCFGNode eforEnd = nodeMap.get(node);
		EclipseCFGNode list = nodeMap.get(node.getExpression());
		EclipseCFGNode body = nodeMap.get(node.getBody());
		EclipseCFGNode param = nodeMap.get(node.getParameter());
		EclipseCFGNode eforBegin = eforEnd.getStart();

		eforEnd.setStart(list.getStart());

		createEdge(list.getEnd(), eforBegin);
		createItrEdge(eforBegin, param.getStart(), false);
		createItrEdge(eforBegin, eforEnd, true);
		createEdge(param.getEnd(), body.getStart());
		createEdge(body.getEnd(), eforBegin);

		blockStack.popUnlabeled();

		eforBegin.setName("efor");
		eforEnd.setName("rofe");
	}

	@Override
	public boolean visit(ForStatement node) {
		EclipseCFGNode forEnd = nodeMap.get(node);
		EclipseCFGNode forBegin = new EclipseCFGNode(null);

		blockStack.pushUnlabeled(node, forEnd, forBegin);
		forEnd.setStart(forBegin);

		return true;
	}

	@Override
	public void endVisit(ForStatement node) {
		EclipseCFGNode forEnd = nodeMap.get(node);
		EclipseCFGNode cond = nodeMap.get(node.getExpression());
		EclipseCFGNode body = nodeMap.get(node.getBody());
		EclipseCFGNode forBegin = forEnd.getStart();
		EclipseCFGNode current, last = null;

		// first, run the initializers
		for (ASTNode init : (List<ASTNode>) node.initializers()) {
			current = nodeMap.get(init);
			if (last != null)
				createEdge(last.getEnd(), current.getStart());
			else
				forEnd.setStart(current.getStart());
			last = current;
		}

		if (cond != null) {
			if (last != null)
				createEdge(last.getEnd(), cond.getStart());
			else
				forEnd.setStart(cond.getStart());

			createBooleanEdge(cond.getEnd(), forEnd, false);
			createBooleanEdge(cond.getEnd(), body.getStart(), true);
		}
		else {
			if (last != null)
				createEdge(last.getEnd(), body.getStart());
			else
				forEnd.setStart(body.getStart());
		}

		// notice that we are inserting forbegin after the initializers,
		// but just before the updaters. This is so that
		// when we hit a continue, we jump to the updaters and NOT the
		// inializers.
		// it is a little weird since the begin point is after the body, but
		// since it's
		// a loop, it's also before the body. :)
		// we also changed forEnd.getStart so that it no longer returns forBegin
		// (see above)
		createEdge(body.getEnd(), forBegin);

		last = forBegin;
		for (ASTNode update : (List<ASTNode>) node.updaters()) {
			current = nodeMap.get(update);
			createEdge(last.getEnd(), current.getStart());
			last = current;
		}

		if (cond != null)
			createEdge(last.getEnd(), cond.getStart());
		else
			createEdge(last.getEnd(), body.getStart());

		blockStack.popUnlabeled();

		forBegin.setName("for");
		forEnd.setName("rof");
	}

	@Override
	public boolean visit(LabeledStatement node) {
		blockStack.pushLabeled(node.getLabel().getIdentifier(), node.getBody());
		return true;
	}

	@Override
	public void endVisit(LabeledStatement node) {
		EclipseCFGNode label = nodeMap.get(node);
		EclipseCFGNode body = nodeMap.get(node.getBody());

		createEdge(body.getEnd(), label);

		blockStack.popLabeled();

		label.setName("label " + node.getLabel().toString());
		label.setStart(body.getStart());
	}

	@Override
	public boolean visit(IfStatement node) {
		EclipseCFGNode ifStmnt = nodeMap.get(node);
		blockStack.overrideIfExists(node, ifStmnt, null);
		return true;
	}

	@Override
	public void endVisit(IfStatement node) {
		EclipseCFGNode ifStmnt = nodeMap.get(node);
		EclipseCFGNode cond = nodeMap.get(node.getExpression());
		EclipseCFGNode thenClause = nodeMap.get(node.getThenStatement());

		createBooleanEdge(cond.getEnd(), thenClause.getStart(), true);
		createEdge(thenClause.getEnd(), ifStmnt);

		if (node.getElseStatement() != null) {
			EclipseCFGNode elseClause = nodeMap.get(node.getElseStatement());
			createBooleanEdge(cond.getEnd(), elseClause.getStart(), false);
			createEdge(elseClause.getEnd(), ifStmnt);
		}
		else {
			createBooleanEdge(cond.getEnd(), ifStmnt, false);
		}

		ifStmnt.setStart(cond.getStart());
		ifStmnt.setName("if");
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		EclipseCFGNode method = nodeMap.get(node);
		EclipseCFGNode implicitCatch;

		excpReturns = new HashMap<ITypeBinding, EclipseCFGNode>();

		undeclExit = new EclipseCFGNode(null);
		createEdge(undeclExit, method);
		undeclExit.setName("(error)");
		exceptionMap.pushCatch(undeclExit, node
		    .getAST().resolveWellKnownType("java.lang.Throwable"));

		for (Name name : (List<Name>) node.thrownExceptions()) {
			implicitCatch = new EclipseCFGNode(null);
			createEdge(implicitCatch, method);
			implicitCatch.setName("(throws)");
			exceptionMap.pushCatch(implicitCatch, (ITypeBinding) name.resolveBinding());
			excpReturns.put(name.resolveTypeBinding(), implicitCatch);
		}

		uberReturn = new EclipseCFGNode(null);
		uberReturn.setName("(uber-return)");
		createEdge(uberReturn, method);

		if (node.isConstructor()) {
			ITypeBinding parentClass = node.resolveBinding().getDeclaringClass();
			if (parentClass.isClass()) { //ignore enums or annotations, as they have constructors but not fields
				TypeDeclaration type = (TypeDeclaration) node.getParent();
				for (FieldDeclaration field : type.getFields()) {
					if (!Modifier.isStatic(field.getModifiers()))
						field.accept(this);
				}
			}
		}

		// visit the statements individually.
		// we'll need to put them together by hand later so we can insert the
		// field decls
		// into constructors.
		for (ASTNode param : (List<ASTNode>) node.parameters())
			param.accept(this);
		if (node.getBody() != null)
			for (ASTNode stmt : (List<ASTNode>) node.getBody().statements())
				stmt.accept(this);

		return false;
	}

	@Override
	public void endVisit(MethodDeclaration node) {
		EclipseCFGNode method = nodeMap.get(node);
		EclipseCFGNode body = null;
		EclipseCFGNode current, last = null;

		// connect params together
		for (ASTNode arg : (List<ASTNode>) node.parameters()) {
			current = nodeMap.get(arg);
			if (last != null)
				createEdge(last.getEnd(), current.getStart());
			last = current;
		}

		if (node.isConstructor())
			body = setUpConstructorBody(node);
		else
			body = setUpMethodBody(node);

		// connect the end of the body to the fall-off return
		createEdge(body.getEnd(), uberReturn);

		if (last != null) {
			createEdge(last.getEnd(), body.getStart());
			method.setStart(nodeMap.get(node.parameters().get(0)).getStart());
		}
		else
			method.setStart(body.getStart());

		// finish off exceptions
		for (int ndx = 0; ndx < node.thrownExceptions().size(); ndx++)
			exceptionMap.popCatch();
		// exceptionMap.popFinally();

		startNode = method.getStart();
		endNode = method;
		method.setName("Declare " + node.getName().getIdentifier());
	}

	/**
	 * set up the body part of a constructor declaration. Return the node that represents the body.
	 * 
	 * @param node
	 * @return
	 */
	private EclipseCFGNode setUpConstructorBody(MethodDeclaration node) {
		EclipseCFGNode last = null, current = null;
		List<ASTNode> statements = new ArrayList<ASTNode>(node.getBody().statements());
		ASTNode firstStmt = null;
		EclipseCFGNode body = new EclipseCFGNode(node.getBody());

		
		if (node.resolveBinding().getDeclaringClass().isClass()) { //annotations and enums don't have fields
			// connect field declarations with initializers together
			for (FieldDeclaration field : ((TypeDeclaration) node.getParent()).getFields()) {
				if (Modifier.isStatic(field.getModifiers()))
					continue;
				for (VariableDeclarationFragment frag : (List<VariableDeclarationFragment>) field
				    .fragments()) {
					if (frag.getInitializer() != null) {
						current = nodeMap.get(frag);
						if (last != null) {
							createEdge(last.getEnd(), current.getStart());
							current.setStart(last.getStart());
						}
						last = current;
					}
				}
			}
			// now figure out where to insert the initializers
			if (statements.size() > 0) {
				firstStmt = statements.get(0);
	
				if (firstStmt instanceof SuperConstructorInvocation) {
					current = nodeMap.get(firstStmt);
					if (last != null) {
						createEdge(current.getEnd(), last.getStart());
						current.setEnd(last.getEnd());
					}
					last = current;
					statements.remove(firstStmt);
				}
				else if (firstStmt instanceof ConstructorInvocation) {
					last = null;
				}
			}
		}

		makeListEdges(last, statements, body);

		return body;
	}

	/**
	 * set up the body part of a method declaration. Return the node that represents the body.
	 * 
	 * @param node
	 * @return
	 */
	private EclipseCFGNode setUpMethodBody(MethodDeclaration node) {
		EclipseCFGNode body = new EclipseCFGNode(node.getBody());

		if (node.getBody() != null)
			makeListEdges(null, node.getBody().statements(), body);

		return body;
	}

	@Override
	public void endVisit(ReturnStatement node) {
		EclipseCFGNode ret = nodeMap.get(node);
		EclipseCFGNode exp = nodeMap.get(node.getExpression());

		if (exp != null) {
			createEdge(exp.getEnd(), ret);
			ret.setStart(exp.getStart());
		}

		hookFinally(ret, (ITypeBinding) null, uberReturn);

		ret.setEnd(null);
		ret.setName("return");
	}

	@Override
	public boolean visit(SwitchStatement node) {
		EclipseCFGNode switchEnd = nodeMap.get(node);
		EclipseCFGNode switchBegin = new EclipseCFGNode(null);

		blockStack.pushUnlabeled(node, switchEnd, null);
		switchEnd.setStart(switchBegin);

		return true;
	}

	@Override
	public void endVisit(SwitchStatement node) {
		EclipseCFGNode switchEnd = nodeMap.get(node);
		EclipseCFGNode switchBegin = switchEnd.getStart();
		EclipseCFGNode exp = nodeMap.get(node.getExpression());
		EclipseCFGNode last = null;
		boolean hasDefault = false;
		List<ASTNode> stmnts = (List<ASTNode>) node.statements();

		createEdge(switchBegin, exp.getStart());

		last = switchEnd;
		for (int ndx = stmnts.size() - 1; ndx >= 0; ndx--) {
			ASTNode currentAST = stmnts.get(ndx);
			EclipseCFGNode current = nodeMap.get(currentAST);

			if (currentAST instanceof SwitchCase) {
				createEdge(exp.getEnd(), current.getStart(), ((SwitchCase) currentAST)
				    .getExpression());
				createEdge(current.getEnd(), last);
				hasDefault = hasDefault || ((SwitchCase) currentAST).getExpression() == null;
				continue;
			}
			createEdge(current.getEnd(), last);
			last = current.getStart();
		}

		if (!hasDefault)
			createEdge(exp.getEnd(), switchEnd, (Expression) null);

		blockStack.popUnlabeled();

		switchBegin.setName("switch");
		switchEnd.setName("hctiws");

	}

	@Override
	public boolean visit(CatchClause node) {
		EclipseCFGNode catchNode = nodeMap.get(node);
		blockStack.overrideIfExists(node, catchNode, null);
		return true;
	}

	@Override
	public void endVisit(CatchClause node) {
		EclipseCFGNode catchNode = nodeMap.get(node);
		EclipseCFGNode declNode = nodeMap.get(node.getException());
		EclipseCFGNode bodyNode = nodeMap.get(node.getBody());

		createEdge(declNode.getEnd(), bodyNode.getStart());
		createEdge(bodyNode.getEnd(), catchNode);

		catchNode.setStart(declNode.getStart());
		catchNode.setName("catch");
	}

	@Override
	public void endVisit(ThrowStatement node) {
		EclipseCFGNode throwNode = nodeMap.get(node);
		EclipseCFGNode expNode = nodeMap.get(node.getExpression());
		ITypeBinding binding = node.getExpression().resolveTypeBinding();
		EclipseCFGNode catchNode = exceptionMap.getCatchNode(binding);
		EclipseCFGNode current = throwNode;

		createEdge(expNode.getEnd(), throwNode);

		hookFinally(throwNode, binding, catchNode.getStart());

		throwNode.setStart(expNode.getStart());
		throwNode.setName("throw");
		throwNode.setEnd(null);
	}

	@Override
	public boolean visit(TryStatement node) {
		EclipseCFGNode tryNode = nodeMap.get(node);
		EclipseCFGNode finallyNode = null, catchNode, bodyNode;

		blockStack.overrideIfExists(node, tryNode, null);

		// first analyze the finally
		if (node.getFinally() != null) {
			node.getFinally().accept(this);
			finallyNode = nodeMap.get(node.getFinally());
			exceptionMap.pushFinally(finallyNode);

		}

		// then analyze the catches
		for (CatchClause catchClause : (List<CatchClause>) node.catchClauses()) {
			catchClause.accept(this);
			catchNode = nodeMap.get(catchClause);
			exceptionMap
			    .pushCatch(catchNode, catchClause.getException().getType().resolveBinding());
		}

		// analyze body now that the exceptionMap is set up
		node.getBody().accept(this);
		bodyNode = nodeMap.get(node.getBody());

		// remove the catches
		for (int ndx = 0; ndx < node.catchClauses().size(); ndx++)
			exceptionMap.popCatch();

		// set up normal flow
		if (finallyNode != null) {
			exceptionMap.popFinally();
			if (bodyNode.getEnd() != null) {
				// if the try has no normal ending, don't attach it to the
				// finally
				createEdge(bodyNode.getEnd(), finallyNode.getStart());
				createEdge(finallyNode.getEnd(), tryNode);
			}
		}
		else
			createEdge(bodyNode.getEnd(), tryNode);

		// set up exceptional flow
		for (CatchClause catchClause : (List<CatchClause>) node.catchClauses()) {
			catchNode = nodeMap.get(catchClause);

			if (finallyNode != null)
				createEdge(catchNode.getEnd(), finallyNode.getStart());
			else
				createEdge(catchNode.getEnd(), tryNode);
		}

		tryNode.setStart(bodyNode.getStart());
		tryNode.setName("try");
		return false;
	}

	@Override
	public boolean visit(WhileStatement node) {
		EclipseCFGNode whileEnd = nodeMap.get(node);
		EclipseCFGNode whileBegin = new EclipseCFGNode(null);

		blockStack.pushUnlabeled(node, whileEnd, whileBegin);
		whileEnd.setStart(whileBegin);

		return true;
	}

	@Override
	public void endVisit(WhileStatement node) {
		EclipseCFGNode whileEnd = nodeMap.get(node);
		EclipseCFGNode cond = nodeMap.get(node.getExpression());
		EclipseCFGNode body = nodeMap.get(node.getBody());
		EclipseCFGNode whileBegin = whileEnd.getStart();

		createEdge(whileBegin, cond.getStart());
		createBooleanEdge(cond.getEnd(), whileEnd, false);
		createBooleanEdge(cond.getEnd(), body.getStart(), true);
		createEdge(body.getEnd(), cond.getStart());

		blockStack.popUnlabeled();

		whileBegin.setName("while");
		whileEnd.setName("elihw");
	}

	/* BLOCKS */

	@Override
	public boolean visit(Block node) {
		EclipseCFGNode block = nodeMap.get(node);
		blockStack.overrideIfExists(node, block, null);
		return true;
	}

	@Override
	public void endVisit(Block node) {
		EclipseCFGNode bNode = nodeMap.get(node);
		makeListEdges(null, node.statements(), bNode);

		bNode.setName("{}");
	}

	@Override
	public void endVisit(ExpressionStatement node) {
		EclipseCFGNode exp = nodeMap.get(node.getExpression());
		EclipseCFGNode stmnt = nodeMap.get(node);

		createEdge(exp.getEnd(), stmnt);
		stmnt.setStart(exp.getStart());
	}

	@Override
	public void endVisit(ParenthesizedExpression node) {
		EclipseCFGNode exp = nodeMap.get(node.getExpression());
		nodeMap.put(node, exp);
	}

	@Override
	public void endVisit(SwitchCase node) {
		if (node.getExpression() != null) {
			EclipseCFGNode exp = nodeMap.get(node.getExpression());
			nodeMap.put(node, exp);
		}
		else {
			EclipseCFGNode defCase = nodeMap.get(node);
			defCase.setName("default");
		}
	}

	@Override
	public void endVisit(SynchronizedStatement node) {
		// TODO duplicate for all possible ways to get out of the sync!
		EclipseCFGNode sync = nodeMap.get(node);
		EclipseCFGNode exp = nodeMap.get(node.getExpression());
		EclipseCFGNode body = nodeMap.get(node.getBody());

		createEdge(exp.getEnd(), body.getStart());
		createEdge(body.getEnd(), sync);
		sync.setStart(exp.getStart());
	}

	/* VARIABLE DECLARATIONS */

	@Override
	public void endVisit(VariableDeclarationExpression node) {
		EclipseCFGNode decls = nodeMap.get(node);
		EclipseCFGNode typeNode = nodeMap.get(node.getType());

		EclipseCFGNode current, last = null;

		createEdge(decls, typeNode.getStart());
		last = typeNode;

		for (ASTNode frag : (List<ASTNode>) node.fragments()) {
			current = nodeMap.get(frag);
			createEdge(last.getEnd(), current.getStart());
			last = current;
		}

		decls.setStart(decls);
		decls.setEnd(last.getEnd());
		decls.setName(node.getType().toString());
	}

	@Override
	public void endVisit(VariableDeclarationFragment node) {
		EclipseCFGNode decl = handleVariableDecl(node, null);
		decl.setName(node.getName().getIdentifier());
	}

	@Override
	public void endVisit(VariableDeclarationStatement node) {
		EclipseCFGNode decls = nodeMap.get(node);
		EclipseCFGNode typeNode = nodeMap.get(node.getType());

		EclipseCFGNode current, last = null;

		createEdge(decls, typeNode.getStart());
		last = typeNode;

		for (ASTNode frag : (List<ASTNode>) node.fragments()) {
			current = nodeMap.get(frag);
			createEdge(last.getEnd(), current.getStart());
			last = current;
		}

		decls.setStart(decls);
		decls.setEnd(last.getEnd());
		decls.setName(node.getType().toString());
	}

	@Override
	public void endVisit(SingleVariableDeclaration node) {
		EclipseCFGNode type = nodeMap.get(node.getType());
		EclipseCFGNode decl = handleVariableDecl(node, type);

		decl.setName(node.getType().toString() + " " + node.getName().getIdentifier());
	}

	private EclipseCFGNode handleVariableDecl(VariableDeclaration node, EclipseCFGNode startPoint) {
		EclipseCFGNode decl = nodeMap.get(node);
		EclipseCFGNode name = nodeMap.get(node.getName());
		EclipseCFGNode current = null;

		if (startPoint != null) {
			current = startPoint.getEnd();
		}

		if (node.getInitializer() != null) {
			EclipseCFGNode init = nodeMap.get(node.getInitializer());
			if (current != null)
				createEdge(current, init.getStart());
			else
				startPoint = init;
			current = init.getEnd();
		}

		if (current != null)
			createEdge(current, name.getStart());
		else
			startPoint = name;

		createEdge(name.getEnd(), decl);

		decl.setStart(startPoint.getStart());

		return decl;
	}

	/* EXPRESSIONS */

	@Override
	public void endVisit(ArrayAccess node) {
		EclipseCFGNode arrayAccess = nodeMap.get(node);
		EclipseCFGNode array = nodeMap.get(node.getArray());
		EclipseCFGNode index = nodeMap.get(node.getIndex());

		createEdge(array.getEnd(), index.getStart());
		createEdge(index.getEnd(), arrayAccess);
		arrayAccess.setStart(array.getStart());
	}

	@Override
	public void endVisit(ArrayCreation node) {
		EclipseCFGNode arrayCreation = nodeMap.get(node);
		if (node.getInitializer() == null)
			makeListEdges(null, node.dimensions(), arrayCreation);
		else {
			EclipseCFGNode arrayInit = nodeMap.get(node.getInitializer());
			createEdge(arrayInit.getEnd(), arrayCreation);
			arrayCreation.setStart(arrayInit.getStart());
		}
	}

	@Override
	public void endVisit(ArrayInitializer node) {
		EclipseCFGNode arrayInit = nodeMap.get(node);
		makeListEdges(null, node.expressions(), arrayInit);
	}

	@Override
	public void endVisit(Assignment node) {
		EclipseCFGNode rhs = nodeMap.get(node.getRightHandSide());
		EclipseCFGNode lhs = nodeMap.get(node.getLeftHandSide());
		EclipseCFGNode assign = nodeMap.get(node);

		createEdge(rhs.getEnd(), lhs.getStart());
		createEdge(lhs.getEnd(), assign);
		assign.setStart(rhs.getStart());
		assign.setEnd(assign);

		assign.setName(node.getOperator().toString());
	}

	@Override
	public void endVisit(CastExpression node) {
		EclipseCFGNode cast = nodeMap.get(node);
		EclipseCFGNode expression = nodeMap.get(node.getExpression());

		createEdge(expression.getEnd(), cast);
		cast.setStart(expression.getStart());
	}

	@Override
	public void endVisit(ConstructorInvocation node) {
		EclipseCFGNode constructor = nodeMap.get(node);
		makeListEdges(null, (List<ASTNode>) node.arguments(), constructor);
	}

	@Override
	public void endVisit(FieldAccess node) {
		EclipseCFGNode field = nodeMap.get(node);
		EclipseCFGNode expression = nodeMap.get(node.getExpression());
		EclipseCFGNode name = nodeMap.get(node.getName());

		createEdge(expression.getEnd(), name.getStart());
		createEdge(name.getEnd(), field);
		field.setStart(expression.getStart());
		field.setName(".");
	}

	@Override
	public void endVisit(InfixExpression node) {
		EclipseCFGNode infix = nodeMap.get(node);
		EclipseCFGNode lhs = nodeMap.get(node.getLeftOperand());
		EclipseCFGNode rhs = nodeMap.get(node.getRightOperand());
		Operator op = node.getOperator();

		// short circuiting
		if (op.equals(Operator.CONDITIONAL_AND) || op.equals(Operator.CONDITIONAL_OR)) {
			boolean isAnd = node.getOperator().equals(InfixExpression.Operator.CONDITIONAL_AND);
			List<Expression> operands = new ArrayList<Expression>();
			EclipseCFGNode last = null;

			operands.add(node.getLeftOperand());
			operands.add(node.getRightOperand());
			operands.addAll(node.extendedOperands());

			int ndx = 0;

			for (Expression opNode : operands) {
				EclipseCFGNode operand = nodeMap.get(opNode);

				if (last != null)
					createBooleanEdge(last.getEnd(), operand.getStart(), isAnd);
				createBooleanEdge(operand.getEnd(), infix, !isAnd);
				if (ndx == operands.size() - 1)
					createBooleanEdge(operand.getEnd(), infix, isAnd);
				last = operand;
				ndx++;
			}
		}
		else {
			createEdge(lhs.getEnd(), rhs.getStart());

			makeListEdges(rhs, (List<ASTNode>) node.extendedOperands(), infix);
		}

		infix.setName(node.getOperator().toString());
		infix.setStart(lhs.getStart());
	}

	@Override
	public void endVisit(InstanceofExpression node) {
		EclipseCFGNode instanceTest = nodeMap.get(node);
		EclipseCFGNode lhs = nodeMap.get(node.getLeftOperand());
		EclipseCFGNode rhs = nodeMap.get(node.getRightOperand());

		createEdge(lhs.getEnd(), rhs.getStart());
		createEdge(rhs.getEnd(), instanceTest);
		instanceTest.setStart(lhs.getStart());

		instanceTest.setName("instanceof");
	}

	@Override
	public void endVisit(ClassInstanceCreation node) {
		EclipseCFGNode constructor = nodeMap.get(node);

		// Normal edges
		makeListEdges(null, (List<ASTNode>) node.arguments(), constructor);

		// handle exception edges
		for (ITypeBinding exception : node.resolveConstructorBinding().getExceptionTypes()) {
			EclipseCFGNode catchNode = exceptionMap.getCatchNode(exception);

			if (catchNode != null)
				createEdge(constructor, catchNode.getStart(), exception);
			// else
			// createEdge(invocation, normalExit, exception);
		}

		constructor.setName("new " + node.getType().resolveBinding().getName());
	}

	@Override
	public void endVisit(MethodInvocation node) {
		EclipseCFGNode invocation = nodeMap.get(node);

		// normal edges
		makeListEdges(
		    nodeMap.get(node.getExpression()), (List<ASTNode>) node.arguments(), invocation);

		// handle exception edges
		for (ITypeBinding exception : node.resolveMethodBinding().getExceptionTypes()) {
			EclipseCFGNode catchNode = exceptionMap.getCatchNode(exception);

			if (catchNode != null)
				createEdge(invocation, catchNode.getStart(), exception);
			// else
			// createEdge(invocation, normalExit, exception);
		}

		invocation.setName("Call " + node.getName().getIdentifier());
	}

	@Override
	public void endVisit(PostfixExpression node) {
		EclipseCFGNode exp = nodeMap.get(node.getOperand());
		EclipseCFGNode postfix = nodeMap.get(node);

		createEdge(exp.getEnd(), postfix);
		postfix.setStart(exp.getStart());

		postfix.setName(node.getOperator().toString());
	}

	@Override
	public void endVisit(PrefixExpression node) {
		EclipseCFGNode exp = nodeMap.get(node.getOperand());
		EclipseCFGNode prefix = nodeMap.get(node);

		createEdge(exp.getEnd(), prefix);
		prefix.setStart(exp.getStart());

		prefix.setName(node.getOperator().toString());
	}

	@Override
	public void endVisit(QualifiedName node) {
		EclipseCFGNode fullName = nodeMap.get(node);
		EclipseCFGNode qual = nodeMap.get(node.getQualifier());
		EclipseCFGNode name = nodeMap.get(node.getName());

		createEdge(qual.getEnd(), name.getStart());
		createEdge(name.getEnd(), fullName);
		fullName.setStart(qual.getStart());
		fullName.setName(".");
	}

	@Override
	public void endVisit(SuperConstructorInvocation node) {
		EclipseCFGNode constructor = nodeMap.get(node);
		EclipseCFGNode exp = nodeMap.get(node.getExpression());

		makeListEdges(exp, (List<ASTNode>) node.arguments(), constructor);
	}

	@Override
	public void endVisit(SuperFieldAccess node) {
		EclipseCFGNode field = nodeMap.get(node);
		EclipseCFGNode name = nodeMap.get(node.getName());

		createEdge(name.getEnd(), field);

		if (node.getQualifier() != null) {
			EclipseCFGNode qual = nodeMap.get(node.getQualifier());
			createEdge(qual.getEnd(), name.getStart());
			field.setStart(qual.getStart());
		}
		else
			field.setStart(name.getStart());
	}

	@Override
	public void endVisit(SuperMethodInvocation node) {
		EclipseCFGNode invocation = nodeMap.get(node);
		EclipseCFGNode qual = nodeMap.get(node.getQualifier());

		makeListEdges(qual, (List<ASTNode>) node.arguments(), invocation);
		invocation.setName("Call " + node.getName().getIdentifier());
	}

	/*
	 * These methods are only here for debugging purposes and can be removed later.
	 */

	@Override
	public void endVisit(StringLiteral node) {
		EclipseCFGNode name = nodeMap.get(node);
		name.setName(node.getLiteralValue());
	}

	@Override
	public void endVisit(ThisExpression node) {
		EclipseCFGNode name = nodeMap.get(node);
		name.setName("this");
	}

	@Override
	public void endVisit(SimpleName node) {
		EclipseCFGNode name = nodeMap.get(node);
		name.setName(node.getIdentifier());
	}

	@Override
	public void endVisit(NullLiteral node) {
		EclipseCFGNode nullNode = nodeMap.get(node);
		nullNode.setName("null");
	}

	@Override
	public void endVisit(NumberLiteral node) {
		EclipseCFGNode num = nodeMap.get(node);
		num.setName(node.getToken());
	}

	@Override
	public void endVisit(TypeLiteral node) {
		EclipseCFGNode num = nodeMap.get(node);
		num.setName(node.getType().toString());
	}

	@Override
	public void endVisit(CharacterLiteral node) {
		EclipseCFGNode num = nodeMap.get(node);
		num.setName(node.getEscapedValue());
	}

	@Override
	public void endVisit(BooleanLiteral node) {
		EclipseCFGNode num = nodeMap.get(node);
		num.setName(node.toString());
	}

	/** TYPES * */

	@Override
	public void endVisit(ArrayType node) {
		EclipseCFGNode type = nodeMap.get(node);
		EclipseCFGNode compType = nodeMap.get(node.getComponentType());

		createEdge(compType.getEnd(), type);
		type.setStart(compType.getStart());
		type.setName(node.toString());
	}

	@Override
	public void endVisit(ParameterizedType node) {
		EclipseCFGNode type = nodeMap.get(node);
		EclipseCFGNode root = nodeMap.get(node.getType());

		makeListEdges(root, node.typeArguments(), type);
		type.setName(node.toString());
	}

	@Override
	public void endVisit(PrimitiveType node) {
		EclipseCFGNode type = nodeMap.get(node);

		type.setName(node.toString());
	}

	@Override
	public void endVisit(QualifiedType node) {
		EclipseCFGNode type = nodeMap.get(node);
		EclipseCFGNode name = nodeMap.get(node.getName());
		EclipseCFGNode qual = nodeMap.get(node.getQualifier());

		createEdge(name.getEnd(), qual.getStart());
		createEdge(qual.getEnd(), type);
		type.setStart(name.getStart());
		type.setName(node.toString());
	}

	@Override
	public void endVisit(SimpleType node) {
		EclipseCFGNode type = nodeMap.get(node);
		EclipseCFGNode name = nodeMap.get(node.getName());

		createEdge(name.getEnd(), type);
		type.setStart(name.getStart());
		type.setName(node.toString());
	}

	@Override
	public void endVisit(WildcardType node) {
		EclipseCFGNode type = nodeMap.get(node);

		if (node.getBound() != null) {
			EclipseCFGNode bound = nodeMap.get(node.getBound());
			createEdge(bound.getEnd(), type);
			type.setStart(bound.getStart());
		}
		type.setName(node.toString());
	}

	@Override
	public boolean visit(TypeDeclarationStatement node) {
		return false;
	}

	@Override
	public boolean visit(AnonymousClassDeclaration node) {
		return false;
	}

}
