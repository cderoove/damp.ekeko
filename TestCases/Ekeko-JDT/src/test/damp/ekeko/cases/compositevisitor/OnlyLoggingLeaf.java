package test.damp.ekeko.cases.compositevisitor;

public class OnlyLoggingLeaf extends Component {
	public void acceptVisitor(ComponentVisitor v) {
		System.out.println("Only logging.");
	}
} 
