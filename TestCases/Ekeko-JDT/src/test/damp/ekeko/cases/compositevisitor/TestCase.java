package test.damp.ekeko.cases.compositevisitor;

public class TestCase {
	static public void runTest() {
		Composite cs = new Composite();
		cs.addComponent(new EmptyLeaf());
		cs.addComponent(new MayAliasLeaf());
		cs.addComponent(new MustAliasLeaf());
		cs.addComponent(new OnlyLoggingLeaf());
		cs.addComponent(new PrototypicalLeaf());
		cs.addComponent(new SuperLogLeaf());
		ComponentVisitor vstor = new ComponentVisitor();
		cs.acceptVisitor(vstor);
		System.out.println("done");
	}
}
