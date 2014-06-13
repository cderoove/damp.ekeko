package test.damp.ekeko.cases.compositevisitor;

public class MayAliasLeaf extends Component {
	
	public int getInput() {
		return 1;
	}
	
	public Object m(Object o) {
		if(getInput() % 2 == 0) 
			return o;
		else
			return new MayAliasLeaf();
	}
	
	public void acceptVisitor(ComponentVisitor v) {
		System.out.println("May alias.");
		v.visitMayAliasLeaf((MayAliasLeaf)m(this));
	}
}

	
