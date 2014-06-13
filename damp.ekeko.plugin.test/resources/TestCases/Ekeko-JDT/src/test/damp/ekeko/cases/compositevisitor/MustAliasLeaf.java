package test.damp.ekeko.cases.compositevisitor;

public class MustAliasLeaf extends Component {
	public void acceptVisitor(ComponentVisitor v) {
		System.out.println("Must alias.");
		Component temp = this;
		v.visitMustAliasLeaf(temp);
	}
}
