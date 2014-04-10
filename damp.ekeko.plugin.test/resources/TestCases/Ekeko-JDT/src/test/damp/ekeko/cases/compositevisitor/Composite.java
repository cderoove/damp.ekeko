package test.damp.ekeko.cases.compositevisitor;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Composite extends Component {

	public List<Component> elements;

	public Composite() {
		elements = new LinkedList<Component>();
	}

	public void addComponent(Component element) {
		elements.add(element);
	}

	public void acceptVisitor(ComponentVisitor v) {
		Iterator<Component> i = elements.iterator();
		while (i.hasNext()) {
			Component comp = (Component) i.next();
			comp.acceptVisitor(v);
		}
	}

}
