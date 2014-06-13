package test.damp.ekeko.cases.compositevisitor;

public class PrototypicalLeaf extends Component {
	public void acceptVisitor(ComponentVisitor v) {
		System.out.println("Prototypical.");
		v.visitPrototypicalLeaf(this);
	}
} 
