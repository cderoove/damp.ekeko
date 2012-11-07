package test.damp.ekeko.cases.compositevisitor;

public class SuperLogLeaf extends OnlyLoggingLeaf {
	public void acceptVisitor(ComponentVisitor v) {
		super.acceptVisitor(v);
		v.visitSuperLogLeaf(this);
	}
} 
